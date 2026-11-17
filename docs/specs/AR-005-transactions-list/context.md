# Context — AR-005 · Transactions list with month headers and day groups

## Starting point

- Base: `feat/AR-004-add-transaction` at `ad40e2d` (stacked on AR-001/002/003).
- AR-004 added the real add sheet and the Activity FAB; this branch replaces
  the demo list it saves into.

## Relevant code

- `core/domain/.../repository/TransactionRepository.kt` — `observeTransactions(from, to)`.
- `core/domain/.../repository/LedgerDirectory.kt` — accounts and categories flows.
- `core/data/.../repository/TransactionRepositoryImpl.kt` — user-scoped queries, archived filtered.
- `core/model/.../Transaction.kt`, `Account.kt`, `Category.kt` — record shapes.
- `core/money/.../CurrencyCode.kt` — fraction digits per currency.
- `core/designsystem/.../text/MoneyFormat.kt` — integer formatting helpers.
- `core/designsystem/.../component/Controls.kt` — `MoneyLabel`, `AramMonthNav`, `AramSegmented`, `AramPrimaryButton`.
- `feature/transactions/.../TransactionsScreen.kt` — demo screen to replace.
- `feature/transactions/.../AddTransactionScreen.kt` — `categoryIcon` mapping to share.
- Design: `Designs/Aram/index.html` Activity screen (month nav, scope pills,
  stat cards, day groups, glass row cards, FAB).

## Conventions

- Minor-unit `Long` amounts; currency-aware formatting at the UI edge.
- Stateless screen + previews; ViewModel owns immutable state (`UiState`).
- Coroutines on `viewModelScope`; repository flows never throw.
- No `!!`, no analytics, no amount/note logging.

## Dependencies

- AR-002 (range observation), AR-004 (FAB + save flow).
- AR-006 (edit row) and AR-009 (transfer pairs) build on this list.

## Decisions log

- Totals are single-currency (dominant currency among visible rows); FX
  conversion is out of scope for the MVP.
- Day headers show the day's outflow as a negative signed total, or the income
  when the day has no outflow, matching the prototype.
- The spent delta compares against the previous month; income shows the entry
  count because "On time · 1st" needs the salary insight engine.
- `LazyColumn` with a sticky month header; each day group is one item with
  per-row keys so 500 rows recycle.
- No error branch: the repository contract maps failures to an empty list.
- `formatMinor`/`formatSignedMinor` live in `:core:designsystem`; `MoneyLabel`
  gained `fractionDigits` and appends the sign, so IRR renders integers.

## Verification log

- `./gradlew :feature:transactions:testDebugUnitTest` — 17/17 pass
  (9 `TransactionsViewModelTest`, 8 `AddTransactionViewModelTest`).
- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261117` — pass
  (525 tasks).
- On-device pass: blocked. The phone's wireless debugging went offline after
  the AR-004 airplane-mode test (`adb devices` shows both known addresses
  `offline`, reconnect fails). Re-run when the device is reachable:
  - seeded month renders newest-first day groups with signed totals;
  - empty month shows the CTA and it opens the add sheet;
  - save pops back and the new row appears without a relaunch;
  - month arrows, scope segments, rotation, dark/light;
  - a 500-row month scrolls without jank.
