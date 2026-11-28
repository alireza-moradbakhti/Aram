# Context — AR-010 · Search and filter transactions

## Starting point

- Base branch / commit: `origin/dev` @ `58277b5c3866391a75a558f6be02857f5589a93a`
- Working branch: `feat/AR-010-search-filter`
- Queue note: AR-008 (#9) was skipped because its "row → edit" acceptance
  criterion needs AR-006's edit sheet, still unmerged in PR #76.

## Relevant code

- `feature/transactions/.../presentation/TransactionsScreen.kt` — app bar
  (search/filter buttons currently `onClick = {}`), month nav, segmented scope,
  stats, day groups, empty state, previews.
- `feature/transactions/.../presentation/TransactionsViewModel.kt` — month +
  scope `combine → flatMapLatest → observeMonth`, sync/outbox merging,
  `buildState`, day grouping, currency selection.
- `core/database/.../dao/TransactionDao.kt` — `observeRange`, `observeRecent`,
  soft-delete conventions.
- `core/domain/.../repository/TransactionRepository.kt` — repository contract
  (observe-only reads + write operations).
- `core/data/.../repository/TransactionRepositoryImpl.kt` — `observeLedger`
  (session `flatMapLatest`, catch → empty, `flowOn(io)`).
- `core/data/src/test/.../TestDoubles.kt` — `FakeTransactionDao`.
- `feature/transactions/src/test/.../TransactionsViewModelTest.kt` —
  `FakeRangeRepository`, test dispatcher/clock conventions.
- `feature/transactions/src/test/.../TransactionsScreenTest.kt` — Compose rule
  and state fixtures.
- Design: `Designs/Aram/index.html` Activity app bar (search/filter icons,
  lines 1671–1684) and "No matches" empty card (lines 775–783);
  `style.css` `.chip`, `.field`, `.sheet`, `.empty` define the visual language.

## Conventions

- ViewModels are Hilt-injected and expose one `StateFlow`; screens are stateless
  where practical.
- Money as `Long` minor units; no analytics; no logging of amounts/notes.
- Room queries always scope `userId` and exclude tombstones; parameters, never
  interpolation.
- Search text is user content — never log it.

## Dependencies

- None unfinished: the Activity list, scope segmented control, sync and outbox
  banners already exist on `dev`.
- AR-009 transfers keep rendering as outflows in the Expenses scope; search
  inherits the existing scope predicate.

## Decisions log

- 2026-11-28 — Type filtering reuses the existing All/Expenses/Income
  segmented control instead of duplicating a type picker in the sheet; the
  applied chip reflects the scope when it is not All.
- 2026-11-28 — `TransactionFilter.excludeType` encodes the Expenses scope
  (exclude `INCOME`, keep `TRANSFER`); a single `type` parameter cannot express
  "all but income".
- 2026-11-28 — Date range ships as presets (Any time / This month / Last 30
  days / This year) mapped to half-open epoch millis with the injected `Clock`;
  custom pickers are out of scope.
- 2026-11-28 — In results mode the month title and stats are hidden and
  replaced by a results count + applied chips, because monthly totals would be
  wrong for a cross-month result set.
- 2026-11-28 — Debounce is 0 ms for a blank query and 300 ms otherwise, so the
  first load and clear-all are instant while typing is throttled.
- 2026-11-28 — Search text stays in screen-local `rememberSaveable` state; the
  ViewModel only receives debounced changes and owns applied filters. Clear-all
  from the no-matches card clears both.
- 2026-11-28 — Implementation detail: applied chips show only category and date
  preset. The scope chip was dropped after the screen test found two "Expenses"
  nodes (segmented + chip); the segmented control already carries the type.
  `TransactionsUiState.query` was added so the results header/card can name the
  debounced query.

## Verification log

- Focused unit tests: `:core:database:testDebugUnitTest` (302 total, new
  `TransactionSearchDaoTest` 10/10), `:core:data:testDebugUnitTest`
  (`TransactionRepositoryImplTest` 16/16, 2 new), `:feature:transactions:testDebugUnitTest`
  (`TransactionsViewModelTest` 22/22 with 8 new, `TransactionsScreenTest` 7/7
  with 4 new) — all pass.
- CI-equivalent: `./gradlew :app:assembleAlphaDebug test
  -PbuildDate=20261128 --console=plain` — BUILD SUCCESSFUL.
- Device: emulator-5554 (Pixel 9 Pro, Android 16, 1280x2856). Light and dark.
  - Month view unchanged; search icon opens a glass field with keyboard focus.
  - `winter` → "3 results · Across all months" with Nov/Oct/Sep rows; month nav
    and stats hidden; Clear all present.
  - `coffee` → no-matches card "No matches for “coffee”" + Clear filters; the
    action closes search and restores the month view.
  - Filter sheet: Shopping + Last 30 days → Apply → "1 result", chips
    "Shopping"/"Last 30 days", filter icon dot; only the 13 Nov row matches.
  - Clear all resets chips, dot, scope and returns to the month view.
  - Dark mode re-checked for results mode and sheet; no visual regressions.
  - logcat: no crashes; only a benign HWUI EGL warning; no note/amount text in
    logs.