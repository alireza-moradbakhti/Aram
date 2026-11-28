# Context — AR-017 · Six-month trend chart on Canvas

## Starting point

- Base: `dev` at the AR-046 merge; `DashboardViewModel` is still the 650 ms
  placeholder with no repositories, and `DashboardScreen` shows static demo
  cards (hero, tiles, actions, Budgets, Recent, This week).

## Relevant code

- `feature/dashboard/.../DashboardViewModel.kt` — placeholder `isLoading`
  timer.
- `feature/dashboard/.../DashboardScreen.kt`
  - `DashboardContent` order: greeting, hero, stats, actions, Budgets, Recent,
    This week.
  - `WeekChart()` (line ~670) — Box-based bars with green/orange gradients;
    the visual reference for the new Canvas chart.
- `core/domain/.../TransactionRepository.kt` — `observeTransactions(from, to)`.
- `core/model/.../Transaction.kt` — `occurredAt` (epoch millis), `type`,
  `amountMinor`.
- `app/.../di/DomainModule.kt` — provides `Clock.systemDefaultZone()`.
- Design: `Designs/Aram/index.html:2536–2549` ("This week" card anatomy),
  tokens `chartStart/chartEnd`, `secondaryHi` for the orange series.

## Conventions

- Long minor units; no logging of amounts; no analytics; no `!!` in
  `src/main`.
- Screens stay stateless; the ViewModel owns windows and the `Clock`.

## Decisions log

- Card goes after "This week" so the insight cluster reads: month (tiles),
  week (bars), six months (trend).
- Paired bars rather than lines: matches the existing week chart and keeps the
  "income vs expense" comparison obvious in dark mode.
- Bars normalized to the largest month; labels always drawn so zero months are
  visible slots, not gaps.

## Verification log

- `./gradlew :feature:dashboard:testDebugUnitTest` — green: `TrendMathTest` (7),
  `DashboardViewModelTest` (6), new `DashboardTrendCardTest` (2), AR-046 render
  tests.
- `./gradlew :app:assembleAlphaDebug test` — BUILD SUCCESSFUL.
- Device emulator-5554 (Pixel 9 Pro AVD, Android 16, 1280×2856):
  - Dark: `.tmp/ar017/01-trend-dark.png` — six labels Jun–Nov, zero months
    (Jun–Aug) as empty labeled slots, paired green/orange bars for Sep–Nov
    (85,000,000 salary inflows vs the month expenses), legend dots readable.
  - Light: `.tmp/ar017/02-trend-light.png` — same data, card keeps its
    contrast.
  - Empty history: unreachable on the demo backend (November seeded, AVD not
    rootable), asserted in `DashboardTrendCardTest` (copy replaces labels).
  - logcat: no FATAL and no app errors.
- Compile fixes: zoned test helpers needed `.toInstant().toEpochMilli()` (a
  `ZonedDateTime` has no `toEpochMilli`).