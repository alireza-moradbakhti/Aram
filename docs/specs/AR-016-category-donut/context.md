# Context — AR-016 · Category donut chart on Canvas

## Starting point

- Base: `dev` at `58277b5`. The dashboard here is still the AR-046 skeleton
  with example tiles; the data-driven Recent/budgets work lives on the open
  AR-018/AR-019/AR-020 branches and is not part of this branch.

## Relevant code

- `feature/dashboard/.../DashboardViewModel.kt` — placeholder `isLoading`
  timer to keep the skeleton state-driven.
- `feature/dashboard/.../DashboardScreen.kt` — `DashboardContent` order:
  greeting, hero, stats, actions, Budgets, Recent, This week; card anatomy in
  the "This week" `GlassCard` (title + chip).
- `core/domain/.../TransactionRepository.kt` — `observeTransactions(from, to)`.
- `core/domain/.../LedgerDirectory.kt` — `observeCategories()`.
- `core/model/.../Category.kt` — `colorArgb`, `kind`.
- `core/designsystem/.../text/MoneyFormat.kt` — `formatMinor`.
- `core/designsystem/.../component/` — `GlassCard`, `AramChip`,
  `AramTextStyles.statValue`.
- Design: `Designs/Aram/index.html:2536–2549` ("This week" card anatomy),
  tokens for surfaces and radii in `style.css`.

## Conventions

- Long minor units everywhere; formatting via `formatMinor(amount, digits)`.
- No logging of amounts; no analytics; no `!!` in `src/main`.
- Screens stay stateless; the ViewModel owns the month window using the
  injected `Clock`.

## Decisions log

- Group by the expense row's own category (no parent roll-up); income rows are
  skipped by `kind`.
- Percentages are integers allocated by largest remainder so they always sum to
  100; sweeps are rounded to 0.01° with the last slice absorbing the remainder.
- Fallback colour is the theme primary when `colorArgb` is transparent; empty
  state keeps a track ring instead of hiding the card.
- The entrance animation runs once when content replaces the skeleton.

## Verification log

- `./gradlew :feature:dashboard:testDebugUnitTest` — green: `DonutMathTest`
  (6), `DashboardViewModelTest` (5), new `DashboardDonutCardTest` (2) and the
  AR-046 render tests.
- `./gradlew :app:assembleAlphaDebug test` — BUILD SUCCESSFUL.
- Device emulator-5554 (Pixel 9 Pro AVD, Android 16, 1280×2856):
  - Dark: `.tmp/ar016/01-launch-dark.png`, scroll `.tmp/ar016/02-scroll-dark.png`
    — "Where it went" card with Housing 57% / Groceries 12% / Shopping 10% /
    Transport 7%, total 20,889,000 (real November rows).
  - Light: `.tmp/ar016/03-scroll-light.png` — same data, card readable.
  - Empty state: no month with no rows can be reached on the demo device (the
    mock API seeds November; no root to shift the clock), so the empty card is
    asserted in `DashboardDonutCardTest` ("No expenses yet this month", centre
    `0`, no legend rows) and rendered in the AR-046 content screenshot.
  - logcat after launch/scroll: no FATAL, no app errors, no amounts logged.
- Root compile fix: `DonutSlice` had to stop being `internal` because the
  public `DashboardScreen(donut = …)` / `DonutUiState` parameter exposes it.