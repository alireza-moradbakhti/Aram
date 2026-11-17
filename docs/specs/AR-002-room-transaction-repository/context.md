# Context — AR-002 · Implement the Room-backed transaction repository

## Starting point

- Base: `feat/AR-001-transaction-repository` (stacked; contracts live there).
- `dev` @ `400e1a5` plus AR-001.

## Relevant code

- `core/domain/.../repository/TransactionRepository.kt`,
  `LedgerLookup.kt` — contracts to implement (range/recent added here).
- `core/data/.../repository/AuthRepositoryImpl.kt` — pattern: `safeOutcome`,
  `withContext(io)`, SQLite-aware error mapping, session scoping.
- `core/data/.../di/DataModule.kt` — Hilt `@Binds` module.
- `core/database/.../dao/TransactionDao.kt` — existing `upsert`,
  `observeRecent`, `observeTotal`.
- `core/database/.../entity/*.kt` — `userId`, `createdAt`, `updatedAt`,
  `deletedAt` columns already exist.
- `core/datastore/.../SessionDataStore.kt` — current user id flow.

## Conventions

- Repositories never throw; `Outcome` for writes, raw flows for reads with a
  catch that logs and emits empty.
- Money is `Long`; ids are UUID strings; timestamps are epoch millis.
- Feature modules must not import `:core:database`.

## Dependencies

- AR-001 (contracts), AR-005 (range observation consumer), AR-018 (recent).

## Decisions log

- `SessionStore` interface added so unit tests can fake the session.
- Range defaults span all epochs so existing single-argument callers keep
  working.
- Blank notes become `null` to match the server contract.

## Verification log

- `./gradlew :core:data:testDebugUnitTest :core:domain:testDebugUnitTest` — pass
  (11 repository cases; domain still green).
- `grep -rn "com.aram.core.database" feature/*/src/main` — no matches.
- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261117` — BUILD SUCCESSFUL
  (507 tasks).
- Device smoke: alpha installed and launched on the Galaxy A53 (no UI surface
  changed in this task).
