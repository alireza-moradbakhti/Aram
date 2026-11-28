# Context — AR-012 · Safe-to-spend calculator

## Starting point

- Base: `dev` @ `58277b5`
- Branch: `feat/AR-012-safe-to-spend`
- `:core:domain` holds pure use cases; `:core:money` holds `Money` (Long minor
  units + `CurrencyCode`). `:core:domain` did not depend on `:core:money` yet.

## Relevant code

- `core/money/.../Money.kt` — `amountMinor`, `plus/minus`, `zero`, `ofMajor`.
- `core/money/.../CurrencyCode.kt` — fraction digits per currency (IRR/JPY 0).
- `core/domain/.../transaction/TransactionValidator.kt` — pattern for a pure
  validator with typed problem results.
- `core/domain/build.gradle.kts` — `api(project(":core:model"))`.

## Conventions

- Money is `Long` minor units end to end; never `Double`.
- Domain modules stay Android-framework-free; `java.time` is fine (JVM lib,
  minSdk 26).
- Plain JUnit tests, no Robolectric.

## Dependencies

- `:core:money` (new `api` dependency on `:core:domain`).
- Budgets/recurring inputs are provided by callers later (AR-019, AR-030).

## Decisions log

- 2026-11-28 — Breakdown stores signed contributions plus a `clampAdjustment`
  so `sum()` always equals the displayed total, even after clamping.
- 2026-11-28 — `daysRemaining` includes today; an ended month behaves as its
  last day (`1`).
- 2026-11-28 — Daily amount floors to whole minor units; the remainder is not
  redistributed (conservative by design).

## Verification log

- 2026-11-28 — `:core:domain:testDebugUnitTest` → BUILD SUCCESSFUL;
  `SafeToSpendCalculatorTest` 9 tests, 0 failures.
- 2026-11-28 — CI-equivalent
  `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261128 --console=plain`
  → BUILD SUCCESSFUL (574 tasks).
- 2026-11-28 — No UI/production wiring on this branch (calculator only), so no
  device step; the screen lands in AR-013.