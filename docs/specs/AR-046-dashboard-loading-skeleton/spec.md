# AR-046 · Dashboard loading skeleton

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/55
- Phase / Sprint: Phase 2 · Dashboard & insight / Sprint 4
- Estimate: 3 pts · P1
- Branch: `feat/AR-046-dashboard-loading-skeleton` (from `dev`)
- Status: In review

## Problem

The dashboard renders its example tiles immediately, with no loading state. Once
real data arrives (AR-013/AR-018), the shell will flash empty or jump as content
lands. The prototype defines a "Loading · skeleton" phone and a "Loading rules"
panel: reserve the final layout, shimmer 1.3 s, keep the chrome real.

## Goals / non-goals

Goals

- A skeleton for the greeting, hero card, stat row, action tiles, budgets card
  and recent list that mirrors the loaded grid.
- Loading is a `DashboardViewModel` state, not a UI timer or a `LaunchedEffect`
  flag.
- Crossfade into content with matching section heights so nothing shifts.
- Dark and light, using the app's radius tokens; shimmer band clipped inside
  each block.
- Screenshots of both themes in the spec folder.

Non-goals

- Real dashboard data (AR-013/AR-018) — the ViewModel's fake initial load is a
  placeholder with a single comment pointing at the follow-up.
- The "Still working… after 8 s" retry: there is no request to time out yet;
  AR-041's repository state will drive that.
- The tab bar and status bar: they stay real, owned by the app shell.

## Acceptance criteria

- [x] Dashboard renders a skeleton matching the prototype at 390 dp while data loads.
- [x] Loaded content replaces the skeleton without a layout shift.
- [x] Both themes match; screenshots in the spec folder.
- [ ] Device pass on the Galaxy A53.

## Technical design

`feature:dashboard`

- `DashboardUiState(isLoading: Boolean = true)`.
- `DashboardViewModel` (`@HiltViewModel`): `uiState: StateFlow`, starts loading,
  clears `isLoading` once the placeholder source settles. Replaced by the
  cache-first repository flow in AR-013/AR-018.
- `DashboardRoute` collects the state with `collectAsStateWithLifecycle` and
  forwards `isLoading` to the stateless `DashboardScreen`.
- `DashboardScreen(displayName, isLoading, onSeeAll)`: a `Crossfade`
  (`tween(320)`) swaps `DashboardSkeleton()` for the existing content column.
- Skeleton primitives are local composables: `SkeletonBlock(shape, modifier)`
  paints `surface1` + `border` and runs one shimmer band (1.3 s linear) clipped
  to the block's shape. The highlight follows the theme (background luminance),
  not the system setting: white 9% in dark, black 4.5% in light — a 90%-white
  band is invisible over the near-white light cards.
- Section heights are constants shared with the skeleton so the loaded layout
  occupies the same vertical space:

| Section | Loaded | Skeleton |
| --- | --- | --- |
| Greeting row | 76 dp (56 text + 20 bottom) | 76 dp |
| Hero | 188 dp (20 padding, 44 amount line, 30 pills) | 188 dp |
| Stat tiles | 100 dp | 100 dp |
| Action tiles | 66 dp | 66 dp |
| Budgets header | 68 dp (8 extra top) | 68 dp |
| Budgets card | 116 dp (76 ring + 40 padding) | 116 dp |
| Recent header | 60 dp | 60 dp |
| Recent card | 214 dp (3 × 66 rows + 16 padding) | 214 dp |

## Data and migrations

None. No database or network changes.

## Test plan

- `DashboardViewModelTest` — starts loading, settles to content on the test
  scheduler.
- Robolectric screenshot/render test at 390 dp in dark and light: skeleton
  visible while loading, content after; PNGs written to
  `docs/specs/AR-046-dashboard-loading-skeleton/screenshots/`.
- Device: Galaxy A53 — cold start skeleton, crossfade, theme switch, rotation.

## Risks and open questions

- The crossfade only guarantees no jump for sections whose heights are pinned
  above; the week-chart card sits below the first viewport and is not part of
  the skeleton (the prototype skeleton stops at Recent).
- Font-scale > 1 grows the loaded text but not the skeleton's fixed dp blocks;
  the device pass checks default scale only.
