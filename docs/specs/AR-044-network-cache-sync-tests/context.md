# Context — AR-044 · Network, cache and sync test suite

## Starting point

- Base: `feat/AR-043-pull-sync-conflicts` after the AR-043 commit `3b6615d`
  (PR #69).
- AR-043 added the cursor pull, tombstones, the 15-minute worker and the
  Settings sync UI; its tests live in `core:data`.

## Relevant code

- `core/database/src/main/java/com/aram/core/database/AramDatabase.kt` —
  `MIGRATION_1_2`, `MIGRATION_2_3`, `MIGRATIONS`.
- `core/database/schemas/com.aram.core.database.AramDatabase/{1,2,3}.json` —
  exported schemas (the tests rebuild old versions from these).
- `core/database/.../dao/SyncOutboxDao.kt` — due/attempts/retry/discard API.
- `core/database/.../dao/LedgerWriteDao.kt` — atomic write + enqueue.
- `core/network/.../mock/MockApiEngine.kt`, `MockApiStore.kt` — duplicate
  budget returns `MockOutcome.Error(409, …)`.
- `feature/transactions/.../TransactionsScreen.kt` —
  `outboxStatus`/`onRetryOutbox`/`onSyncNow` parameters under test.
- `core/data/src/test/.../AuthRepositoryImplTest.kt` and
  `outbox/OutboxSyncImplTest.kt` — existing coverage kept as-is.

## Conventions

- Tests are `testDebugUnitTest` (JVM); Robolectric where Android APIs are
  needed; no emulator.
- No `!!` in `src/main`; tests may use it but none were needed.
- Amounts stay in minor units; no logging of ledger content.

## Dependencies

- `:core:database` test deps: JUnit, `androidx.test.ext:junit`,
  kotlinx-coroutines-test, Robolectric 4.18.1, `androidx.room:room-testing`.
- `feature:transactions` test deps: `androidx.compose.ui:ui-test-junit4`,
  `ui-test-manifest` (debug), Robolectric.

## Decisions log

- 2026-11-22 — Dropped `MigrationTestHelper` for a schema-JSON rebuild: Room
  2.8.5's helper creates its own `SupportSQLiteDriver` that rejects
  Robolectric's database path. Opening through `Room.databaseBuilder` with the
  real migrations still validates the produced schema and identity hash.
- 2026-11-22 — `retryAll` leaves `lastError` in place; the UI derives failures
  from `attempts`, so the DAO is left unchanged and the test asserts only
  `attempts`/`nextAttemptAt`.

## Verification log

- `./gradlew :core:database:testDebugUnitTest` — 17 tests, pass.
- `./gradlew :core:network:testDebugUnitTest :feature:transactions:testDebugUnitTest`
  — pass (including `TransactionsScreenTest` and the duplicate-budget case).
- Full `:app:assembleAlphaDebug test -PbuildDate=20261122` — BUILD SUCCESSFUL
  (556 tasks, all variants).
