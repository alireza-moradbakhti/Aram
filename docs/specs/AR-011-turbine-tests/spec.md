# AR-011 · Repository and ViewModel tests with Turbine

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/12
- Phase / Sprint: Phase 1 · Transactions core / Sprint 2
- Estimate: 3 pts · P1
- Branch: `test/AR-011-turbine-tests`
- Status: Done

## Problem

State behavior must be pinned before the UI grows. Most suites read
`uiState.value` after `advanceUntilIdle`, which proves the end state but not the
sequence, so regressions in loading → loaded, submitting → saved/failed, or a
silent dropped emission would pass unnoticed.

## Goals / non-goals

### Goals
- Turbine-based flow assertions for the repository sync state and the ViewModel
  state streams that drive the transaction flows, settings sync health, and the
  dashboard placeholder.
- At least one failure path per ViewModel test class touched.
- Deterministic with `StandardTestDispatcher`; no Android framework in unit tests.

### Non-goals
- Rewriting existing value-based tests that already prove behavior.
- Undo: `dev` has no undo affordance yet (delete lives in AR-006); the issue's
  "undo" item is recorded as not applicable until that flow lands.
- Instrumented tests (AR-015).

## Acceptance criteria

- [x] Tests are deterministic with a test dispatcher.
- [x] No Android framework dependency in unit tests.
- [x] Coverage includes at least one failure path per ViewModel.

## Technical design

- Catalog already carries `kotlinx-coroutines-test` and `turbine`; add the
  `turbine` dependency to the dashboard and settings feature modules.
- `LedgerSyncRepositoryImplTest`: Turbine on `state` — REFRESHING then IDLE on a
  successful pull; FAILED on a 5xx; OFFLINE on an offline error.
- `TransactionsViewModelTest`: Turbine on `uiState` — first emission loading,
  second loaded with the month's rows; a scope change emits the filtered rows.
- `AddTransactionViewModelTest`: Turbine on `uiState` — `Idle → Submitting →
  Idle(saved)`; a repository failure emits `Submitting → Failed`.
- `SettingsViewModelTest`: Turbine on `syncStatus` — REFRESHING with queue
  counts, then FAILED with `failed > 0`.
- `DashboardViewModelTest`: Turbine — loading stays true before the delay,
  false after.
- `WelcomeViewModel` already covers failure in six cases; left untouched.

## Data and migrations

None.

## Test plan

Focused suites:
- `:core:data:testDebugUnitTest`
- `:feature:transactions:testDebugUnitTest`
- `:feature:settings:testDebugUnitTest`
- `:feature:dashboard:testDebugUnitTest`

Then the CI-equivalent `./gradlew :app:assembleAlphaDebug test`.

## Risks and open questions

- Turbine timeouts must be bounded by `runTest`; use `advanceUntilIdle` before
  `awaitItem` where the producer is a `stateIn(WhileSubscribed)` stream.