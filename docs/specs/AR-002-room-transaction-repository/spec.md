# AR-002 · Implement the Room-backed transaction repository

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/3
- Phase / Sprint: Phase 1 · Transactions core / Sprint 1
- Estimate: 3 pts · P0
- Branch: `feat/AR-002-room-transaction-repository` (stacked on AR-001)
- Status: Shipped

## Problem

AR-001 defines the contract; nothing persists transactions yet. `TransactionDao`
only exposes a recent query, and no implementation exists for
`TransactionRepository` or `LedgerLookup`.

## Goals

- Room-backed `TransactionRepository` in `:core:data` with entity/model mappers.
- DAO queries for month ranges, recent rows, by id, update, and soft delete.
- `createdAt`/`updatedAt`, `deletedAt IS NULL` filtering, `userId` scoping.
- `LedgerLookup` implemented from the account and category DAOs.
- Hilt bindings; repository tests against fake DAOs.

## Non-goals

- Cross-module transactions, outbox/sync (AR-042), or seed data.
- UI wiring (AR-004/AR-005).

## Acceptance criteria

- [x] Repository tests use a fake DAO and cover create, update, delete, and ranges.
- [x] Soft delete sets `deletedAt` and `updatedAt` instead of removing the row.
- [x] Feature modules do not import `:core:database`.

## Technical design

### Contract extension

`TransactionRepository` gains range and recent observation:

```kotlin
fun observeTransactions(
    fromInclusive: Long = Long.MIN_VALUE,
    toExclusive: Long = Long.MAX_VALUE,
): Flow<List<Transaction>>
fun observeRecent(limit: Int): Flow<List<Transaction>>
```

Observation scopes to the session user, filters tombstones, and orders by
`occurredAt DESC`. Read failures are logged and emit an empty list; the flow
type stays exception-free for ViewModels.

### Writes

- `create` builds the entity with a UUID, `createdAt = updatedAt = now`, trims
  and nulls an empty note, and upserts.
- `update` uses a DAO `UPDATE` limited to `userId` and non-deleted rows; zero
  affected rows → `AppError.NotFound`.
- `softDelete` sets `deletedAt = updatedAt = now`; zero affected rows →
  `AppError.NotFound`.
- No session → `AppError.Unauthorized`. DAO exceptions map to `AppError.Storage`.

### Session seam

`SessionStore` interface in `:core:datastore` (implemented by
`SessionDataStore`) so repository tests do not need a real DataStore.

### DAOs

- `TransactionDao`: `observeRange`, `observeById`, `findById`, `update`,
  `softDelete`; existing `observeRecent` reused.
- `AccountDao` / `CategoryDao`: `findById` scoped to the user, tombstones
  excluded.

## Data

No schema change. `TransactionEntity` already has the required columns.

## Test plan

- Fake DAOs and a fake `SessionStore`; `runTest`.
- Create (timestamps, user scope, note trim), update (updatedAt, NotFound),
  soft delete (tombstone timestamps, NotFound), range and recent observation,
  no-session behaviour.
- `./gradlew :app:assembleAlphaDebug test`.

## Risks and open questions

- Contract extension is made in this task because the range query is part of
  the issue scope; AR-005 consumes it for month grouping.
