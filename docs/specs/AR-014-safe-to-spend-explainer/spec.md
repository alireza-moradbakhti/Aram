# AR-014 · Safe-to-spend explainer sheet

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/15
- Phase / Sprint: Phase 2 · Dashboard & insight / Sprint 4
- Estimate: 2 pts · P1
- Branch: `feat/AR-014-safe-to-spend-explainer` (stacked on `feat/AR-013-dashboard-shell`)
- Status: Done

## Problem

Tapping the dashboard hero runs an empty callback. The number looks arbitrary;
users cannot see how income, spending, commitments and days left produce the
daily figure.

## Goals / non-goals

### Goals
- Bottom sheet opened from the hero, in the app's sheet style (30 dp top
  radius, drag handle, close action).
- Rows for income, spent, planned budgets, upcoming bills, the clamp
  adjustment (only when non-zero), days left, daily amount, and the total.
- Plain-language lead plus one suggested action derived from the result.
- Explanations for a zero or clamped result.
- Screen-reader labels that read label and value as one sentence.

### Non-goals
- Editing budgets or bills from the sheet (AR-019/AR-021).
- Charts or history inside the sheet.
- Analytics of sheet opens.

## Acceptance criteria

- [x] Every row maps to the calculator breakdown.
- [x] The sheet explains a zero or clamped result.
- [x] Screen reader labels describe each value.

## Technical design

- `SafeToSpendExplainerSheet(result: SafeToSpend?, onDismiss: () -> Unit)` in
  `feature/dashboard/presentation`; a `ModalBottomSheet` like the account sheet.
- Rows are driven by `SafeToSpendBreakdown` so the sum invariant holds:
  income + spent + planned budgets + upcoming bills + adjustment = total.
  The adjustment row appears only when non-zero.
- `explainerAdvice(result, month)` (pure, internal) picks one sentence:
  - clamped (adjustment > 0): "Planned spending is higher than income, so
    safe-to-spend is pinned at zero." + trim/earn suggestion;
  - no income: "Add this month's income to see how much is safe to spend.";
  - otherwise: "Keep daily spending at or under <daily> to finish <month> on
    track."
- Route owns `rememberSaveable` visibility; the external
  `onExplainSafeToSpend` parameter on `DashboardRoute` goes away, and
  `AramApp` no longer passes it.
- Each row uses `Modifier.semantics(mergeDescendants = true) {
  contentDescription = "<label>: <value> <ISO>" }`; the title is a heading and
  the close button has a "Close" description.

## Data and migrations

None. The sheet renders the `SafeToSpend` already in `DashboardUiState`.

## Test plan

- Unit: `explainerAdvice` for on-track, clamped, and no-income results.
- Robolectric render: rows, clamp copy, and content descriptions; light + dark
  screenshots are not committed.
- Device: open from the hero in both themes, dismiss via close and scrim, check
  TalkBack-style descriptions through the semantics tree.

## Risks and open questions

- The design prototype has no explainer sheet; styling follows the existing
  account/add sheets and `tokens.json`.
- Stacked on AR-013 (itself on AR-012) so the hero callback and state exist.