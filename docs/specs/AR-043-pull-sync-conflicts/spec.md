# AR-043 · Pull sync, conflicts, and tombstones

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/46
- Phase / Sprint: Phase 1 · Online transactions core / Sprint 3
- Estimate: 5 pts · P0
- Branch: `feat/AR-042-outbox-sync-worker` (stacked on AR-042; AR-043 commits on top)
- Status: Draft

## Problem

AR-041 pulls one month at a time, so changes outside the opened month and
server-side deletes only surface when the user visits that month. There is no
stored cursor, no push-first ordering, and no way to see or force sync from
Settings.

## Goals / non-goals

Goals

- Persist a `/sync/pull` cursor in DataStore and pull changes since it.
- Push the outbox before pulling, then sweep the current month for tombstones.
- Merge with last-write-wins and let unflushed local rows win.
- Advance the cursor only after a complete pull; hold it when the pull fails or
  returns a full page (the backend caps at 500 rows and has no paging).
- Run the full sync on app open, on reconnect, every 15 minutes, and from a
  manual "Sync now" in Settings with live status and last-synced time.
- Sign-out clears the cursor with the user's data so the next sign-in does a
  full pull.

Non-goals

- `/sync/pull` pagination: the Aram-Api endpoint returns at most 500 rows with
  no page token. Until the backend adds paging, a full page is treated as
  partial (cursor unchanged, re-pulled next time). Tracked as a backend gap.
- Syncing budgets/goals/settings DTOs into Room — the payload is parsed but the
  budget/goal features still read their own endpoints; AR-044+.
- Conflict canonicalisation on 409 (no 409 in the backend; skipped pushes are
  reconciled by the next pull).

## Acceptance criteria

- [x] Two clients (mock scenario) converge after concurrent edits and deletes.
- [x] Tombstones propagate and never resurrect deleted rows.
- [x] Cursor is not advanced on a failed or partial pull.
- [x] Sign-out clears data; signing back in starts from a full pull.
- [x] Tests cover ordering, pagination, conflict precedence, and tombstone
      application.

## Technical design

Modules touched: `core:datastore`, `core:domain`, `core:data`, `feature:settings`,
`app`.

Data flow (`LedgerSyncRepositoryImpl.runSync`)

1. Mutex, then guest/token guard (no-op, returns true).
2. Skip when the same month synced < 15 min ago unless forced.
3. `OutboxSync.flush()` — push-first ordering.
4. GET categories → GET accounts → GET `/sync/pull?since=<cursor>` or without
   `since` when no cursor. Upsert delta rows preserving local winners
   (`pendingPush` or `updatedAt > startedAt`).
5. Sweep up to 5 pages of the month (100 rows each); when the page set is
   complete, tombstone rows missing from the response.
6. `nextCursor = serverTime` when the delta had fewer than 500 rows, otherwise
   keep the previous cursor (partial page).
7. Persist `SyncMetadata(now, month, cursor)` and emit `IDLE`; on failure keep
   the stored metadata and emit `OFFLINE`/`FAILED`/`STALE`.

Background: `LedgerSyncWorker` (`@HiltWorker`) calls `syncNow()` and retries on
failure. `SyncScheduler` enqueues a unique 15-minute periodic work
(`aram-ledger-sync`) and a one-shot on app start (`aram-ledger-sync-now`), both
with a connected-network constraint.

Settings: `SettingsViewModel` combines `LedgerSync.state` and
`OutboxSync.snapshot` into `SyncStatusUi`; the profile header shows a coloured
dot plus "Synced 5m ago · 2 waiting" style text and a "Sync now" action that
calls `LedgerSync.syncNow()`.

## Data and migrations

- `SyncMetadata` gains `cursor: String?`; `SyncMetadataDataStore` stores/removes
  the `sync_cursor` preference.
- No Room schema change.
- `AuthRepositoryImpl.wipeUser` already clears Room and `SyncMetadataStore`
  (cursor included) on sign-out.

## Test plan

Unit (`:core:data`, `LedgerSyncRepositoryImplTest`)

- Push-before-pull ordering via a recording outbox and HTTP recorder.
- Cursor advances to `serverTime` after a complete pull.
- Failed pull keeps the previous cursor and reports `OFFLINE`.
- Delta rows outside the swept month are cached.
- A 500-row delta page keeps the old cursor.
- Guest sync touches neither the outbox nor metadata.
- Two devices sharing one mock server converge after an edit and a delete
  (tombstone survives a second pull).
- Existing: staleness window, forced refresh, pending rows win, tombstone sweep,
  guest no-op, server error.

Unit (`:feature:settings`, `SettingsViewModelTest`)

- State combines ledger + outbox with the current clock.
- "Sync now" triggers `LedgerSync.syncNow()`.
- Summary labels for fresh/stale/offline/failed/queued states.

Manual (batched with the other AR tickets)

- Settings shows the live status, "Sync now" spins "Syncing…", then
  "Synced just now"; airplane mode shows "Offline · showing cached data".

## Risks and open questions

- `/sync/pull` has no paging; changes beyond the newest 500 rows need the month
  sweep to converge. Backend follow-up.
- The periodic worker is scheduled on app start only; WorkManager persists it,
  so it survives restarts, but it is not re-armed on boot beyond WorkManager's
  own rescheduling.
- Mock server timestamps use the injected clock; production LWW depends on
  server `clientUpdatedAt` handling (verified against Aram-Api contract docs).
