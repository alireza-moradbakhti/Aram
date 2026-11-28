# AR-010 · Search and filter transactions

- Issue: https://github.com/alireza-moradbakhti/Aram/issues/11
- Phase / Sprint: Phase 1 · Transactions core / Sprint 2
- Estimate: 5 pts · P1
- Branch: `feat/AR-010-search-filter` (from `dev` @ `58277b5`)
- Status: Done

## Problem

The Activity screen is locked to one month. Finding an old transaction means
paging month by month, and there is no way to narrow the ledger by note text,
type, category, or date. The prototype only leaves search and filter icon holes
(`index.html` Activity app bar) plus a "No matches" empty card — this task
fills them.

## Goals / non-goals

Goals

- Note search across all months, case-insensitive, debounced ~300 ms.
- Filters: type (existing All/Expenses/Income segmented control), one category,
  and a date range preset — all composing with each other and with search.
- Filter sheet in the app's bottom-sheet style, with applied-filter chips and a
  clear-all action.
- Distinct "no matches" empty state (`No matches for “…”` + Clear filters) that
  is not confused with the existing "nothing this month" state.
- Queries stay fully parameterized in Room; deleted rows never surface.

Non-goals

- Multi-category selection, amount ranges, or saved filters.
- A separate search screen or search history.
- Server-side search (reads stay cache-first from Room).
- Changing month navigation behavior when no search/filter is active.

## Acceptance criteria

- [x] Combined filters compose correctly.
- [x] Search is case-insensitive and does not scan deleted rows.
- [x] No raw SQL string interpolation is used.

## Technical design

Data flow: screen → `TransactionsViewModel` → `TransactionRepository.search`
(new) → `TransactionDao.search` (new, parameterized).

- `TransactionFilter` (`:core:domain`) carries `query`, `type`,
  `excludeType`, `categoryId`, optional `fromInclusive`/`toExclusive`.
  `excludeType` exists because the "Expenses" scope must include `TRANSFER`
  rows (they render as outflows) while excluding `INCOME`; a single equality
  parameter cannot express that.
- `TransactionDao.search` binds everything: `(:type IS NULL OR type = :type)`,
  `(:excludeType IS NULL OR type != :excludeType)`, category equality, half-open
  `occurredAt` bounds, `note LIKE '%' || :noteQuery || '%'` (SQLite LIKE is
  ASCII case-insensitive), `deletedAt IS NULL`, `ORDER BY occurredAt DESC`.
  No string concatenation of user input into SQL.
- ViewModel keeps the month pipeline unchanged when idle. `query` is trimmed,
  debounced (0 ms when blank, 300 ms otherwise) and `distinctUntilChanged`
  before joining `month`/`scope`/`filters` in a `combine`. Filtering switches
  the observation to `search(...)`; results still group by day. Month nav and
  stats are hidden in results mode (their numbers are month-scoped), replaced
  by a results header with the count and applied chips.
- `TransactionsUiState` gains `isFiltering`, `noMatches`, `filters`,
  `categories`. `isEmpty` keeps meaning "no data this month".
- Clear-all resets scope to All, category, date preset, and the search text.

UI (design tokens from `Designs/Aram`):

- App bar: title + search + filter icons; tapping search swaps the title for a
  glass `BasicTextField` with search glyph and a clear (×) action; filter icon
  opens the sheet.
- Filter sheet: `ModalBottomSheet` like the add sheet (30 dp top radius, drag
  handle), category chips in the add-sheet chip style, date presets as
  selectable rows, Reset (ghost) + Apply (primary).
- Applied chips: `AramChip` row (date preset, chosen category) plus a "Clear all"
  text button in the results header. The type is already visible in the scope
  segmented control directly above, so it is not duplicated as a chip; the
  filter icon carries a dot while a category/date filter is applied.
- No-matches card mirrors the prototype: search icon tile, bold
  `No matches for “query”`, hint line, glass "Clear filters" button.

## Data and migrations

No schema change; `AccountDao`/`TransactionDao` queries only. The search uses
the existing `(userId, occurredAt)` index; `LIKE '%…%'` is a scan within the
user's rows, acceptable at local-ledger sizes.

## Test plan

- DAO (Robolectric): case-insensitive note search, deleted rows excluded,
  type/excludeType, category, half-open date bounds, every filter combined,
  empty result set, other user excluded.
- Repository (JVM): filter mapping and session scoping (fake DAO).
- ViewModel (JVM): 300 ms debounce before the repository is queried, results
  mode on query/filter, scope composes into the filter, month mode untouched
  when idle, no-matches vs empty-month distinction, clear-all resets.
- Screen (Robolectric Compose): search opens the field, applied chips render,
  no-matches card action clears.
- Full `:app:assembleAlphaDebug test`, device run with the new states in light
  and dark.

## Risks and open questions

- Date presets (Any time / This month / Last 30 days / This year) are the
  MVP reading of "date range"; custom from/to pickers are a follow-up.
- `LIKE '%…%'` cannot use an index, fine for a single user's local ledger.
- Transfers are included in the Expenses scope by existing convention; search
  inherits it.