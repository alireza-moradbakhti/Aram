# Context — AR-011 · Repository and ViewModel tests with Turbine

## Starting point

- Base: `dev` @ `58277b5`
- Branch: `test/AR-011-turbine-tests`
- Note: the catalog already lists `kotlinx-coroutines-test` and `turbine`, and
  `:core:data`, `:feature:transactions`, `:feature:onboarding` already use them.
  The remaining gap is sequence-level assertions, not infrastructure.

## Relevant code

- `core/data/src/test/.../LedgerSyncRepositoryImplTest.kt` — drives Ktor
  `MockEngine`, asserts `state.value` after the call.
- `feature/transactions/src/test/.../TransactionsViewModelTest.kt`,
  `AddTransactionViewModelTest.kt` — value reads after `advanceUntilIdle`.
- `feature/settings/.../SettingsViewModel.kt` — `syncStatus` is
  `combine(...).stateIn(WhileSubscribed(5_000))`.
- `feature/dashboard/.../DashboardViewModel.kt` — placeholder loader with a
  delay; no I/O, so its only "failure" surface is time-based.
- `gradle/libs.versions.toml` — `unitTest` bundle (`junit`,
  `kotlinx-coroutines-test`, `turbine`).

## Conventions

- `StandardTestDispatcher` + `Dispatchers.setMain` in every VM suite.
- No Robolectric in ViewModel tests; fakes only.
- Assertions on emissions, not sleeps.

## Dependencies

- None. AR-009 (transfers) is complete on its own branch and does not change
  these state streams.

## Decisions log

- 2026-11-28 — Keep existing value assertions and add Turbine sequence tests
  next to them; do not churn passing tests.
- 2026-11-28 — "Undo" is not implemented anywhere on `dev`; record it as not
  applicable and cover save success + failure instead.
- 2026-11-28 — Dashboard has no I/O failure path; its sequence test covers
  loading → content only.

## Verification log

- 2026-11-28 — Focused suites:
  `:core:data:testDebugUnitTest :feature:transactions:testDebugUnitTest
  :feature:settings:testDebugUnitTest :feature:dashboard:testDebugUnitTest`
  → BUILD SUCCESSFUL. `:feature:transactions` 30 tests, 0 failed.
- 2026-11-28 — First run failed 2 tests in `AddTransactionViewModelTest`: the
  Turbine consumer received the `onAmountChange` emission before the
  `Submitting` one. Fix: await the amount emission explicitly before
  `onSave()`. No production code change.
- 2026-11-28 — CI-equivalent
  `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261128 --console=plain`
  → BUILD SUCCESSFUL (574 tasks).
- 2026-11-28 — No production/UI change on this branch, so no device step;
  nothing new to inspect on the emulator.