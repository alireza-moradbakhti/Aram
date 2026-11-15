# Working the Aram board (GitHub Scrum guide)

How to see the backlog, the active sprint, and story points — and how to run a
two-week sprint on GitHub Projects. Board:
<https://github.com/users/alireza-moradbakhti/projects/6>

## 1. The pieces

| Layer | What it holds |
| --- | --- |
| **Issue** | One task with acceptance criteria. Its number is permanent (`#2`); its key is `AR-001`. |
| **Label** | Taxonomy: `area:*`, `priority:*`, `phase:*`. |
| **Milestone** | The phase (Phase 1–4) with an exit criterion and due date. |
| **Project** | The board — the same issues, sliced by fields and views. |
| **Field** | `Status`, `Priority`, `Size`, `Story Points`, `Sprint` (iteration), `Assignees`. |

A sprint is **not** a milestone. Milestones are phases (weeks/months); the
`Sprint` iteration field is the two-week commitment. An issue belongs to one
phase and one sprint.

## 2. Task codes (AR-0NN)

- `AR-001` … `AR-036` map 1:1 to issues `#2` … `#37`, in delivery order.
  `AR-037` is `#38`; the backend-era tasks `AR-038` … `AR-044` are `#41` … `#47`.
- Codes never change and are never reused. New work starts at `AR-045`.
- Use the code everywhere: branch (`feat/AR-005-…`), commit (`… (AR-005)`),
  comments, and the issue title (`AR-005 · Transactions list …`).
- The full key → title → sprint table is in [MASTER_PLAN.md](MASTER_PLAN.md).

## 3. One-time setup: four views (5 minutes)

Open the board → **+ New view** for each:

| View | Layout | Filter | Group / sort |
| --- | --- | --- | --- |
| **Backlog** | Table | `is:open` | Group by `Sprint`; sort by `Priority` ↑, then `Story Points` ↑ |
| **Sprint board** | Board | `sprint:@current` | Columns = `Status` |
| **Roadmap** | Roadmap | *(none)* | Group by `Milestone` (dates come from milestone due dates) |
| **My work** | Table | `assignee:@me -status:Done` | Sort by `Priority` ↑ |

Notes:

- `@current` is resolved from today's date against the iteration calendar, so
  the sprint board switches itself every other Monday — no maintenance.
  `sprint:@next` and `sprint:@previous` also work.
- Multi-word values need quotes: `status:"In Progress"`.
- Slice the sprint board by `Priority` or `area:*` when it feels crowded.
- Right-click a view tab to rename, duplicate, or set it as default.

### Story points

`Story Points` is a **number** field (1, 2, 3, 5, 8). Two ways to read totals:

1. **In the UI:** group by `Sprint` (Backlog view) — the group header shows the
   point total. Group by `Milestone` for phase totals.
2. **From the CLI** (JSON exposes the field under the literal key `story Points`):

```bash
GH="C:/Program Files/GitHub CLI/gh.exe"
"$GH" project item-list 6 --owner alireza-moradbakhti --limit 100 --format json \
  --jq '[.items[] | select((.sprint.title // .sprint) == "Sprint 1") | ."story Points" // 0] | add'
```

### Capacity reference

| Sprint | Dates | Capacity | Phase |
| --- | --- | --- | --- |
| S1 | Nov 16 – Nov 29, 2026 | 31 | Phase 1 |
| S2 | Nov 30 – Dec 13, 2026 | 29 | Phase 1 |
| S3 | Dec 14 – Dec 27, 2026 | 21 | Phases 1–2 (holidays) |
| S4 | Dec 28 – Jan 10, 2027 | 23 | Phases 1–2 |
| S5 | Jan 11 – Jan 24, 2027 | 21 | Phase 3 |
| S6 | Jan 25 – Feb 7, 2027 | 22 | Phases 3–4 |

If the sum for the current sprint exceeds capacity, move the lowest-priority
item to the next iteration — do not stretch the sprint.

Sprint 6 exists because the app became online: network, auth, cache, and the
write outbox add ~26 net points. Biometric lock and the widget moved to the
post-MVP backlog instead of stretching every sprint.

### Online behavior in every device pass

Alpha runs against the mock API (`ARAM_API_MODE=mock`); beta/prod run against the
real backend. For any screen that reads or writes data, the device script must
include:

1. Airplane mode → cached data still renders, with an offline banner and
   last-synced time.
2. Airplane mode → add/edit a transaction → it is accepted, shows a pending
   state, and survives an app restart.
3. Reconnect → refresh (or wait for the worker) → the pending row syncs and the
   banner clears.
4. Backend unreachable or 5xx → typed error with retry; never a blank screen or
   endless spinner.

Real token values, emails, and amounts never appear in logs or screenshots.

### Optional: burn-up chart

**Insights → New chart** → *Burn up*: x-axis = time, y-axis = sum of
`Story Points`, filter `sprint:@current`. It answers "how many points are left
this sprint" without a spreadsheet.

## 4. The sprint loop

**Planning (Monday, ~30 min)**
1. Open **Backlog**, filter `sprint:@next` (or empty).
2. Fill the next iteration: top `P0` first, then `P1`, until points ≈ capacity.
   Sprint 1 starts with **AR-037** (safety baseline), then **AR-038** (network
   foundation) — pull them first, before any feature work.
3. Set `Sprint` on each card (drag into the iteration group).
4. Confirm every item has points, priority, and no blocked dependency
   (see the dependency column in MASTER_PLAN).
5. Outcome: a sprint forecast you believe, not a wish list.

**Daily (5 min)**
- Move cards honestly: Todo → In Progress when you start, Review when CI is
  green and the device script is handed over, Done only after your report.
- WIP limit: **two** items in progress. Finish before starting.
- Blocked? Comment on the issue with what is blocking and what you tried.

**Mid-sprint check**
- Compare remaining `Todo` points with days left. If the trend misses the
  capacity, cut scope now and say so — don't discover it on the last day.

**Sprint review (last day)**
- Demo the `alphaDebug` APK, not slides.
- Done means: acceptance criteria met, CI green, device pass reported, "Shipped"
  comment posted.

**Retro (10 min)**
- Three lines in the sprint's review comment or a Discussion: what worked,
  what slipped, one change for next sprint.

**Close**
- Unfinished work: remove the `Sprint` value so it returns to the backlog. Do
  not silently carry it into the next sprint — re-commit deliberately.

## 5. How `/ship` drives the board

```text
/ship start task AR-005
```

1. Reads the issue (key → number → body, labels, milestone).
2. Branches from `dev` as `feat/AR-005-<slug>` and sets **In Progress**.
3. Writes `docs/specs/AR-005-<slug>/spec.md` + `context.md`, waits for your y/n.
4. Implements, runs `./gradlew :app:assembleAlphaDebug test`, sets **Review**.
5. Hands you a numbered device-test script for the alpha APK.
6. After your report: rebuilds, posts the **Shipped** comment (branch, commit,
   points, verification, follow-ups), commits, pushes, sets **Done**.
7. Opens a PR into `dev` with a proper title and body, waits for CI, squash
   merges, deletes the feature branch, and syncs local `dev`.

Never set Done manually before the device pass; the comment is the audit trail
used for sprint stats and the portfolio case study.

## 6. Pull requests, merging, and branch cleanup

**Every change reaches `dev` through a PR.** Feature branches are short-lived;
`dev` and `main` are permanent.

| Rule | Detail |
| --- | --- |
| Base branch | `dev` for features, fixes, chores, docs. `main` only receives `dev`. |
| Title | `type(area): summary (AR-0NN)` — e.g. `feat(transactions): group list by month (AR-005)` |
| Body | Fill `.github/PULL_REQUEST_TEMPLATE.md`: summary, task key + points, verification (CI + device), checklist |
| Merge | Squash merge, only with green CI and a device pass |
| Cleanup | The head branch is deleted after merge. `dev` and `main` are **never** deleted. |
| Release | `dev` → `main` is a release action, not a feature PR; never auto-merged |

Automation in the repo:

- `.github/workflows/cleanup-branches.yml` deletes a head branch when a PR is
  merged **into `dev`**, and only if the head is neither `dev` nor `main` and
  lives in this repository (fork branches are left alone).
- Repository auto-delete is deliberately **off**: enabling it would delete
  `dev` when the `dev` → `main` release PR merges.
- Dependabot PRs also target `dev` (`target-branch: "dev"`).

After merging from the CLI:

```bash
git checkout dev && git pull --ff-only origin dev
```

If a branch is not merged, leave it alone — cleanup is for merged work only.

## 7. CLI cheat sheet

```bash
GH="C:/Program Files/GitHub CLI/gh.exe"   # or just gh if on PATH

# my open work
"$GH" issue list --repo alireza-moradbakhti/Aram --assignee @me --state open

# find an issue by key
"$GH" issue list --repo alireza-moradbakhti/Aram --search "AR-005 in:title" \
  --json number,title,url

# everything on the board with fields
"$GH" project item-list 6 --owner alireza-moradbakhti --limit 100 --format json

# move a card
"$GH" project item-edit 6 --owner alireza-moradbakhti --url <issue-url> \
  --field Status --value "In Progress"

# leave a trace
"$GH" issue comment <number> --repo alireza-moradbakhti/Aram --body-file <file>
```

Gotchas:

- User-owned Projects v2 need a **classic** PAT (`repo` + `project`); fine-grained
  tokens fail with *Resource not accessible by personal access token*.
- `item-edit` with a field **name** requires `--url`; with `--id` it requires
  `--field-id`. One field per call.
- Bulk edits hit a secondary rate limit (~1 mutation/minute) long before the
  hourly quota; wait and retry instead of hammering.
- In `item-list --format json`, story points are under `"story Points"` (with the
  space), not `storyPoints`.

## 8. Labels

| Label | Meaning |
| --- | --- |
| `area:app` `area:ui` `area:domain` `area:data` `area:database` `area:transactions` `area:dashboard` `area:budgets` `area:goals` `area:infra` `area:docs` | Where the change lands |
| `priority:p0` `priority:p1` `priority:p2` | p0 = blocks the phase, p1 = planned, p2 = nice to have |
| `phase:1` … `phase:4` | Phase of the roadmap |

## 9. Rules that keep the board honest

1. Every branch maps to exactly one `AR-0NN`.
2. Every commit references the key; every PR says `Closes #<issue>`.
3. If an issue changes scope, edit the issue first, then the code.
4. No new work without an issue and a code.
5. The board is the source of truth — if it is not on the board, it is not planned.
