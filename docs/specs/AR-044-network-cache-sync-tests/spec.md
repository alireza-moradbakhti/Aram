# AR-044 · Network, cache and sync test suite

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/47
- Phase / Sprint: Phase 1 · Online transactions core / Sprint 3
- Estimate: 3 pts · P0
- Branch: `feat/AR-043-pull-sync-conflicts` (stacked on AR-043; AR-044 commits on top)
- Status: Draft

## Problem

The online sync work (AR-039 … AR-043) shipped with repository-level tests, but
the layers underneath had no coverage of their own: the Room schema had never
been migrated from a real old file, the outbox/write DAOs were untested, the
409 path of a duplicate budget was unverified, and the Compose UI around queued
and failed changes was never exercised.

## Goals / non-goals

Goals

- Prove the Room migrations (1→2, 2→3) run against the exported schemas and
  keep existing rows.
- Cover `SyncOutboxDao` scheduling semantics (oldest-first, parked rows,
  batching, failed counts, retry/reset, per-entity cleanup).
- Cover `LedgerWriteDao`: the ledger row and its outbox entry are written
  together, and a missing/soft-deleted row queues nothing.
- Verify a duplicate budget surfaces as `ApiError.Conflict` through the real
  request pipeline.
- Cover the outbox UI states (queued banner, retry action, per-row Queued chip)
  with Robolectric Compose tests.
- Keep auth (register/sign-in/seeding/expiry/401/sign-out wipe) and outbox
  (FIFO, idempotency key reuse, backoff, permanent parking, delete
  idempotency) coverage green; they existed already and stay in the suite.

Non-goals

- Instrumented/emulator tests: the DAO and migration coverage runs on
  Robolectric in the normal `test` task, so CI needs no emulator job.
- Rewriting existing repository tests; only additions and the two adjustments
  needed by the new `LedgerSync.syncNow()`/`OutboxSync` seams.
- Backend contract changes (no `/sync/pull` paging, no 409 for transactions).

## Acceptance criteria

- [x] `:core:database` has migration tests for 1→2, 2→3 and a 1→3 hop, built
      from the exported schema JSON.
- [x] `SyncOutboxDao` and `LedgerWriteDao` behaviour is covered.
- [x] A duplicate budget returns `ApiError.Conflict` from the mock engine.
- [x] Compose tests cover the queued banner, the failed banner and the Queued
      chip.
- [x] Auth and outbox suites remain green.
- [x] `:app:assembleAlphaDebug test` passes locally.

## Technical design

`:core:database` unit tests (Robolectric, in-memory Room):

- `AramDatabaseMigrationTest` — Room 2.8's `MigrationTestHelper` does not run
  under Robolectric (its `SupportSQLiteDriver` rejects the shadow database
  path), so the old database is rebuilt by executing every `createSql` from the
  exported `schemas/<db>/<version>.json`, then opened with the production
  `AramDatabase.MIGRATIONS`, which also validates the result against the v3
  schema and the Room identity hash.
- `SyncOutboxDaoTest` — ordering, due filtering (parked/other users), batch
  limit, attempts/backoff, failed counts, `markSynced`, `discard`,
  `deleteByUser`, `observeCount`.
- `LedgerWriteDaoTest` — create/update/delete transactionality and the
  "queue only what actually changed" invariant.

`:core:network` — `MockApiEngineTest` gains the duplicate-budget conflict case.

`feature:transactions` — `TransactionsScreenTest` renders the screen with
Robolectric Compose (BOM 2026.09, `isIncludeAndroidResources`) for the
`OutboxStatus` states.

## Data and migrations

No schema change. The tests pin schemas 1, 2 and 3; `schemas/` is registered as
a unit-test resource root so the JSON is on the test classpath.

## Test plan

- Unit (JVM/Robolectric): `./gradlew test -PbuildDate=...`
- CI-equivalent: `./gradlew :app:assembleAlphaDebug test -PbuildDate=...`
- Manual: none new; the AR-042/AR-043 device sheet already covers the banner
  and sync status on a real phone.

## Risks and open questions

- Robolectric + Room's new driver API is fragile across Room upgrades; the
  manual-schema approach avoids the broken helper and only depends on the
  exported JSON format.
- `retryAll` resets `attempts`/`nextAttemptAt` but leaves `lastError`; nothing
  reads it after a retry, so it is not asserted.
