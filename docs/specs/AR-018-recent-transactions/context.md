# Context — AR-018 · Recent transactions on the dashboard

## Starting point

- Branch: `feat/AR-018-recent-transactions` from local `dev`
  (`chore/AR-019-budgets-repository` and AR-020 are still open PRs, so this
  branch stacks on top of the same working tree).
- `DashboardViewModel` only flipped `isLoading`; `DashboardScreen` had a static
  `RecentRow` example list and an `EmptyRecent` placeholder.

## Relevant code

- `feature/dashboard/.../DashboardViewModel.kt` — `combine(observeRecent,
  observeCategories, observeAccounts)`, `Transaction.toRecent`, label helpers.
- `feature/dashboard/.../DashboardScreen.kt` — recent card, `RecentRow`,
  `EmptyRecent`, `categoryIcon(iconKey)` mapping mirrored from
  `feature/transactions/.../TransactionVisuals.kt`.
- `core/data/.../TransactionRepositoryImpl.kt:239` — `observeRecent(limit)`.
- `core/data/.../TransactionRepositoryImpl.kt:257` — `observeAccounts` filters
  `archived`; accounts soft-deleted by sync are not in the map.
- `core/database/.../TransactionDao.kt:63` — recent query contract.
- `core/money` `CurrencyCode.entries` — fraction digits by ISO code.
- `app/.../navigation/AramApp.kt` — Home route now passes `onOpenLedger` and
  `onAddTransaction`.
- Design: `Designs/Aram/index.html:2497–2536` (Recent section, `.list-row`,
  meta `Category · date`, `.amount-in` for income), empty copy at 752–759.

## Conventions

- Money stays `Long` minor units; UI formats with `fractionDigits` per currency.
- Stateless screens; one Hilt ViewModel; `collectAsStateWithLifecycle`.
- No analytics, no amount/note logging, no `!!` in `src/main`.
- Tests: JUnit + coroutines-test for the ViewModel; Robolectric +
  `compose-ui-test` v2 for the card.

## Dependencies

- `feature:dashboard` adds `implementation(project(":core:domain"))` and
  `implementation(project(":core:money"))`; tests already had Robolectric.

## Decisions log

- Meta omits the category when the note already equals it (e.g. note
  "Groceries" in category Groceries → `Nov 16`); the prototype never has that
  duplicate case.
- Today's rows show `HH:mm`, older rows `MMM d`, matching the prototype's
  `Food · 09:12` / `Income · Oct 30`.
- Transfers append the account name so the two legs are distinguishable.
- Row tap and "See all" both open the Activity tab (there is no row detail
  screen yet).
- Transfer sign: both legs render as outflow, consistent with the ledger; see
  the spec's non-goals.

## Verification log

- `./gradlew :feature:dashboard:testDebugUnitTest -PbuildDate=20261128` — green
  (`DashboardViewModelTest` 6, `DashboardRecentTest` 4).
- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261128` — BUILD
  SUCCESSFUL (1m 1s), 574 tasks.
- Device, emulator-5554 (Pixel 9 Pro, Android 16, 1280×2856), app
  `com.aram.app.alpha.debug`:
  - Home Recent shows 5 rows in ledger order (Fuel 21 Nov … Cinema 11 Nov) with
    icons, `Category · date` meta and signed amounts — light
    (`home-light-scrolled.png`, `home-light-v2.png`) and dark
    (`home-dark.png`).
  - Row tap → Activity tab (`row-tap-activity.png`); "See all" → Activity
    (`seeall-result.png`).
  - Injected TRANSFER pair displayed `Transfer · 16:07 · Cash` and
    `Transfer · 16:07 · Card` (`home-transfer-dark-v2.png`), proving the
    account suffix; rows removed afterwards via sign-out/sign-in (sign-out
    wipes the local user).
  - Empty state with the prototype copy and `Add expense` → Add expense sheet
    (`home-empty-light.png`, `empty-cta-add.png`); ledger restored after
    (`home-final.png`).
  - logcat: no `FATAL EXCEPTION`; no amounts/notes/emails in app logs.
- Note: the AR-046 skeleton screenshot test rewrites
  `docs/specs/AR-046-dashboard-loading-skeleton/screenshots/dashboard-content-dark.png`
  during this change; left untouched and not committed.