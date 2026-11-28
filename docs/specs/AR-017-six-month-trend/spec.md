# AR-017 · Six-month trend chart on Canvas

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/18
- Phase / Sprint: Phase 2 · Dashboard & insight / Sprint 4
- Estimate: 3 pts · P1
- Branch: `feat/AR-017-six-month-trend` (from `dev`)
- Status: In review

## Problem

The month tiles answer "how much this month" but not "is it getting better".
A compact six-month income/expense chart makes the direction visible without a
chart library.

## Goals / non-goals

Goals

- Bucket income and expense by calendar month for the last six months, in the
  device time zone.
- Custom Compose Canvas paired bars with month labels and a compact In/Out
  legend.
- Pure Kotlin bucketing with tests around month boundaries and zone offsets.
- Months without rows render as zero-height bars, not gaps.
- Empty state when there is no history at all.

Non-goals

- Per-category trends, drill-down, or tooltips (AR-016 covers categories).
- Real income/expense totals elsewhere on the dashboard (AR-018+).
- Localized month abbreviations: the dashboard already uses `Locale.US`.

## Acceptance criteria

- [x] Buckets respect the device time zone and month boundaries.
- [x] Missing months render as zero, not gaps.
- [x] Chart remains readable in dark mode.

## Technical design

`feature:dashboard`

- `presentation/TrendMath.kt` — pure data:
  - `MonthBucket(label, incomeMinor, expenseMinor)`.
  - `TrendUiState(buckets)` with `isEmpty` when every bucket is zero.
  - `buildMonthlyTrend(rows, endMonth: YearMonth, zone: ZoneId, months: Int = 6)`:
    - bucket `occurredAt` by `Instant…atZone(zone).toLocalDate()`'s `YearMonth`;
    - keep `[endMonth − 5, endMonth]` only; oldest → newest;
    - always emit `months` buckets so missing months are zero;
    - sum `INCOME` and `EXPENSE` separately; `TRANSFER` rows are ignored;
    - labels via `DateTimeFormatter.ofPattern("MMM", Locale.US)`.
- `DashboardViewModel` gains `TransactionRepository` + `Clock`; observes the
  half-open `[sixMonthsStart, nextMonthStart)` window and maps rows onto
  `TrendUiState`. The 650 ms skeleton placeholder stays.
- `DashboardScreen`: `SixMonthsCard` after "This week" — `GlassCard` head
  ("Six months" + In/Out legend dots), 96 dp Canvas with six paired bars
  (income green, expense orange, rounded tops), 700 ms ease-out height
  animation, 10 sp month labels under each group. Empty history swaps the
  canvas for faint copy.
- `feature:dashboard` gains `:core:domain`.

## Data and migrations

No schema change. Reads `observeTransactions(from, to)`; no writes.

## Test plan

- JVM `TrendMathTest`: always six buckets; zone boundary (23:59 vs 00:00),
  cross-zone instants, income/expense split, transfers ignored, out-of-window
  rows ignored, empty rows → `isEmpty`.
- JVM `DashboardViewModelTest`: placeholder state, six buckets on a fixed
  clock, totals per month, empty history.
- Robolectric `DashboardTrendCardTest`: card copy, legend and labels; empty
  state copy instead of labels.
- Device: real November data light/dark, no crashes; screenshots in `.tmp/`.

## Risks and open questions

- The prototype has no six-month chart; the card reuses the "This week" card
  anatomy and its bar colours so the dashboard reads as one system.
- Bars are normalized to the largest month so a single spike flattens the
  rest; acceptable for six data points.