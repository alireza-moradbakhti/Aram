# AR-015 · Room instrumented tests for transaction queries

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/16
- Phase / Sprint: Phase 1 · Transactions core / Sprint 4
- Estimate: 2 pts · P1
- Branch: `test/AR-015-room-instrumented-tests`
- Status: Done

## Problem

`TransactionDao` backs the transaction list, month totals, recent strip and the
sync tombstones, but its query semantics were only covered indirectly. There
were no instrumented DAO tests, and `observeTotal` used an inclusive `BETWEEN`
despite a `toExclusive` parameter, so a transaction at the first instant of the
next month could leak into the previous month's total.

## Goals / non-goals

Goals

- Run the transaction queries against a real SQLite engine on an emulator via
  an in-memory Room database, one fresh database per test.
- Cover month range boundaries: first and last instants of the month are
  included, the first instant of the next month is not.
- Cover soft-delete filtering across every read path (range, recent, by id,
  totals) and both tombstone helpers.
- Cover income/expense totals separately.

Non-goals

- Migrations (covered by `AramDatabaseMigrationTest`), outbox behavior
  (`SyncOutboxDaoTest`, `LedgerWriteDaoTest`), and repository logic.
- Running `connectedAndroidTest` in CI; there is no emulator job yet (the
  acceptance criterion is "once the emulator job lands").

## Acceptance criteria

- [x] Tests pass on an emulator and in CI once the emulator job lands.
- [x] Boundary dates at month start and end are covered.
- [x] Deleted rows never appear in results.

## Technical design

`:core:database`

- `build.gradle.kts` — set `testInstrumentationRunner` to
  `androidx.test.runner.AndroidJUnitRunner`; add `androidx.test.ext:junit`
  (with Espresso bringing the runner) to `androidTestImplementation`.
- `TransactionDao.observeTotal` — replace `occurredAt BETWEEN :fromInclusive
  AND :toExclusive` with `occurredAt >= :fromInclusive AND occurredAt <
  :toExclusive`, matching `observeRange` and the parameter name. No production
  caller existed yet, so this only aligns the contract before it is consumed.
- `src/androidTest/.../TransactionDaoTest.kt` — 7 tests using
  `Room.inMemoryDatabaseBuilder` in `@Before`, `db.close()` in `@After`, and
  `runBlocking`/`Flow.first()` for suspend and flow queries:
  - range includes the first/last instants, excludes the next month;
  - totals use the same half-open range (rows before/after excluded);
  - soft-deleted rows vanish from range, recent, by-id, observe-by-id and
    totals; a second delete is a no-op;
  - income and expense totals are separate and skip deleted rows;
  - recent returns newest live rows up to the limit;
  - `softDeleteMissing` tombstones absent rows but spares `pendingPush` rows
    and sets `updatedAt`/clears the queued flag;
  - `softDeleteAllInRange` clears the window except `pendingPush` rows and
    rows outside the range.

## Data and migrations

None. Tests run against the current schema (version 3) in memory.

## Test plan

- `./gradlew :core:database:connectedDebugAndroidTest -PbuildDate=20261128`
  on emulator-5554 — 7/7 pass.
- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261128` — regression
  check for the whole repo (unit tests, Robolectric DAO tests).

## Risks and open questions

- The `observeTotal` boundary fix changes behavior for any future caller: a
  row exactly at `toExclusive` no longer counts. That is the intended
  half-open contract already used by `observeRange`.
- Instrumented tests currently run locally only; CI needs an emulator job
  before they gate merges.