# Context — AR-003 · Build a locale-aware money input component

## Starting point

- Base: `dev` @ `400e1a5`.
- `:core:money` has `Money`, `CurrencyCode` (with `fractionDigits`), and tests.

## Relevant code

- `core/money/.../Money.kt` — `ofMajor(BigDecimal)` uses `HALF_UP`; `majorUnits`.
- `core/money/.../CurrencyCode.kt` — USD/EUR/GBP/AED/TRY (2), IRR/JPY (0).
- `core/designsystem/.../component/Controls.kt` — button/field styling tokens.
- `core/designsystem/.../Theme.kt` — `aramColors` (border, textDim, textFaint,
  danger, surface1) and `AramTheme(darkTheme)`.
- Prototype: `.field` / `.amount-input` (workbench) and `.expense-amount` /
  `.amount-field` (add sheet, 34 sp/40 bold, bottom rule).

## Conventions

- Stateless components with explicit value/onValueChange.
- Money is `Long` minor units; no `Double`/`Float`.
- Custom components over Material text fields; copy from string resources at
  the call site, not inside the component.

## Dependencies

- AR-004 uses the large variant; AR-020 (budget limit) and AR-024 (goal target)
  can reuse it.

## Decisions log

- One parser result type per failure so the UI can pick copy without parsing
  message strings.
- `,` is treated as a decimal separator, never as a thousands separator; the
  design's "180,000" example is display text, not input.
- Component exposes `large` rather than a style enum; two variants are enough.

## Verification log

- `./gradlew :core:money:testDebugUnitTest :core:designsystem:compileDebugKotlin`
  — pass (10 parser cases).
- `./gradlew :app:assembleAlphaDebug test -PbuildDate=20261117` — BUILD SUCCESSFUL
  (515 tasks).
- Device: component is not yet mounted in a screen; AR-004 renders the large
  variant in the add sheet for the on-device pass.
