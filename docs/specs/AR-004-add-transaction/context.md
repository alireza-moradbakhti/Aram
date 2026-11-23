# Context — AR-004 · Add-transaction screen

## Starting point

- Base: `feat/AR-001-transaction-repository` at `caddd49`, with
  `feat/AR-002-room-transaction-repository` (`89645ac`) and
  `feat/AR-003-money-input` (`c585512`) merged in.
- AR-002 and AR-003 are open PRs; this branch stacks on both.

## Relevant code

- `core/domain/.../repository/TransactionRepository.kt` — observe + write contract.
- `core/domain/.../repository/LedgerLookup.kt` — per-id lookups used by validation.
- `core/domain/.../usecase/CreateTransaction.kt` — resolve → validate → persist.
- `core/domain/.../transaction/TransactionValidation.kt` — field flags and problems.
- `core/data/.../repository/TransactionRepositoryImpl.kt` — DAOs + session wiring.
- `core/database/.../dao/AccountDao.kt`, `CategoryDao.kt` — `observeByUser` exists.
- `core/money/.../AmountParser.kt`, `core/designsystem/.../AmountTextField.kt`.
- `feature/onboarding/.../WelcomeViewModel.kt` — ViewModel + `SubmitState` pattern.
- `feature/transactions/.../TransactionsScreen.kt` — demo screen whose sheet is replaced.
- `app/.../navigation/AramApp.kt` — tab scaffold where the route lands.
- Design: `Designs/Aram/index.html` add-expense sheet (`.expense-sheet`, amount
  field, `cat-chips`, `expense-meta`, note, primary button).

## Conventions

- Minor units (`Long`) + `CurrencyCode`; amount text parsed by `AmountParser`.
- Stateless screens with previews; ViewModel owns immutable `UiState`.
- No `!!` in `src/main`; no analytics; no logging of amounts or notes.
- Dark and light must both be verified.

## Dependencies

- AR-001 (contract + use case), AR-002 (Room repository), AR-003 (amount field).
- AR-005 replaces the demo list that currently receives navigation after save.

## Decisions log

- `LedgerDirectory` is a separate interface instead of growing `LedgerLookup`:
  use cases stay lookup-only and pickers opt in to list observation.
- Category is required for income/expense by the validator, so the form always
  selects the first matching category when the type flips.
- Success is signalled with a one-shot `saved` flag; the route pops and the app
  shell shows a snackbar, which disappears with the sheet.
- Amount errors are kept as a typed enum in state and turned into copy in the
  screen, matching the project's "strings near UI" convention.
- Save feedback reuses the prototype's `.toast` (dark pill, 1 dp
  `borderStrong`, 13 sp) via a `SnackbarHost` slot, so both themes match the
  design instead of Material's inverse surface.
- The FAB lives in the app scaffold for both Home and Activity, as in the
  prototype (Activity's FAB opens the same sheet).

## Verification log

- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261117` — pass
  (525 tasks). `feature:transactions` unit tests 9/9; money 11+7 pass.
- Device SM-A536E, Android 16 (dark): FAB on Home and Activity opens the sheet;
  empty save shows "Enter an amount." and stays; 18.50 expense + 950 income
  save, pop to Activity, toast "Transaction saved"; income toggle swaps chips;
  account picker expands with a check row; date picker sets "Yesterday";
  rotation with typed amount keeps the form; landscape scrolls to Save; light
  mode and light-mode toast match the tokens.
- Offline save: not completed on device — enabling airplane mode drops the
  wireless-debug connection. Write path is local-only (Room), covered by the
  repository tests.

