# AR-003 · Build a locale-aware money input component

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/4
- Phase / Sprint: Phase 1 · Transactions core / Sprint 1
- Estimate: 2 pts · P1
- Branch: `feat/AR-003-money-input`
- Status: Shipped

## Problem

The add sheet will collect amounts as text. Without a shared parser every
screen would invent its own rules, risk floating-point money, and mishandle
zero-decimal currencies such as IRR and JPY.

## Goals

- `AmountParser` in `:core:money`: accepts `.` or `,` decimals, rejects
  invalid/zero/negative input and too many fraction digits, and never touches
  `Double` or `Float`.
- `AmountTextField` in `:core:designsystem`: stateless, with label, currency
  suffix, error/hint, a bordered field variant and the prototype's large
  add-sheet variant; light and dark previews.

## Non-goals

- Live grouping separators while typing (AR-004 owns presentation).
- Currency conversion or FX preview.

## Acceptance criteria

- [x] No `Double` or `Float` participates in parsing.
- [x] JPY/IRR-style currencies accept no decimals.
- [x] The component is stateless and previewable.

## Technical design

### Parser

```kotlin
sealed interface AmountParseResult { Valid(Money) | Empty | Zero | Invalid | TooManyFractionDigits(max) }
object AmountParser {
    fun parse(input: String, currency: CurrencyCode): AmountParseResult
    fun decimalSeparator(locale: Locale): Char
}
```

- Trims spaces, accepts a single `.` or `,` as the decimal separator, rejects
  mixed/extra separators and non-digits.
- Fraction digits are counted after stripping trailing zeros; USD allows 2,
  IRR/JPY allow 0.
- Conversion uses `Money.ofMajor` (BigDecimal, `HALF_UP`).
- Zero, negative, and malformed input map to `Zero`/`Invalid`.

### Component

`AmountTextField(value, onValueChange, currency, label, placeholder, error,
hint, enabled, large)` renders a `BasicTextField` with the currency ISO suffix.
Borders react to focus and error; the large variant matches the add sheet's
34 sp amount with a bottom rule.

## Test plan

- `AmountParserTest`: comma decimals, dot decimals, zero-decimal currencies,
  too many digits, trailing zeros, empty, zero, negative, malformed, locale
  separator.
- Compile-checked dark/light previews for both variants.
- `./gradlew :app:assembleAlphaDebug test`.

## Risks and open questions

- Locale grouping separators in stored text are rejected; callers should store
  raw digits.
