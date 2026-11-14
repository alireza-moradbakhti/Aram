# Roadmap & delivery plan

This is how Aram gets from a skeleton to a real product. Work is sliced into five two-week sprints; every task is a GitHub issue on the [project board](https://github.com/alireza-moradbakhti/Aram/projects) with acceptance criteria.

- **Cadence:** 2-week sprints, 7–8 issues each, roughly 20–25 focused hours per sprint.
- **Flow:** Todo → In Progress → In Review → Done.
- **Definition of done:** acceptance criteria met, tests added where logic changed, `./gradlew :app:assembleAlphaDebug test` green, CI green, docs updated if behavior changed.
- **Scope rule:** an issue that does not fit a sprint goes back to the backlog instead of stretching the sprint.

## Sprint 1 — Transactions foundation
**Nov 16 – Nov 29, 2026 · Milestone M1**

Outcome: record income and expenses end-to-end, from a validated form to a grouped list and correct account balances.

- [ ] Domain contract and use cases for transactions
- [ ] Room-backed repository (observe by month, update, soft delete)
- [ ] Locale-aware money input component
- [ ] Add-transaction screen (account, category, date, note)
- [ ] Transaction list with month headers, day groups, and empty state
- [ ] Edit/delete with undo
- [ ] Derived account balances + tests
- [ ] Navigation wiring for list, add, and edit routes

## Sprint 2 — Transfers, search, dashboard data
**Nov 30 – Dec 13, 2026 · Milestone M1 complete, M2 started**

Outcome: transactions behave like a real ledger, and the dashboard has a trustworthy safe-to-spend number.

- [ ] Atomic transfers between accounts
- [ ] Search and filter sheet (text, type, category, date range)
- [ ] Repository/ViewModel tests with Turbine and fakes
- [ ] Safe-to-spend calculator with a testable breakdown
- [ ] Dashboard shell: greeting, safe-to-spend card, month in/out
- [ ] Safe-to-spend explainer sheet
- [ ] Room instrumented tests for queries and soft deletes

## Sprint 3 — Charts and budgets
**Dec 14 – Dec 27, 2026 · Milestone M2 complete, M3 started**

Outcome: the dashboard tells the month’s story visually, and budgets track real spend.

- [ ] Category donut chart (custom Canvas)
- [ ] Six-month trend chart (custom Canvas)
- [ ] Recent transactions section on the dashboard
- [ ] Budgets repository and monthly progress query
- [ ] Budgets screen with month selector and progress
- [ ] Budget editor with 80% / 100% alert states
- [ ] Shared progress and amount components in the design system

## Sprint 4 — Goals and polish
**Dec 28, 2026 – Jan 10, 2027 · Milestone M3 complete**

Outcome: saving becomes a habit loop with goals, streaks, and projected completion.

- [ ] Goals, contributions, and progress in domain and data
- [ ] Goals list with progress arcs and streak badges
- [ ] Add-money bottom sheet and goal editor
- [ ] Streak and projected-completion calculator + tests
- [ ] Goal completion celebration
- [ ] Empty states and first-run guidance across tabs
- [ ] Settings: profile, currency, calendar, about

## Sprint 5 — Product polish and release
**Jan 11 – Jan 24, 2027 · Milestone M4**

Outcome: the app is pleasant to live with and ready to show publicly.

- [ ] Recurring rules and WorkManager scheduling
- [ ] CSV export through the Storage Access Framework
- [ ] Biometric app lock
- [ ] Safe-to-spend home-screen widget
- [ ] Release signing, versioning, and store assets
- [ ] Real screenshots, demo GIF, and case study in the README
- [ ] Room migration tests and an emulator job in CI

## After the MVP

- CSV import for bank statements
- Insight cards and subscription detection
- Remote accounts + sync (outbox, conflict policy — see [SPEC.md §8](SPEC.md))
- Shared budgets / households
- Multi-currency conversion with rate snapshots

## Working agreement

1. Pick the top unblocked issue from the current sprint.
2. Create a branch named after the type (`feat/...`, `fix/...`, `chore/...`).
3. Keep the PR focused; link the issue with `Closes #N`.
4. Merge only with a green CI check.
5. At sprint end: close what is done, move the rest back to the backlog, write a short retro (what worked, what slipped, one change for next sprint).
