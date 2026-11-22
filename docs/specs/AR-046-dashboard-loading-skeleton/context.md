# Context — AR-046 · Dashboard loading skeleton

## Starting point

- Base: `dev` at `400e1a5` (AR-038 merged). `feature/dashboard` is unchanged by
  the AR-039…AR-044 stack.
- `DashboardScreen` is a stateless column with example tiles and no ViewModel.

## Relevant code

- `feature/dashboard/.../presentation/DashboardScreen.kt` — the loaded layout
  and all private tiles (`SafeToSpendHero`, `StatTile`, `ActionTile`,
  `RecentRow`).
- `core/designsystem/.../Theme.kt` — `AramColors.surface1`, `border`,
  `AramShapes`.
- `core/designsystem/.../Controls.kt` — `AramSectionHeader` (top 24 / bottom 12).
- `core/designsystem/.../Typography.kt` — `AramTextStyles.heroAmount`
  (38 sp / 44 line), `statValue` (20 / 26).
- Design source: `Designs/Aram/index.html` lines 508–552 ("Loading · skeleton")
  and 720–727 ("Loading rules"); `.sk*` styles at `style.css` 1685–1704.

## Conventions

- Stateless screens + one Hilt ViewModel per feature;
  `collectAsStateWithLifecycle`.
- Radius tokens: 10/14/18/22/28 dp (`AramShapes`); hero 28, cards 22, tiles 14.
- No hardcoded colours: skeleton uses `surface1`/`border`; shimmer highlight is
  white 9% when the theme background is dark, black 4.5% otherwise.
- No `!!` in `src/main`; amounts are never logged.

## Dependencies

- `feature:dashboard` gains KSP + Hilt, lifecycle-compose, hilt-navigation.
- Test-only: JUnit, kotlinx-coroutines-test, Robolectric, Compose UI test,
  `ui-test-manifest` (debug).
- `robolectric` is re-declared in `libs.versions.toml` here; the same line is
  added by AR-044, so the merge is additive.

## Decisions log

- 2026-11-22 — Fake load lives in the ViewModel (not the composable) so the
  skeleton tracks state; it is a placeholder for AR-013/AR-018 and carries a
  single comment.
- 2026-11-22 — Section heights pinned as constants rather than measured at
  runtime: the loaded layout already has deterministic line heights for the
  example content, and constants keep the crossfade CLS-free.
- 2026-11-22 — Shimmer highlight is derived from `colorScheme.background`
  luminance instead of `isSystemInDarkTheme()`: the first screenshot run showed
  the dark render still using the light 90% white band, because the theme can be
  forced independently of the system setting. Light mode uses black 4.5%, since
  light cards are already ~white and a white band disappears.

## Verification log

- 2026-11-22 — `./gradlew :feature:dashboard:testDebugUnitTest -PbuildDate=20261122`
  — BUILD SUCCESSFUL. `DashboardViewModelTest` 3/3, `DashboardSkeletonRenderTest`
  3/3; PNGs written to `screenshots/` (dark skeleton, light skeleton, dark
  content). Pixel check: dark band peak `(40,49,46)` on `(11,21,18)` background;
  light band `(242,243,243)` on `(247,250,248)`.
- 2026-11-22 — Full CI-equivalent build:
  `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261122 --stacktrace`
  — BUILD SUCCESSFUL in 2m 7s, 535 tasks (`app-alpha-debug.apk` packaged).
- 2026-11-22 — CI "Alpha build & test" on PR #71 — pass (3m 25s).
- Device sheet on emulator-5554 (Android 16), stacked integration build:
  - Skeleton appears on cold start and swaps to content; dark-mode and
    landscape renders look correct.
  - Fix found on device: the bottom content padding (16dp) ended under the FAB,
    so the Recent card's amounts were covered when scrolled to the end. Padding
    is now 96dp, matching the design's `nav-clear` space. Re-verified on device:
    the last card scrolls clear of the FAB.
  - Known cosmetic note: in landscape the FAB overlaps the hero balance; not
    changed, since the design source is portrait-first.
