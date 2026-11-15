# Roadmap & delivery plan

This is how Aram gets from a skeleton to a real product. Work is sliced into six
two-week sprints; every task is a GitHub issue on the
[project board](https://github.com/alireza-moradbakhti/Aram/projects) with
acceptance criteria.

- **Cadence:** 2-week sprints, 6–8 issues each, roughly 20–25 focused hours per sprint.
- **Flow:** Todo → In Progress → Review → Done.
- **Definition of done:** acceptance criteria met, tests added where logic changed, `./gradlew :app:assembleAlphaDebug test` green, CI green, real-device pass (including airplane mode for cached reads and queued writes), docs updated if behavior changed.
- **Scope rule:** an issue that does not fit a sprint goes back to the backlog instead of stretching the sprint.
- **Online rule:** alpha runs against the mock API; beta/prod run against the real backend described in [API_CONTRACT.md](API_CONTRACT.md).
- **Task keys:** every issue carries a stable `AR-0NN` key; the full key list, dependencies, and the post-MVP KMP plan live in [MASTER_PLAN.md](MASTER_PLAN.md). How to run the board: [GITHUB_SCRUM.md](GITHUB_SCRUM.md).

## Sprint 1 — Safety and the online foundation
**Nov 16 – Nov 29, 2026 · Milestone M1**

Outcome: nothing crashes or hangs, alpha can talk to a mock backend with the real
API shape, and the first transactions land in a cache the UI can observe.

- [ ] AR-037 Safe error handling across screens and layers (first task)
- [ ] AR-038 Network client, API config, and error mapping
- [ ] AR-039 Mock API and auth fixtures for alpha
- [ ] AR-001 Transaction domain contract and use cases
- [ ] AR-002 Room-backed transaction repository (cache layer)
- [ ] AR-003 Locale-aware money input component
- [ ] AR-004 Add-transaction screen
- [ ] AR-005 Transaction list with month headers and day groups

## Sprint 2 — Online transactions end-to-end
**Nov 30 – Dec 13, 2026 · Milestone M1**

Outcome: sign in with the mock account, record income/expense/transfer, see
balances, and read everything from a cache that refreshes from the API.

- [ ] AR-006 Edit/delete with undo
- [ ] AR-007 Derived account balances + tests
- [ ] AR-008 Navigation wiring for list, add, and edit routes
- [ ] AR-009 Atomic transfers between accounts
- [ ] AR-010 Search and filter sheet (text, type, category, date range)
- [ ] AR-040 Online auth flow and secure token session
- [ ] AR-041 Cache-first repositories with pull-to-refresh
- [ ] AR-011 Repository/ViewModel tests with Turbine and fakes

## Sprint 3 — Dashboard data and offline writes
**Dec 14 – Dec 27, 2026 · Milestone M2 started**

Outcome: a trustworthy safe-to-spend number, and writes survive a dead network.

- [ ] AR-012 Safe-to-spend calculator with a testable breakdown
- [ ] AR-013 Dashboard shell: greeting, safe-to-spend card, month in/out
- [ ] AR-042 Offline write outbox and sync worker
- [ ] AR-043 Pull sync, conflicts, and tombstones

## Sprint 4 — Dashboard polish and budgets
**Dec 28, 2026 – Jan 10, 2027 · Milestone M2 complete, M3 started**

Outcome: the dashboard tells the month's story visually, sync is proven by tests,
and budgets start tracking real spend.

- [ ] AR-014 Safe-to-spend explainer sheet
- [ ] AR-015 Room instrumented tests for queries and soft deletes
- [ ] AR-016 Category donut chart (custom Canvas)
- [ ] AR-017 Six-month trend chart (custom Canvas)
- [ ] AR-018 Recent transactions section on the dashboard
- [ ] AR-019 Budgets repository and monthly progress query
- [ ] AR-020 Budgets screen with month selector and progress
- [ ] AR-044 Network, cache, and sync test suite

## Sprint 5 — Budgets and goals
**Jan 11 – Jan 24, 2027 · Milestone M3**

Outcome: budgets alert before overspending, and saving becomes a habit loop with
goals, streaks, and projected completion.

- [ ] AR-021 Budget editor with 80% / 100% alert states
- [ ] AR-022 Shared progress and amount components in the design system
- [ ] AR-023 Goals, contributions, and progress in domain and data
- [ ] AR-024 Goals list with progress arcs and streak badges
- [ ] AR-025 Add-money bottom sheet and goal editor
- [ ] AR-026 Streak and projected-completion calculator + tests
- [ ] AR-027 Goal completion celebration

## Sprint 6 — Polish and release
**Jan 25 – Feb 7, 2027 · Milestone M4**

Outcome: the app is pleasant to live with, connected to the real backend, and
ready to show publicly.

- [ ] AR-028 Empty states and first-run guidance across tabs
- [ ] AR-029 Settings: profile, currency, calendar, sign out, sync status
- [ ] AR-030 Recurring rules and WorkManager scheduling
- [ ] AR-031 CSV export through the Storage Access Framework
- [ ] AR-034 Release signing, versioning, and store assets
- [ ] AR-035 Real screenshots, demo GIF, and case study in the README
- [ ] AR-036 Room migration tests and an emulator job in CI

## After the MVP

- Biometric app lock
- Home-screen safe-to-spend widget
- CSV import for bank statements
- Insight cards and subscription detection
- Realtime sync (WebSocket/SSE) and multi-device presence
- Shared budgets / households
- Multi-currency conversion with rate snapshots

## Working agreement

1. Pick the top unblocked issue from the current sprint.
2. Create a branch named after the type (`feat/...`, `fix/...`, `chore/...`).
3. Keep the PR focused; link the issue with `Closes #N`.
4. Merge only with a green CI check and the device pass reported.
5. At sprint end: close what is done, move the rest back to the backlog, write a short retro (what worked, what slipped, one change for next sprint).
