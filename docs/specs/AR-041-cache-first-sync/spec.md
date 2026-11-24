# AR-041 · Cache-first repositories with pull-to-refresh

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/44
- Phase / Sprint: Phase 1 / Sprint 2
- Estimate: 5 pts · P0
- Branch: feat/AR-041-cache-first-sync
- Status: Review

## Problem

The ledger screens were fed from Room but nothing refreshed Room from the API,
so an online user saw only the local seed. The backend also hard-deletes rows
and has no tombstones, so a refresh must decide deletion from a full month
sweep.

## Goals / non-goals

- One shared `LedgerSync` that owns "when to pull", exposed as `SyncState`.
- Pull one UTC month plus the category/account references, upsert into Room.
- Never blank cached content on failure.
- Keep writes local; the outbox is AR-042.

## Acceptance criteria

- [x] Transactions list renders from Room with no network, after a previous successful sync.
- [x] Pull-to-refresh updates the list without recomposition glitches or a full-screen spinner.
- [x] Opening a screen older than 15 minutes triggers one background refresh; failures show a banner, not an error screen.
- [x] Deleted rows pulled as tombstones disappear from reads.
- [x] Repository tests cover cached read, successful refresh, offline fallback, and conflict merge.

## Technical design

- `SyncState(status, lastSyncedAtMillis)` in `:core:model`; `SyncStatus` is
  IDLE / REFRESHING / OFFLINE / STALE / FAILED.
- `LedgerSync` (`:core:domain`) exposes `state: Flow<SyncState>` and
  `refresh(month, force)`.
- `LedgerSyncRepositoryImpl` (`:core:data`) serialises refreshes with a Mutex,
  skips when the same month synced under 15 minutes ago unless forced, and is a
  no-op for guests (no user id or no token). Pulls `GET /api/categories`,
  `GET /api/profile/accounts` and paged `GET /api/transactions?year&month`.
- Merge is last-write-wins: rows that are `pendingPush` or were updated after
  the pull started are kept; server rows otherwise replace local ones.
- Tombstones: only after a complete page sweep. An empty response tombstones
  the whole UTC month, otherwise rows absent from the kept id set are
  soft-deleted. `pendingPush` rows are never tombstoned.
- `last_synced_at` + `last_synced_month` live in a dedicated DataStore file and
  are cleared on sign-out with the rest of the user cache.
- UI: `:feature:transactions` injects `LedgerSync`, shows a
  `PullToRefreshBox`, and renders a banner (offline / stale / failed) with
  Retry instead of an error screen.

## Data and migrations

- Room v2: `transactions.pendingPush INTEGER NOT NULL DEFAULT 0`, migration
  `MIGRATION_1_2`; DAO `softDeleteMissing` / `softDeleteAllInRange` skip
  pending rows and stamp `pendingPush = 0` on pulled writes.
- `CategoryDao.softDeleteOrphans` / `AccountDao.softDeleteOrphans` drop the
  offline seed once the server list arrives, keeping referenced rows.

## Test plan

- `LedgerSyncRepositoryImplTest`: full pull, staleness skip, forced refresh,
  offline cache intact, LWW with pending row, tombstone sweep, pending rows
  survive, guest no-op, server error keeps cache.
- `TransactionsViewModelTest`: refresh on first composition, forced refresh on
  pull-to-refresh.
- Manual/device: covered in the sprint test sheet.

## Risks and open questions

- Backend `TransactionDto` has no `updatedAt`; LWW compares server rows to
  local `updatedAt` and the pull start time only.
- Page cap (5 × 100) means a month with more than 500 rows is treated as
  incomplete and skips tombstones.
