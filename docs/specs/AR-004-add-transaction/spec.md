# AR-004 · Add-transaction screen

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/5
- Phase / Sprint: Phase 1 · Transactions core / Sprint 1
- Estimate: 5 pts · P0
- Branch: feat/AR-004-add-transaction (stacked on AR-001 + AR-002 + AR-003)
- Status: Shipped

## Problem

The Activity screen's add sheet is demo-only: it parses digits into a fake
in-memory list and never touches the ledger. The app needs a real add flow that
validates with domain rules and persists through `CreateTransaction`.

## Goals / Non-goals

- Goals: real income/expense entry saved through the domain use case; options
  (accounts, categories) read live from Room; validation errors inline; form
  survives rotation; sheet layout matches the prototype.
- Non-goals: editing (AR-006), list rendering of saved rows (AR-005), transfers
  (AR-009), FAB/nav finalization (AR-008), account or category management.

## Acceptance criteria

- [x] Happy path saves and returns to the list.
- [x] Invalid amount, account, or category blocks saving with inline errors.
- [x] The screen previews without a ViewModel.

## Technical design

- `:core:domain` — new `LedgerDirectory` (observeAccounts/observeCategories)
  next to `LedgerLookup`; a narrow read contract for pickers so use cases keep
  their lookup-only dependency.
- `:core:data` — `TransactionRepositoryImpl` also implements `LedgerDirectory`
  (archived rows filtered out), bound in `DataModule`.
- `app` — `DomainModule` provides `CreateTransaction` (it has no injectable
  constructor).
- `:feature:transactions` — `AddTransactionViewModel` (`@HiltViewModel`,
  immutable `AddTransactionUiState`): collects both directory flows, keeps the
  selection valid when the type flips, parses the amount with `AmountParser`
  against the selected account's currency, and calls `CreateTransaction`.
  `TransactionWriteResult.Invalid` maps problems to field flags;
  `Failed` maps to `SubmitState.Failed`.
- `AddTransactionScreen` is stateless: state + callbacks only, account picker
  inline, Material date picker for the day, note limited to 80 chars.
- `AddTransactionRoute` hosts the state, renders the stateless screen inside a
  `ModalBottomSheet` (prototype presentation), and signals `onSaved` once.
- `AramApp` gains an `add-transaction` destination: bottom bar and FAB hide
  there, Home and Activity FABs navigate to it, saving pops back to Activity
  and shows a snackbar.
- `AramToast` (`:core:designsystem`) renders that snackbar as the prototype's
  dark pill toast in both themes; `AramApp` hosts it through `SnackbarHost`.

## Data and migrations

None. Accounts and categories already exist (seeded at profile creation);
transactions write through the AR-002 Room repository.

## Test plan

- Unit: `AddTransactionViewModelTest` with fake directory + fake ledger —
  defaults, type switch, amount errors (empty, zero, too many decimals),
  save success, invalid from domain, failure, config-change survival of state.
- Manual: dark/light, rotation with typed amount, save from Home and Activity,
  airplane mode (local write still succeeds).

## Risks and open questions

- The demo Activity list does not show saved rows until AR-005; the snackbar is
  the success signal for this task.
- `TransactionType.TRANSFER` is excluded from the toggle (AR-009).
