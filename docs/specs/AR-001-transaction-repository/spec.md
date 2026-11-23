# AR-001 · Add transaction repository contract and use cases

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/2
- Phase / Sprint: Phase 1 · Transactions core / Sprint 1
- Estimate: 3 pts · P0
- Branch: `feat/AR-001-transaction-repository`
- Status: Shipped

## Problem

Presentation needs to create, edit, observe, and soft-delete transactions without
touching Room. Today only `AuthRepository` exists in `:core:domain`, and the
validation rules that keep the ledger consistent (positive amounts, account and
category ownership, currency match, category kind vs type) live nowhere.

## Goals

- `TransactionRepository` contract in `:core:domain` for observe, create,
  update, and soft-delete.
- A pure `TransactionValidator` with typed problems per field, plus use cases
  that resolve the account and category, validate, and only then write.
- Unit tests for every validation rule and for the use-case orchestration.

## Non-goals

- Room implementation (AR-002), mock API (AR-039), transfers (AR-009).
- Changing the `Transaction` model or the database schema.
- Any Android dependency in `:core:domain`.

## Acceptance criteria

- [x] `:core:domain` has no Android imports.
- [x] Income, expense, zero, negative, and currency mismatch cases are tested.
- [x] No Room or DataStore detail leaks into the contract.

## Technical design

### Contracts

```kotlin
interface TransactionRepository {
    fun observeTransactions(): Flow<List<Transaction>>
    fun observeTransaction(id: String): Flow<Transaction?>
    suspend fun create(draft: TransactionDraft): Outcome<Transaction>
    suspend fun update(id: String, draft: TransactionDraft): Outcome<Transaction>
    suspend fun softDelete(id: String): Outcome<Unit>
}

interface LedgerLookup {
    suspend fun account(id: String): Outcome<Account?>
    suspend fun category(id: String): Outcome<Category?>
}
```

`LedgerLookup` lets the use cases resolve ownership without depending on a
concrete account/category repository. The Room implementation (AR-002) provides
both interfaces.

### Validation

`TransactionValidator.validate(draft, account, category)` returns
`TransactionValidation.Valid` or `Invalid(Map<TransactionField, TransactionProblem>)`
with two problems max per field not possible — one problem per field:

- `AmountNotPositive` — `amountMinor <= 0`.
- `AccountNotFound` / `AccountArchived`.
- `CategoryRequired` — income and expense need a category.
- `CategoryNotFound` / `CategoryArchived`.
- `CategoryKindMismatch` — income draft needs an income category and vice versa.
- `CategoryNotAllowed` — transfers must not carry a category.
- `CurrencyMismatch` — draft currency differs from the account currency.

### Use cases

`CreateTransaction`, `UpdateTransaction`, `DeleteTransaction`, and
`ObserveTransactions`. Create/update resolve the account and category through
`LedgerLookup`, return `TransactionWriteResult.Invalid` without writing when a
rule fails, and otherwise delegate to the repository. Lookup storage failures
surface as `TransactionWriteResult.Failed(AppError)`.

## Data

No schema change. `TransactionDraft` mirrors the server fields:
account, optional category, type, positive amount, currency, timestamp, note.

## Test plan

- `TransactionValidatorTest` — valid income, valid expense, zero, negative,
  missing/archived account, missing/archived category, kind mismatch, currency
  mismatch, category required, transfer with category.
- `CreateTransactionTest` — valid write, invalid short-circuit, lookup failure.
- `UpdateTransactionTest` — invalid update leaves the repository untouched.
- `DeleteTransactionTest` — delegates to `softDelete`.

## Risks and open questions

- Transfers need two rows; AR-009 adds the atomic batch use case. The validator
  only rejects a category on transfer rows today.
