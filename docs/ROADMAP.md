# Roadmap & delivery plan

This is how Aram gets from a skeleton to a real product. Work is sliced into five two-week sprints; every task is a GitHub issue on the [project board](https://github.com/alireza-moradbakhti/Aram/projects) with acceptance criteria.

- **Cadence:** 2-week sprints, 7–8 issues each, roughly 20–25 focused hours per sprint.
- **Flow:** Todo → In Progress → Review → Done.
- **Definition of done:** acceptance criteria met, tests added where logic changed, `./gradlew :app:assembleAlphaDebug test` green, CI green, real-device pass, docs updated if behavior changed.
- **Scope rule:** an issue that does not fit a sprint goes back to the backlog instead of stretching the sprint.
- **Task keys:** every issue carries a stable `AR-0NN` key; the full key list, dependencies, and the post-MVP KMP plan live in [MASTER_PLAN.md](MASTER_PLAN.md). How to run the board: [GITHUB_SCRUM.md](GITHUB_SCRUM.md).

## Sprint 1 — Transactions foundation
**Nov 16 – Nov 29, 2026 · Milestone M1**

Outcome: nothing crashes or hangs on a failed load, and income/expenses are recorded end-to-end — from a validated form to a grouped list and correct account balances.

- [ ] AR-037 Safe error handling across screens and layers (first task)
- [ ] AR-001 Transaction domain contract and use cases
- [ ] AR-002 Room-backed transaction repository
- [ ] AR-003 Locale-aware money input component
- [ ] AR-004 Add-transaction screen
- [ ] AR-005 Transaction list with month headers and day groups
- [ ] AR-006 Edit/delete with undo
- [ ] AR-007 Derived account balances + tests
- [ ] AR-008 Navigation wiring for list, add, and edit routes

## Sprint 2 — Transfers, search, dashboard data
**Nov 30 – Dec 13, 2026 · Milestone M1 complete, M2 started**

Outcome: transactions behave like a real ledger, and the dashboard has a trustworthy safe-to-spend number.

- [ ] AR-009 Atomic transfers between accounts
- [ ] AR-010 Search and filter sheet (text, type, category, date range)
- [ ] AR-011 Repository/ViewModel tests with Turbine and fakes
- [ ] AR-012 Safe-to-spend calculator with a testable breakdown
- [ ] AR-013 Dashboard shell: greeting, safe-to-spend card, month in/out
- [ ] AR-014 Safe-to-spend explainer sheet
- [ ] AR-015 Room instrumented tests for queries and soft deletes

## Sprint 3 — Charts and budgets
**Dec 14 – Dec 27, 2026 · Milestone M2 complete, M3 started**

Outcome: the dashboard tells the month’s story visually, and budgets track real spend.

- [ ] AR-016 Category donut chart (custom Canvas)
- [ ] AR-017 Six-month trend chart (custom Canvas)
- [ ] AR-018 Recent transactions section on the dashboard
- [ ] AR-019 Budgets repository and monthly progress query
- [ ] AR-020 Budgets screen with month selector and progress
- [ ] AR-021 Budget editor with 80% / 100% alert states
- [ ] AR-022 Shared progress and amount components in the design system

## Sprint 4 — Goals and polish
**Dec 28, 2026 – Jan 10, 2027 · Milestone M3 complete**

Outcome: saving becomes a habit loop with goals, streaks, and projected completion.

- [ ] AR-023 Goals, contributions, and progress in domain and data
- [ ] AR-024 Goals list with progress arcs and streak badges
- [ ] AR-025 Add-money bottom sheet and goal editor
- [ ] AR-026 Streak and projected-completion calculator + tests
- [ ] AR-027 Goal completion celebration
- [ ] AR-028 Empty states and first-run guidance across tabs
- [ ] AR-029 Settings: profile, currency, calendar, about

## Sprint 5 — Product polish and release
**Jan 11 – Jan 24, 2027 · Milestone M4**

Outcome: the app is pleasant to live with and ready to show publicly.

- [ ] AR-030 Recurring rules and WorkManager scheduling
- [ ] AR-031 CSV export through the Storage Access Framework
- [ ] AR-032 Biometric app lock
- [ ] AR-033 Safe-to-spend home-screen widget
- [ ] AR-034 Release signing, versioning, and store assets
- [ ] AR-035 Real screenshots, demo GIF, and case study in the README
- [ ] AR-036 Room migration tests and an emulator job in CI

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
