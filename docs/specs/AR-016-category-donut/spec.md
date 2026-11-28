# AR-016 · Category donut chart on Canvas

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/17
- Phase / Sprint: Phase 2 · Dashboard & insight / Sprint 4
- Estimate: 3 pts · P1
- Branch: `feat/AR-016-category-donut` (from `dev`)
- Status: In review

## Problem

The dashboard tells users how much they spent, but not where. A compact
category donut with a legend gives the month a shape without a chart library,
and keeps the calm, glass-card look of the prototype.

## Goals / non-goals

Goals

- Custom Compose Canvas donut coloured by the category's own `colorArgb`.
- Legend rows with colour dot, category name, percentage and amount; the donut
  centre shows the month's total spent.
- Slices are computed outside Compose and unit-tested: sums to 360° within
  0.01° tolerance, largest-remainder integer percentages, no NaN.
- Zero-state: track ring only, centre `0`, faint "No expenses yet this month";
  no arcs and no NaN.
- Short entrance animation (700 ms ease-out) that respects the loaded state.

Non-goals

- Tap/drill-down into a category filter (Activity already supports it later).
- The six-month trend chart (AR-017) and real budget/safe-to-spend numbers.
- Parent/child category roll-up: expenses group by their own category.

## Acceptance criteria

- [x] Segments sum to 360 degrees within rounding tolerance.
- [x] Zero-state renders without NaN or empty arcs.
- [x] No chart library dependency.

## Technical design

`feature:dashboard`

- `chart/DonutMath.kt` — pure data: `CategorySpend`, `DonutSlice` and
  `buildDonutSlices(spending)`:
  - drop non-positive amounts; empty list when the total is 0.
  - sort by amount descending; sweep = amount / total × 360, values rounded to
    0.01°, last slice absorbs the remainder so the sum is exactly 360.
  - `percent` integers allocated with the largest remainder so they sum to 100.
- `DashboardViewModel` gains `TransactionRepository`, `LedgerDirectory` and
  `Clock`; combines the current-month expense rows (half-open
  `[monthStart, nextMonthStart)`) with categories and maps them onto
  `DonutUiState(totalMinor, fractionDigits, monthLabel, slices, isEmpty)`.
- `DashboardScreen`: `SpendingDonutCard` between Recent and This week —
  `GlassCard` head ("Where it went", month chip), 132 dp canvas with 18 dp
  stroke and rounded caps, animated sweep from 0, centre total and "spent"
  caption, legend to the right (dot, name, percent, amount). Empty state keeps
  the track ring and the faint copy.
- `feature:dashboard` gains `:core:domain` and `:core:money`.

## Data and migrations

No schema change. Reads `observeTransactions(monthStart, nextMonthStart)` and
`observeCategories()`; income rows are excluded by category kind.

## Test plan

- JVM: `DonutMathTest` — zero input; single category = 360° and 100%;
  two/three-way splits sum to 360 and 100; rounding leftovers land on the last
  slice; non-positive amounts ignored; deterministic order.
- Robolectric: existing dashboard skeleton tests must stay green.
- Device: real November data light/dark, empty month (via month data absent),
  no crashes; screenshots in `.tmp/`.

## Risks and open questions

- The prototype has no category donut; the card reuses the "This week" card
  anatomy so it reads as part of the same insight cluster.
- Categories without a usable `colorArgb` fall back to the primary accent.