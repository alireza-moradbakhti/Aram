# Aram API contract

The app is online-first: the server is the source of truth, Room is the offline
cache, and writes are queued in an outbox when the network is unavailable.
Alpha builds run against an in-memory mock that implements this exact contract
(and can be pointed at the stage host); beta runs against stage; production runs
against the production API. This document is the handoff between the Android app
and `Aram-Api` (.NET 10 / FastEndpoints).

- **Base URL:** `{host}/api` — the host is chosen per build (see
  [Environments](#environments)); every path below is relative to `/api`.
- **Transport:** HTTPS only, JSON UTF-8, `camelCase` fields.
- **IDs:** UUID strings. Transactions may carry a client-generated id (sync);
  everything else is server-generated.
- **Dates:** ISO-8601 UTC strings (`"2026-11-21T12:00:00Z"`). Months are UTC and
  selected with `year` + `month` query parameters.
- **Money:** decimal **IRR** (`amountIrr: 10420000`). Every money field is a JSON
  number and a Kotlin `Double` at the network boundary, converted to
  `core:money` on the way in. Rows entered in another currency also keep
  `enteredCurrency`, `enteredAmount` and `fxRate`.
- **Ownership:** the user is derived from the bearer token. The server rejects
  missing/stale tokens with `401`.

## Environments

Two deployments run the same contract:

| Environment | Host | Used by |
| --- | --- | --- |
| Stage | `https://test-myendpoint` *(placeholder until the backend publishes DNS)* | `alpha` with `remote` mode, `beta`, `prodDebug` |
| Production | `https://myendpoint` *(placeholder)* | `prodRelease` only |

Example: `https://test-myendpoint/api/auth/login`.

- Hosts are build configuration (`aram.api.stageBaseUrl` /
  `aram.api.prodBaseUrl` in `gradle.properties`), never hardcoded in Kotlin.
- The client exposes `ApiEnvironment` (`STAGE`, `PRODUCTION`) plus an
  `EndpointProvider`; API mode (`mock` / `remote`) is a separate switch, so
  alpha can run against fixtures while every remote build still knows its host.
- The production host is reachable **only** from `prodRelease`; debug builds
  never write to production data.
- Stage data is disposable and may be reset at any time.
- A `prod` release build fails configuration if it would use the mock engine or
  a non-production host. Debug builds may override the host with
  `ARAM_API_BASE_URL`; cleartext is allowed for local servers in debug only.

## Response envelope

Every JSON endpoint except `GET /health` wraps its payload in `Result<T>`:

```json
{ "isSuccess": true, "message": "", "errors": [], "data": { } }
```

- `isSuccess: false` with `200` is still a failure; the client maps it to a
  server error carrying `message`.
- `data` is the payload described per endpoint below.
- Deletes return `Result<bool>` with `true`; they do not return `204`.

### Errors

| Status | Body | Client mapping |
| --- | --- | --- |
| `400` | FastEndpoints validation: `{statusCode, message, errors: {field: [msg]}}` | `Validation(fields, message)` |
| `401` | `Result<null>` — missing/expired token or bad credentials | `Unauthorized` |
| `403` | `Result<null>` | `Forbidden` |
| `404` | `Result<null>` | `NotFound` |
| `409` | `Result<null>` with a message (e.g. duplicate budget) | `Conflict(message)` |
| `429` | optional `Retry-After` header | `RateLimited(retryAfterSeconds)` |
| `5xx` | `Result<null>` | `Server(requestId, message)` |

Requests carry `X-Request-Id` (client-generated), `X-Platform` and
`X-App-Version`. Logs never contain amounts, notes, emails or tokens.

## Authentication

Email + password, no third-party login. The token is a 24-hour JWT with **no
refresh and no logout endpoint**; a `401` on an authenticated request ends the
session locally.

| Method | Path | Body | Returns |
| --- | --- | --- | --- |
| POST | `/auth/register` | `email`, `password`, `displayName`, `homeCurrency?` | `AuthResponse` (`201`) |
| POST | `/auth/login` | `email`, `password` | `AuthResponse` |
| POST | `/auth/forgot-password` | `email` | `{message, developmentToken?}` |
| POST | `/auth/reset-password` | `email`, `token`, `newPassword` | `bool` |
| POST | `/auth/change-password` | `currentPassword`, `newPassword` | `bool` |
| GET | `/auth/me` | — | `Profile` |
| PUT | `/auth/profile` | `displayName?`, `homeCurrency?`, `secondaryDisplay?`, `language?`, `appearance?`, `quietHours?` | `Profile` |
| DELETE | `/auth/account` | `confirm: "DELETE"`, `password?` | `bool` |

```json
{
  "token": "eyJ…",
  "expiresAtUtc": "2026-11-22T10:15:30Z",
  "profile": {
    "id": "…", "displayName": "Ava Bennett", "firstName": "Ava",
    "email": "demo@aram.app", "avatarInitials": "AB", "homeCurrency": "IRR",
    "secondaryDisplay": false, "language": "en", "appearance": "system",
    "quietHours": "", "lastSyncedAt": null,
    "stats": { "transactions": 36, "budgets": 3, "goals": 2 }
  }
}
```

Passwords: at least 8 characters with a lowercase letter and a digit.

## Profile and accounts

| Method | Path | Returns |
| --- | --- | --- |
| GET | `/profile/stats` | `{transactions, budgets, goals}` |
| GET | `/profile/accounts` | `Account[]` `{id, name, institution, kind, isDefault}` |
| GET | `/profile/export` | `text/csv` (not enveloped) |

## Categories

| Method | Path | Query | Returns |
| --- | --- | --- | --- |
| GET | `/categories` | `kind?` (`expense`/`income`) | `Category[]` |

`Category` = `{id, slug, name, iconKey, tone, kind}`. Ten seeded slugs:
`groceries`, `dining`, `transport`, `subscriptions`, `bills`, `shopping`, `fun`,
`housing`, `other`, `salary`. Transactions reference the **slug**; budgets
reference the **id**.

## Transactions

| Method | Path | Body / Query | Returns |
| --- | --- | --- | --- |
| GET | `/transactions` | `year?`, `month?`, `kind?`, `page?`, `pageSize?` (max 100), `search?` | `TransactionList` |
| POST | `/transactions` | `SaveTransaction` | `Transaction` (`201`) |
| PUT | `/transactions/{id}` | `SaveTransaction` | `Transaction` |
| DELETE | `/transactions/{id}` | — | `bool` |
| GET | `/transactions/summary` | `year?`, `month?` | `TransactionSummary` |
| GET | `/transactions/insights/weekly` | — | `WeeklyInsight` |

`Transaction`:

```json
{
  "id": "…", "kind": "expense", "amountIrr": 10420000,
  "category": { "id": "…", "slug": "groceries", "name": "Groceries", "iconKey": "cart", "tone": "ok", "kind": "expense" },
  "account": { "id": "…", "name": "Everyday", "institution": "Bank", "kind": "bank" },
  "note": "Weekly shop", "occurredAt": "2026-11-20T08:05:00Z",
  "enteredCurrency": "USD", "enteredAmount": 10.0, "fxRate": 1042000.0
}
```

`SaveTransaction` = `{kind, amount, currency?, categorySlug?, accountId?, note?,
occurredAt?, clientUpdatedAt?}`. `amount` is expressed in `currency` (the
profile home currency when omitted) and converted to IRR server-side. Omitting
`categorySlug` uses `other` for expenses and `salary` for income.

`TransactionList` groups a UTC month newest-day-first:

```json
{
  "year": 2026, "month": 11, "page": 1, "pageSize": 50,
  "totalCount": 12, "totalPages": 1,
  "totalSpentIrr": 28420000, "totalIncomeIrr": 45000000, "spentDeltaPercent": -4.5,
  "groups": [{ "date": "2026-11-20", "label": "Yesterday", "totalIrr": 10420000, "items": [] }]
}
```

`TransactionSummary` = `{year, month, spentIrr, incomeIrr, netIrr,
spentDeltaPercent?, transactionCount, daysInMonth, daysLeft, dailyAllowanceIrr}`.

`WeeklyInsight` = `{weekStart, spentIrr, deltaPercent?, days: [{date, label,
amountIrr, isToday}]}`.

## Budgets

| Method | Path | Body / Query | Returns |
| --- | --- | --- | --- |
| GET | `/budgets` | — | `Budget[]` `{id, categoryId, categorySlug, categoryName, iconKey, monthlyLimitIrr, rollover}` |
| GET | `/budgets/overview` | `year?`, `month?` | `BudgetOverview` |
| GET | `/budgets/categories` | — | expense `Category[]` |
| POST | `/budgets` | `{categoryId? / categorySlug?, monthlyLimitIrr, rollover?}` | `Budget` (`201`; `409` if duplicate) |
| PUT | `/budgets/{id}` | `{monthlyLimitIrr, rollover?}` | `Budget` |
| DELETE | `/budgets/{id}` | — | `bool` |

`BudgetOverview` = `{year, month, totalLimitIrr, totalSpentIrr,
totalRemainingIrr, percentUsed, status, statusLabel, daysLeft, perDayLeftIrr,
resetsOn, categories: BudgetItem[]}` where each item adds
`{spentIrr, remainingIrr, percentUsed, status, statusLabel}`.

## Goals

| Method | Path | Body | Returns |
| --- | --- | --- | --- |
| GET | `/goals` | `includeArchived=true?` | `Goal[]` |
| POST | `/goals` | `{name, targetIrr, savedIrr?, targetDate?, iconKey?}` | `Goal` (`201`) |
| PUT | `/goals/{id}` | `{name?, targetIrr?, targetDate?, iconKey?, isArchived?}` | `Goal` |
| DELETE | `/goals/{id}` | — | `bool` |
| POST | `/goals/{id}/contributions` | `{amount, currency?, occurredAt?}` | `Goal` |

`Goal` = `{id, name, targetIrr, savedIrr, remainingIrr, percent, targetDate?,
iconKey, isReached, isArchived}`.

## Notifications

| Method | Path | Query | Returns |
| --- | --- | --- | --- |
| GET | `/notifications` | `scope?` | `{scope, unreadCount, groups: [{label, items}]}` |
| POST | `/notifications/{id}/read` | — | `{id, isRead, unreadCount}` |
| POST | `/notifications/{id}/unread` | — | `{id, isRead, unreadCount}` |
| POST | `/notifications/read-all` | — | `unreadCount` |

Each item = `{id, type, title, body, iconTone, isRead, occurredAt, relativeTime}`.

## Dashboard

| Method | Path | Query | Returns |
| --- | --- | --- | --- |
| GET | `/dashboard` | `year?`, `month?` | `Dashboard` |

`Dashboard` = `{owner, month, safeToSpend, stats, budgets: BudgetItem[],
recentTransactions: Transaction[], week: WeeklyInsight, unreadNotifications,
sync: {lastSyncedAt, offlineFirst}}` where `safeToSpend` =
`{amountIrr, dailyAllowanceIrr, percentOfBudget, status, statusLabel}` and
`stats` = `{spentIrr, spentDeltaPercent?, savedIrr, savedDeltaIrr}`.

## Settings and rates

| Method | Path | Body | Returns |
| --- | --- | --- | --- |
| GET | `/settings` | — | `Settings` |
| PUT | `/settings` | same fields as profile preferences | `Settings` |
| GET | `/settings/currencies` | — | `{code, decimals, symbol, rateToIrr, names}`[] |
| GET | `/settings/locales` | — | `{code, name, nativeName, direction}`[] |
| POST | `/settings/convert` | `{from, to, amount}` | `{from, to, amount, result, rate, updatedAt}` |
| GET | `/rates` | — | same list as currencies |

`Settings` = `{displayName, email, avatarInitials, homeCurrency,
secondaryDisplay, language, appearance, quietHours, version, privacy,
ratesUpdatedAt, currencies, locales}`. Seeded rates (IRR base): IRR 1, USD
1,042,000, EUR 1,128,000, AED 283,700, TRY 30,200.

## Sync

The client queues offline writes and replays them here on reconnect. Only
transactions sync; everything else is read-only or edited online.

| Method | Path | Body / Query | Returns |
| --- | --- | --- | --- |
| POST | `/sync/push` | `{items: SyncTransactionItem[]}` | `{applied: id[], skipped: id[], serverTime, lastSyncedAt}` |
| GET | `/sync/pull` | `since?` (ISO-8601) | `{serverTime, lastSyncedAt, transactions, budgets, goals, settings}` |

`SyncTransactionItem` = `{id, kind, amountIrr, categorySlug, accountId?, note?,
occurredAt?, clientUpdatedAt?, enteredCurrency?, enteredAmount?, fxRate?}`.

- The client id doubles as the server row id (upsert key).
- **Last-write-wins:** an item older than the stored `clientUpdatedAt` is
  reported in `skipped`, not applied.
- Invalid items (unknown category, kind mismatch, non-positive amount) are
  `skipped`.
- `pull` returns rows at or after `since` (max 500 transactions) plus the full
  budgets, goals and settings snapshot.

## Health

`GET /health` is the one bare endpoint (no `Result<T>`):

```json
{ "status": "Healthy", "totalDuration": "00:00:00.004" }
```

## Mock (alpha)

Alpha builds (`ApiMode.MOCK`) serve this contract from an in-memory store, so
the UI behaves identically with or without a server. Deletes return
`Result<bool>` just like the real API, and `GET /health` is bare.

Seeded account: **demo@aram.app / demo1234** (Ava Bennett, IRR) with three
accounts, three budgets, two goals, a welcome notification and about a month of
transactions. `forgot-password` exposes `developmentToken:
"mock-reset-token"`; any reset token is **not** valid in production.

Debug builds can switch scenarios from Profile → Developer → Network scenario:

| Scenario | Effect |
| --- | --- |
| Normal | Serve the contract |
| Offline | Every request fails as `IOException` → `ApiError.Offline` |
| Server error | Every request returns `500` |
| Unauthorized | The next request returns `401`, then resets to Normal |
| Slow | Adds 3 s latency |

Latency is 300–800 ms in the app and 0 in unit tests.
