# Context — AR-014 · Safe-to-spend explainer sheet

## Starting point

- Base: `feat/AR-013-dashboard-shell` @ `88494db` (stacked), which sits on
  `feat/AR-012-safe-to-spend` and `dev`.
- `DashboardRoute` takes `onExplainSafeToSpend` and `AramApp` passes `{}`;
  the hero's click already routes to it.

## Relevant code

- `feature/dashboard/.../DashboardScreen.kt:83-99` — route and screen
  signatures; `SafeToSpendHero(result, onClick)` at `:564`.
- `core/domain/.../safetospend/SafeToSpendCalculator.kt` — `SafeToSpend`
  (`total`, `daysRemaining`, `dailyAmount`, `breakdown`) and
  `SafeToSpendBreakdown` (`income`, `spent`, `plannedBudgets`, `upcomingBills`,
  `clampAdjustment`, `total`, `sum()`).
- `core/designsystem/.../text/MoneyFormat.kt` — `formatMinor`,
  `formatSignedMinor`.
- `feature/settings/.../SettingsScreen.kt:576-648` — `ModalBottomSheet` sheet
  style to mirror (30 dp top corners, drag handle, close icon button).
- `app/.../navigation/AramApp.kt` — DashboardRoute call site.

## Conventions

- Stateless screens; visibility state lives in the route.
- Money stays in minor units; call `formatMinor` only at render time.
- No logging of amounts.

## Dependencies

- AR-013 (state + hero) and AR-012 (breakdown).
- Planned budgets and upcoming bills are zero until AR-019/AR-030; the rows
  still render so the sheet's structure does not change later.

## Decisions log

- 2026-11-28 — Stacked the branch on AR-013, because the sheet renders the
  `SafeToSpend` state introduced there.
- 2026-11-28 — The sheet shows all breakdown lines (not only non-zero ones) so
  "every row maps to the breakdown" is visible; the clamp adjustment line only
  appears when it is non-zero because it is not a user concept.
- 2026-11-28 — Removed the unused `onExplainSafeToSpend` plumbing on
  `DashboardRoute`; the route now owns the sheet.

## Verification log

- 2026-11-28 — `:feature:dashboard:testDebugUnitTest` — 12 tests, 0 failures
  (`SafeToSpendExplainerTest` 4: rows + spoken values, clamped copy,
  empty-month copy, advice matrix).
- 2026-11-28 — `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261128 --console=plain`
  — BUILD SUCCESSFUL.
- 2026-11-28 — Device `emulator-5554` (Pixel_9_Pro, Android 16), light + dark:
  - Hero tap opens the sheet; rows read `45,000,000`, `−18,644,000`, `0`, `0`,
    `26,356,000`, `3 days`, `≈ 8,785,333 IRR`; advice names the daily cap and
    November.
  - `uiautomator dump` shows the spoken labels: "Income this month:
    45,000,000 IRR", "Spent so far: minus 18,644,000 IRR", "Planned budgets:
    0 IRR", "Upcoming bills: 0 IRR", "Safe to spend: 26,356,000 IRR",
    "Days left: 3 days left in November", "Daily amount: 8,785,333 IRR a day";
    the close button is "Close" and the scrim is "Close sheet".
  - Dismissed by the close button, system back, and scrim tap — sheet gone each
    time; no crashes or ANRs in logcat.
  - The clamped/no-income copy is not reachable with the seeded ledger and is
    covered by `SafeToSpendExplainerTest`.