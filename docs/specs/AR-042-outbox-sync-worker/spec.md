# AR-042 · Offline write outbox and sync worker

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/45
- Phase / Sprint: Phase 1 · Transactions core / Sprint 3
- Estimate: 8 pts · P0
- Branch: `feat/AR-042-outbox-sync-worker` (stacked on `feat/AR-041-cache-first-sync`)
- Status: Draft

## Problem

AR-040/AR-041 made reads cache-first, but writes still assume the network. A
create/edit/delete made offline either fails or lives only in memory, and there
is no durable record of what still needs to reach the server.

## Goals / non-goals

Goals

- Persist every local ledger mutation in a `sync_outbox` row inside the same
  Room transaction that writes the ledger row.
- Drain the queue oldest-first from a WorkManager worker with a fixed
  `Idempotency-Key` per entry, exponential backoff (30 s → 1 h) and a
  connected-network constraint.
- Surface queue health in Activity (per-row "Queued" marker plus a summary
  banner with retry).
- Trigger on app start, after each write, and on pull-to-refresh.

Non-goals

- Pulling with a cursor (`/sync/pull?since`) and the Settings "Sync now"
  section — AR-043.
- Transfers as a two-row single request — no transfer flow exists yet.
- Conflict canonicalisation on 409 — the backend does not return 409 for LWW
  pushes and exposes no single-transaction GET; a stale push is answered as
  `skipped` and the periodic pull reconciles. Tracked as a follow-up.

## Acceptance criteria

- [x] Offline create/edit/delete is accepted instantly, survives process death
      (Room outbox row), and syncs after reconnect (worker + app-start trigger).
- [x] Retries reuse the same idempotency key; the entry id is the key.
- [ ] A 409 replaces the local row with the canonical one — deferred, see
      non-goals; conflicts currently count as applied.
- [x] Permanent failures (4xx validation/forbidden, undecodable payload) park
      with attempts = 6 and surface a Retry action.
- [x] Worker and enqueue logic are covered by tests (fake API + fixed clock).

## Technical design

Modules touched: `core:database`, `core:network`, `core:domain`, `core:data`,
`core:model`, `feature:transactions`, `app`.

Data flow

1. `TransactionRepositoryImpl.create/update/softDelete` build the transaction
   entity and an outbox entry (payload = serialized `SyncTransactionItem` with
   the category slug), then call `LedgerWriteDao` which wraps both writes in one
   `@Transaction` method. `OutboxTrigger.schedule()` hands work to WorkManager.
2. `OutboxSyncImpl.flush()` takes a mutex, reads due entries (batch 25), and
   for each:
   - push: `POST /sync/push` with one item and `Idempotency-Key: <entry id>`;
   - delete: `DELETE /transactions/{id}` (404 counts as applied).
   Outcomes: applied → delete entry + clear `pendingPush`; conflict → applied;
   permanent 4xx / bad payload → park at `PARKED_AT`; transient
   (offline/timeout/5xx/429) → attempts + 1, backoff, stop the run (strict FIFO);
   unauthorized → stop.
3. `SyncOutboxWorker` (`@HiltWorker`) flushes once and returns retry/success;
   `WorkManagerOutboxTrigger` enqueues unique work `aram-outbox-sync` with
   `ExistingWorkPolicy.KEEP`, connected constraint and 30 s exponential backoff.
4. Activity observes `OutboxSync.snapshot` (pending/failed counts) and shows a
   "Queued" label on rows with `pendingPush`, plus a banner: pending → "Sync
   now" (flush), failed → "Retry" (retryAll + flush). Pull-to-refresh flushes
   before pulling.

## Data and migrations

- v2 → v3: `ALTER TABLE categories ADD COLUMN slug TEXT` (nullable);
  `CREATE TABLE sync_outbox` with indices `(userId, nextAttemptAt)` and
  `(userId, entityId)`.
- `Transaction.pendingPush` is mapped to the model so rows can be marked.
- Sign-out clears `sync_outbox` for the user and Room data as before.
- Schema exported: `core/database/schemas/.../3.json`.

## Test plan

Unit (`:core:data`)

- `TransactionRepositoryImplTest`: create/update/delete enqueue an entry and
  schedule a worker; failed writes enqueue nothing.
- `OutboxSyncImplTest`: FIFO push + clear, idempotency key equals entry id,
  offline → attempts 1 + 30 s backoff, bad payload parks, delete calls the
  endpoint, delete of a missing row succeeds, guest no-op, retry/discard.
- `AuthRepositoryImplTest`: sign-out clears the outbox.

Unit (`:feature:transactions`)

- VM exposes the outbox snapshot, refreshes flush first, retry resets + flushes,
  queued rows carry `pendingSync`.

Manual (batched with the other AR tickets)

- Airplane mode → add/edit/delete → rows show Queued → reconnect → Queued
  disappears and the server state matches → kill and relaunch while offline to
  confirm the queue survives process death.

## Risks and open questions

- Root build forces `androidx.core`, `profileinstaller` and
  `room-compiler-processing` versions because the configured mirrors do not
  serve the versions Hilt/WorkManager request. Remove once mirrors catch up.
- WorkManager's default initializer is disabled in the manifest; verify no
  other component relied on it.
- No Room migration test harness yet (needs `room-testing` + an instrumented
  or Robolectric source set) — tracked for AR-044.
