# AR-020 · Budgets screen with month selector

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/21
- Phase / Sprint: Phase 3 · Budgets & goals / Sprint 5
- Estimate: 5 pts · P0
- Branch: `feat/AR-020-budgets-screen` (stacked on `chore/AR-019-budgets-repository`)
- Status: Done

## Problem

The Budgets tab was a static demo screen: fixed October numbers, no data, no
states. AR-019 added `BudgetRepository.observeProgress(month)`; this task turns
the tab into the real monthly plan view driven by Room.

## Goals / non-goals

Goals

- Month selector: previous/next arrows, a "This month" shortcut whenever the
  selected month is not the current one, and a subtitle that explains the
  month (`Resets 1 Dec · 3 days left`, `Closed 1 Nov`, `Starts 1 Dec · …`).
- Total budget card: allocated vs. spent vs. left, overall progress bar,
  daily allowance (`≈ X / day`) or `Month ended`, and `% remaining`.
- Per-category rows: icon, name, `spent / limit`, progress bar, percentage,
  remaining amount, and a tone (on track / near limit / over).
- Empty state with a create-budget CTA, including "no budgets in {month}".
- Loading skeleton while the first Room emission is in flight.

Non-goals

- Create/edit/delete flows and the over-budget summary banner (AR-021).
- Wiring budgets into sync/outbox (follow-up to AR-019).
- Rewriting the repository or schema; AR-020 only consumes
  `BudgetRepository`.

## Acceptance criteria

- [x] Totals match repository data for the selected month.
- [x] Over-budget categories are visually distinct but not alarming.
- [x] Progress is accessible (value + label, not color alone).

## Technical design

`:feature:budgets`

- `presentation/BudgetsViewModel.kt`
  - `month: MutableStateFlow<YearMonth>` seeded from `Clock`; `flatMapLatest`
    into `repository.observeProgress(month)`.
  - `BudgetsUiState` — month/isCurrentMonth, loading, currency + fraction
    digits, totals (limit/spent/remaining/fraction/percent), `daysLeft`,
    `perDayMinor`, `rows: List<BudgetRowUi>`, derived `isEmpty` and `status`.
  - Totals use the dominant currency by row count (falls back to `IRR`) so a
    minority-currency row cannot corrupt the headline.
  - Row tone: spent > limit → `OVER`; ≥ 80% → `NEAR_LIMIT`; else `OK`.
  - `daysLeft`: current month = `length - day + 1`; future = full length;
    past = 0 (with `Month ended` copy).
- `presentation/BudgetsScreen.kt` — stateless `BudgetsScreen(state, …)` plus
  `BudgetsRoute` (Hilt). Composes `AramAppBar`, `AramMonthNav`, `GlassCard`
  total card, category rows, `AramPrimaryButton("New budget")` and the
  rollover note. Test tags: `budgets-skeleton`, `budget-progress-total`,
  `budget-progress-<id>`.

`:core:designsystem`

- `AramMonthNav` gains an optional `onCurrentMonth`; when present a "This
  month" text action sits between the title block and the next arrow. Existing
  Transactions usage is unchanged (parameter defaults to null).

`:app`

- `AramApp.kt` routes the Budgets tab to `BudgetsRoute`; create/open/settings
  surface a "… coming soon" snackbar until AR-021 ships the editor.

## Data and migrations

None. Reads existing `budgets`, `categories`, `transactions` tables through
`BudgetRepository`.

## Test plan

Unit / Robolectric (`:feature:budgets:testDebugUnitTest`, 16 tests)

- `BudgetsViewModelTest` (7): totals and rows for the current month, tone
  thresholds at 79/80/101%, headline `OVER` when total spent exceeds limit,
  empty month, month navigation (`daysLeft` past/future), minority-currency
  row excluded from totals.
- `BudgetsScreenTest` (9, Robolectric 36, `w390dp`): totals, row copy, over
  copy and spoken descriptions, progress semantics, empty CTA, other-month
  empty title, "This month" visibility, arrow navigation, skeleton tag.

Manual (agent, emulator-5554, Pixel 9 Pro AVD / Android 16)

- Seeded five November budgets for the demo account; verified totals
  (7,600,000 limit / 6,259,000 spent / 1,341,000 left, ≈447,000/day), tones
  (on track, near limit, over by 160,000), past/future/current month
  subtitles, "This month" shortcut visibility, empty states, snackbars, light
  and dark.

## Risks and open questions

- Totals in a single dominant currency; per-row limits can still be in other
  currencies and are excluded from the headline (documented in AR-019).
- Creating a budget for a past/closed month is still offered; AR-021 decides
  whether the editor blocks it.