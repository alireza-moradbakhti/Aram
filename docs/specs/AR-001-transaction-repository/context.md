# Context — AR-001 · Transaction repository contract and use cases

## Starting point

- Base: `dev` @ `400e1a5` (AR-038 merged).
- `:core:domain` contains only `AuthRepository`; unit tests run on plain JUnit.

## Relevant code

- `core/domain/src/main/java/com/aram/core/domain/repository/AuthRepository.kt` —
  the existing contract style: `Flow` for observation, `Outcome` for writes.
- `core/model/.../Transaction.kt` — `id`, account, optional category, type,
  positive `amountMinor`, currency, `occurredAt`, note, transfer/recurring ids.
- `core/model/.../Account.kt`, `Category.kt` — `archived`, `currencyCode`,
  `CategoryKind`.
- `core/model/.../Outcome.kt`, `AppError.kt` — no raw exceptions escape.
- `docs/API_CONTRACT.md` — transaction fields; amount always positive; category
  null for transfers; soft delete keeps a tombstone.

## Conventions

- `Outcome<T>` for fallible operations; failures carry `AppError`.
- Pure Kotlin in `:core:domain`; no Android imports.
- Money is `Long` minor units.
- Validate at the boundary; repositories trust validated input.

## Dependencies

- AR-002 implements `TransactionRepository` + `LedgerLookup` with Room.
- AR-004 consumes `CreateTransaction`; AR-005 consumes `ObserveTransactions`.

## Decisions log

- Validation lives in a pure validator invoked by use cases, so every rule is
  testable without Room and AR-004 can render typed `TransactionProblem`s.
- One problem per field; the map is keyed by `TransactionField`.
- `LedgerLookup` returns `Outcome<Account?>` so storage failures are distinct
  from not-found validation errors.
- Transfers are accepted at the contract level but must not carry a category;
  the atomic two-row write arrives with AR-009.

## Verification log

- `./gradlew :core:domain:testDebugUnitTest` — pass (14 validator cases,
  create/update/delete use-case tests).
- `grep -rn "^import android" core/domain/src` — no matches.
- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261117` — BUILD SUCCESSFUL
  (503 tasks).
- Device smoke: alpha installed and launched on the Galaxy A53 (no UI surface
  changed in this task).
