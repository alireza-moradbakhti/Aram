# Context — AR-013 · Dashboard shell

## Starting point

- Base: `feat/AR-012-safe-to-spend` @ its head (stacked), which sits on
  `dev` @ `58277b5`.
- The dashboard ViewModel is a 650 ms placeholder (`DashboardViewModel.kt`);
  the screen renders fixed sample numbers and has skeleton previews.

## Relevant code

- `feature/dashboard/.../DashboardScreen.kt` — `DashboardRoute`,
  `DashboardScreen(displayName, isLoading)`, `SafeToSpendHero`, `StatTile`,
  skeleton + previews.
- `feature/transactions/.../TransactionsViewModel.kt:139-215` — combine of
  current/previous month rows + directory, dominant-currency helper, totals.
- `core/domain/.../safetospend/SafeToSpendCalculator.kt` (AR-012).
- `core/domain/.../repository/TransactionRepository.kt` — range-based
  `observeTransactions(from, to)`.
- `app/.../di/DomainModule.kt` — Hilt provides `java.time.Clock`.
- `core/designsystem/.../text/MoneyFormat.kt` — `formatMinor`, `formatSignedMinor`.
- Design: `Designs/Aram/index.html:2402-2452` (greeting, hero, stat grid).

## Conventions

- Stateless screens; all data through `DashboardUiState`.
- Money stays in minor units everywhere below the UI.
- `TRANSFER` legs are not income or expense.

## Dependencies

- AR-012 calculator (stacked branch).
- Budgets/bills inputs arrive with AR-019/AR-030.

## Decisions log

- 2026-11-28 — Stacked on AR-012 because the hero needs `SafeToSpendCalculator`;
  the PR base is the AR-012 branch.
- 2026-11-28 — Transfers excluded from in/out totals; they are already netted
  per account by AR-007.
- 2026-11-28 — "% of budget" hero pill becomes "% of income spent" until
  budgets exist; hidden when there is no income.
- 2026-11-28 — The explainer sheet is AR-014; this task wires the tap callback.

## Verification log

- 2026-11-28 — `./gradlew :feature:dashboard:testDebugUnitTest` — pass (5
  ViewModel tests + skeleton render test).
- 2026-11-28 — `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261128 --console=plain`
  — BUILD SUCCESSFUL (574 tasks, 1m46s).
- 2026-11-28 — Device `emulator-5554` (Pixel_9_Pro, Android 16), app
  `com.aram.app.alpha.debug`, mock API, demo user Ava Bennett:
  - Light and dark mode: greeting from the session profile, hero
    `26,356,000 IRR`, `≈ 8,785,333 a day for the next 3 days`,
    `41% of income`, `On track`.
  - Totals cross-checked on the Activity tab for the same month: SPENT
    `18,644,000` and INCOME `45,000,000` — dashboard "This month" equals SPENT,
    hero and "Saved" equal INCOME − SPENT, `41%` equals SPENT / INCOME, and
    the `11% under Oct` delta matches the Activity footer's `10.7%`.
  - Transfer legs (`Move to card` ±500,000) appear in Activity but are
    excluded from the dashboard totals.
  - Hero tap runs the wired callback without a crash (the explainer sheet is
    AR-014).
  - Empty and error states are not reachable with the seeded demo ledger; both
    branches are covered by `DashboardViewModelTest` (`empty month first-run`,
    `failure → retry`) and by the `Empty`/`Error` Compose previews.