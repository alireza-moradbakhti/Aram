# Context — AR-020 · Budgets screen with month selector

## Starting point

- Base: `chore/AR-019-budgets-repository` (AR-019 commit `0a98a80`), itself
  branched from `dev`; PR #83 is open into `dev`.
- `feature/budgets` contained only the original demo screen and no Gradle
  wiring for DI/tests.

## Relevant code

- `feature/budgets/.../BudgetsViewModel.kt` — new: month state machine +
  `BudgetRepository.observeProgress`; builds `BudgetsUiState`.
- `feature/budgets/.../BudgetsScreen.kt` — rewritten: stateless screen, route,
  total card, rows, empty state, skeleton.
- `feature/budgets/build.gradle.kts` — hilt/ksp plugins, test options
  (`isIncludeAndroidResources`), domain/money/lifecycle/hilt deps, unit-test
  deps (coroutines-test, compose-ui-test-junit4, robolectric).
- `core/designsystem/.../Controls.kt` — `AramMonthNav` optional
  `onCurrentMonth` (shared with Transactions; default null keeps old behavior).
- `app/.../navigation/AramApp.kt` — Budgets tab now hosts `BudgetsRoute`,
  placeholder snackbars until AR-021.
- Pattern follow: `feature/transactions` (repository + ViewModel + Robolectric
  tests), `GlassCard`, `AramPrimaryButton`, `AramChip`.

## Conventions

- Stateless `Screen(state, callbacks)` + `Route` with `hiltViewModel()`.
- Money stays `Long` minor units; `formatMinor` with the currency's fraction
  digits; `CurrencyCode.fromIsoCode` may throw for unknown codes, so fraction
  digits fall back to 0 via `runCatching`.
- No analytics, no `!!` in `src/main`, no sensitive values in logs.
- Tests assert semantics (content descriptions, `ProgressBarRangeInfo`) not
  colors alone.

## Dependencies

- AR-019 `BudgetRepository` / `BudgetProgress` / `BudgetRow` contracts.
- Design source: `Desktop\Project\Designs\Aram` — budgets phone block plus
  `.glass`, `.budget-hero`, `.budget-item`, `.bar`, `.chip-*`, `.tiny`.

## Decisions log

- Month navigation lives in the ViewModel so rotation keeps the selected
  month; "This month" only renders off-current.
- Headline totals use the dominant currency by row count; minority-currency
  rows still appear in the list with their own amounts.
- `daysLeft` for past months is 0 and the card shows `Month ended` instead of a
  daily allowance.
- Category count ("N active") uses the design's `.tiny` treatment (11sp, faint)
  rather than `AramSectionHeader`'s green action link — the shared header is
  for tappable actions ("See all").
- Create/settings/row taps are wired to placeholder snackbars; AR-021 replaces
  them with the editor.

## Verification log

- `./gradlew :feature:budgets:testDebugUnitTest -PbuildDate=20261128` —
  BUILD SUCCESSFUL, 16 tests (7 ViewModel + 9 Robolectric screen).
- `./gradlew :app:installAlphaDebug -PbuildDate=20261128` — BUILD SUCCESSFUL.
- Full CI-equivalent: `./gradlew :app:assembleAlphaDebug test
  -PbuildDate=20261128 --console=plain` — see PR checks / run below.
- Device (emulator-5554, Pixel 9 Pro AVD, Android 16, app
  `com.aram.app.alpha.debug`, demo account):
  - Content dark/light: totals 7,600,000 / 6,259,000 / 1,341,000, near-limit
    chip, rows Bills 85% near, Dining 113% over by 160,000, Groceries 69%,
    Subscriptions 66%, Transport 88% near, ≈447,000/day, 18% remaining.
  - Month nav: October 2026 (`Closed 1 Nov`, empty), December 2026
    (`Starts 1 Dec · 31 days`, empty), "This month" shortcut appears off-month
    and is absent on November.
  - Empty-state CTA, row tap and settings tap → placeholder snackbars.
  - Logcat: no FATAL/AndroidRuntime; no amounts/emails/tokens in app logs.
- Robolectric covers the loading skeleton (Room emission is instant on device,
  so the skeleton is not observable manually).
- Temporary seeding fixture (`SeedBudgetsFixtureTest`) was used to insert
  November budgets for the demo user, then deleted before commit.