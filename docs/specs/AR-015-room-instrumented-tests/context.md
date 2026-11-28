# Context — AR-015 · Room instrumented tests for transaction queries

## Starting point

- Base: `dev` at the commit that includes AR-019 (PR #83 pending, not merged).
- `core/database` had Robolectric DAO tests only (`LedgerWriteDaoTest`,
  `SyncOutboxDaoTest`) and no `androidTest` source set or instrumentation
  runner.

## Relevant code

- `core/database/.../dao/TransactionDao.kt` — queries under test:
  `observeRange`, `observeTotal`, `observeRecent`, `observeById`,
  `findById(s)`, `softDelete`, `softDeleteMissing`, `softDeleteAllInRange`.
- `core/database/.../AramDatabase.kt` — schema v3, in-memory builder used by
  the tests.
- `core/database/build.gradle.kts` — runner and `androidTestImplementation`
  entries.
- Convention reference: `core/database/src/test/.../LedgerWriteDaoTest.kt`
  (in-memory builder, `runBlocking`, entity factory helpers).

## Conventions

- One fresh in-memory database per test; `db.close()` after.
- Fixed UTC instants for the October 2026 window so boundary assertions are
  deterministic regardless of the device zone.
- `runBlocking` for suspend DAO calls; `Flow.first()` for observable queries.
- JUnit4 `AndroidJUnit4` runner; assertions on ids/amounts only — no amounts
  logged.

## Dependencies

- Emulator `emulator-5554` (Pixel 9 Pro AVD, Android 16) for
  `connectedDebugAndroidTest`.
- `androidx.test.ext:junit` 1.3.0 and Espresso 3.7.0 (runner) from the
  version catalog.

## Decisions log

- `observeTotal` was inclusive on both ends (`BETWEEN`) although the parameter
  is named `toExclusive` — fixed to a half-open range to match `observeRange`.
  No production caller existed, so this is a contract alignment rather than a
  behavior change for users.
- Espresso is declared (not just `androidx.test.ext:junit`) because it brings
  `androidx.test:runner`, which provides `AndroidJUnitRunner` for the library
  module's test APK.
- Kept the Robolectric DAO tests as-is; the instrumented suite proves the real
  SQLite engine, the Robolectric ones stay fast for the JVM test task.

## Verification log

- `./gradlew :core:database:connectedDebugAndroidTest -PbuildDate=20261128`
  on emulator-5554 — BUILD SUCCESSFUL, 7 tests (first run caught an
  `Integer`/`Long` assertion mismatch in the tombstone test, fixed to `10L`).
- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261128` — see run
  below (regression check for the DAO query change).
- No app code or UI changed, so no separate device walkthrough was needed
  beyond the instrumentation run; logcat left clean.