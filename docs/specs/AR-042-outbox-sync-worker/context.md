# Context — AR-042 · Offline write outbox and sync worker

## Starting point

- Base: `feat/AR-041-cache-first-sync` @ `881ef31` (PR #67, still open).
- Branch: `feat/AR-042-outbox-sync-worker`.

## Relevant code

- `core/database/.../entity/SyncOutboxEntity.kt` — table `sync_outbox`, op
  constants `CREATE/UPDATE/DELETE` (lowercase strings), attempts default 0,
  `nextAttemptAt` epoch millis.
- `core/database/.../dao/SyncOutboxDao.kt` — `due`, `markAttempt`,
  `markSynced` (clears `pendingPush`), `retryAll`, `discard`, `deleteByUser`.
- `core/database/.../dao/LedgerWriteDao.kt` — atomic ledger write + enqueue.
- `core/data/.../outbox/OutboxPayloads.kt` — entity → `SyncTransactionItem`
  (category slug, IRR amount, ISO instants), icon→slug fallback.
- `core/data/.../outbox/OutboxSyncImpl.kt` — FIFO drain, backoff, parking.
- `core/data/.../outbox/SyncOutboxWorker.kt`,
  `WorkManagerOutboxTrigger.kt` — unique work with connected constraint.
- `feature/transactions/.../TransactionsViewModel.kt` — combines ledger month,
  `LedgerSync.state` and `OutboxSync.snapshot`; `onRefresh` flushes first.
- `app/.../AramApplication.kt` — `Configuration.Provider` + app-start schedule.

## Conventions

- Follows AR-040/041 patterns: fixed `Clock` injection, `safeApiCall`,
  `AramJson`, no `!!` (gradle `verifyNoNotNullAssertions`).
- No logging of note/amount/payload contents.
- `SyncOutboxEntity` payload is the wire DTO, so retries are self-contained.

## Dependencies

- `androidx.work:work-runtime-ktx` 2.12.0, `androidx.hilt:hilt-work` 1.4.0
  (+ `hilt-compiler`), already used by `:core:data` and `:app`.

## Decisions log

1. Deletes go through REST `DELETE`, not `/sync/push` — the backend answers
   `skipped` for older stamps without distinguishing permanent from stale, so a
   push-based delete would silently fail. `404` is treated as applied.
2. A conflict is treated as applied (see spec non-goals); the pull after push
   overwrites the local row.
3. Strict FIFO: a transient failure stops the run so later entries cannot
   overtake an earlier edit.
4. Backoff: `30 s << attempts` capped at 1 h; attempts 6 marks parked
   (`nextAttemptAt = Long.MAX_VALUE / 2`) until retry/discard.
5. Sign-out deletes the queue so one user's mutations never reach another
   session.
6. Added a root `resolutionStrategy.force` for three androidx artifacts the
   mirrors do not serve at the versions WorkManager/Hilt request
   (`room-compiler-processing` 2.8.5, `core`/`core-ktx` 1.19.1,
   `profileinstaller` 1.4.1); requires `--refresh-dependencies` once.

## Verification log

- `./gradlew :core:data:testDebugUnitTest :feature:transactions:testDebugUnitTest`
  — pass (2026-11-22).
- `:app:assembleAlphaDebug test -PbuildDate=20261122` — pass.
- OutboxSyncImplTest — 9 tests, all green (added the local-profile case below).
- Room schema `3.json` exported (sync_outbox + categories.slug present).
- Device sheet on emulator-5554 (Android 16), stacked integration build:
  - Offline add of 90,000 IRR showed the queued banner; reconnecting pushed it
    (`POST /api/sync/push` 200) and cleared the banner automatically.
  - Fix found on device: with a local profile (user id, no token) `flush()`
    returned false, so `SyncOutboxWorker` retried forever (RETRY loop in
    logcat). It now returns true and leaves the queue intact until sign-in;
    covered by `a local profile without a token settles instead of retrying
    forever`. Re-verified on device: worker finishes SUCCESS.
