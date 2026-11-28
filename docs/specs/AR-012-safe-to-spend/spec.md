# AR-012 · Safe-to-spend calculator with a testable breakdown

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/13
- Phase / Sprint: Phase 2 · Dashboard & insight / Sprint 3
- Estimate: 3 pts · P0
- Branch: `feat/AR-012-safe-to-spend`
- Status: Done

## Problem

The dashboard must answer "how much can I spend today?" with one number the user
can trust. The number has to be explainable line by line, and it must not depend
on Android or the wall clock in tests.

## Goals / non-goals

### Goals
- Pure Kotlin calculator: `(income − spent − remaining budget allocations − upcoming bills) / days remaining`.
- Return a breakdown object (one line per component) whose lines sum exactly to
  the displayed monthly total.
- Clamp the monthly pool at zero; floor the daily amount to whole minor units.
- Injectable `java.time.Clock` for deterministic tests.

### Non-goals
- Dashboard UI/wiring (AR-013) and the explainer sheet (AR-014).
- Reading budgets or recurring bills from repositories (AR-019/AR-030); the
  calculator takes already-summed inputs.

## Acceptance criteria

- [x] Tests cover mid-month, first day, last day, overspend, and no income.
- [x] The breakdown sums exactly to the displayed total.
- [x] No Android dependency.

## Technical design

Package `com.aram.core.domain.safetospend` in `:core:domain`:

- `SafeToSpendCalculator(clock: Clock)` — `calculate(month, income, spent,
  plannedBudgets, upcomingBills): SafeToSpend`.
- `SafeToSpendBreakdown` — signed contributions (`income`, `spent`,
  `plannedBudgets`, `upcomingBills` as negatives, `clampAdjustment` ≥ 0) with
  `sum(): Money`.
- `SafeToSpend` — `total: Money`, `daysRemaining: Int`, `dailyAmount: Money`,
  `breakdown`.

Rules:
- All inputs must share one currency (`require`).
- `daysRemaining` counts today through the month's last day, clamped to `1..monthLength`;
  a month already over behaves like the last day.
- `total = max(0, income − spent − plannedBudgets − upcomingBills)`;
  `clampAdjustment` is the amount the clamp added, so the lines always sum to
  `total`.
- `dailyAmount = total / daysRemaining` using floor division on minor units; the
  remainder stays unallocated (deliberately conservative).

`:core:domain` gains `api(project(":core:money"))`.

## Data and migrations

None.

## Test plan

- `SafeToSpendCalculatorTest` (JVM, fixed `Clock`): mid-month, first day, last
  day, overspend, no income, breakdown-sum invariant, floor rounding, currency
  mismatch, past month.

Then CI-equivalent `./gradlew :app:assembleAlphaDebug test`.

## Risks and open questions

- The issue formula subtracts "remaining budget allocations" after "spent";
  implemented literally. If budgets are meant to cap spending rather than be
  subtracted on top, that is a product decision for the explainer sheet (AR-014).