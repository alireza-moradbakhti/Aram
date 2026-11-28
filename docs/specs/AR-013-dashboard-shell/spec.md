# AR-013 · Dashboard shell with safe-to-spend and month in/out

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/14
- Phase / Sprint: Phase 2 · Dashboard & insight / Sprint 3
- Estimate: 5 pts · P0
- Branch: `feat/AR-013-dashboard-shell` (stacked on `feat/AR-012-safe-to-spend`)
- Status: Done

## Problem

The dashboard is still the AR-046 shell: the hero and stat cards render
hard-coded numbers and the ViewModel is a 650 ms placeholder delay. It must
show the session profile and the real month totals, and it must handle loading,
empty-first-run, and error states.

## Goals / non-goals

### Goals
- ViewModel reads the current month (and the previous month for deltas) from
  `TransactionRepository` and `LedgerDirectory`, and feeds
  `SafeToSpendCalculator` from AR-012.
- Hero card shows the monthly safe-to-spend total, the daily amount, and days
  left; tapping it exposes an explainer callback.
- "This month" (expenses) and "Saved" (income − expenses) stat cards use the
  selected currency and formatting from `:core:money`/designsystem.
- Loading skeleton (existing), empty-first-run card, and an error state with
  retry.
- Previews for content, loading, empty, and error with sample state.

### Non-goals
- The explainer sheet itself (AR-014): only the tap callback is wired.
- Budgets and recurring bills as calculator inputs (AR-019/AR-030); both pass
  zero for now, so the "% of budget" pill is replaced by "% of income".
- Recent list (AR-018), budgets card (AR-020), trend chart (AR-017); those
  sections keep their placeholders.

## Acceptance criteria

- [x] Uses the session profile and formatting from `Money`.
- [x] Numbers match repository totals for the current month.
- [x] Screen previews with sample state.

## Technical design

`DashboardUiState`: `isLoading`, `hasError`, `month`, `safeToSpend`
(AR-012 `SafeToSpend`), `spent`, `saved`, `previousSpent`, `previousSaved`,
`isEmpty`.

`DashboardViewModel(repository, directory, clock)`:
- `reload` trigger + `flatMapLatest`; combines current-month rows,
  previous-month rows, and accounts.
- `catch` emits `hasError = true`; `retry()` bumps the trigger.
- Currency = dominant transaction currency, falling back to the first account,
  then `IRR`.
- `TRANSFER` rows are excluded from both in/out totals.
- Deltas: percent over/under the previous month for spent; signed difference
  of saved.

`:feature:dashboard` gains `implementation(project(":core:domain"))`.

## Data and migrations

None.

## Test plan

- `DashboardViewModelTest` (JVM): loading → content with month totals, transfer
  exclusion, empty month, error + retry, previous-month deltas.
- `DashboardSkeletonRenderTest` updated for the state-driven screen.
- CI-equivalent build, then device check (light + dark, empty + error, tap).

## Risks and open questions

- Stacked on AR-012 so the calculator is available; retarget the PR to `dev`
  once AR-012 merges.
- The design's "68% of budget" pill needs budget data; until AR-019 the pill
  shows "% of income spent" instead.