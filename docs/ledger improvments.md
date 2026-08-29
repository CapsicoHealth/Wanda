# Credit Ledger: Org / Project / Item Spend Attribution

**Status: COMPLETE and in production code.** Schema, views, `CreditHelper`, both charge call sites, the
`/svc/wanda/credits/report` endpoint and the Floria usage dashboard are all implemented and compiling.

This document started life as migration notes. Now that the migration is done it is maintained as the
**reference for integrating further apps** — sections 1–3 are the contract you code against, section 4 is
the historical record of what changed, and section 5 is what remains unbuilt.

---

## 1. The model

### 1.1 `UserPlanCreditLedger` — the WORM ledger

Append-only source of truth for every credit movement. `UserPlanSubscription.creditsBalance` is only a
denormalized cache and can always be rebuilt with a `SUM(amount)` per subscription.

| Column | Notes |
|---|---|
| `userRefnum` | Always set. Who spent it. |
| `organizationRefnum` | Nullable FK. Derived server-side from `projectRefnum` when not supplied. |
| `projectRefnum` | Nullable FK. The container the work happened in. |
| `subscriptionRefnum` | Always set. The wallet. |
| `billingRefnum` | Set on `GRANT` rows only. |
| `paymentSystemProductId` | Denormalized product grouping key. |
| `type` | `GRANT` / `USE` / `BONUS` / `ADJ`. |
| `amount` | **Signed.** Positive for `GRANT`/`BONUS`, **negative for `USE`**. |
| `balanceAfter` | Balance snapshot so an audit never recomputes a running total. |
| `itemType` | Free-form, e.g. `"Flow"`, `"Agent"`, `"Document"`, `"order:<id>"`. |
| `itemId` | Free-form, the item's own key. |
| `itemLabel` | Human-readable display text. |

**Credit convention:** 1 credit = 1 US cent. Charges are computed as `costUSD * 100 * markupCostPct`.

### 1.2 Item identity — `itemType` + `itemId` are one key

`itemId` is only unique **within** an `itemType`. A Flow and an Agent can perfectly well carry the same id,
because every app picks its own key model (UUID, refnum, slug). So:

- The two are an **inseparable composite key** — in the views, the indices, and the report endpoint.
- Supplying one without the other is a `BadRequestException`, never a wildcard.
- `itemType` is deliberately **free-form**, not an enum: apps exist outside Wanda, so Wanda can never
  exhaustively enumerate asset types, now or ever.

The three item columns split three previously-conflated concerns:

| Column | Role | Cardinality |
|---|---|---|
| `itemType` | the coarse, filterable *bucket* | low — good for dropdowns and group-bys |
| `itemId` | the machine *key* for aggregation | high — unique within a type |
| `itemLabel` | the human *display* string | high, and it may drift over time |

Because `itemLabel` can change (a flow gets renamed), the views expose it as `MAX(itemLabel)` so it rides
along for display without splitting the aggregation group.

### 1.3 The reporting views

All are `USE`-only projections, narrow by design (no `billingRefnum`, `balanceAfter`, `subscriptionRefnum`).

**90-day rollups** — columns `amount30Days`, `amount60Days`, `amount90Days`:

| View | Grain | Queries |
|---|---|---|
| `...MinimalOrganizationProjectView` | project | `Project` (1 row), `Organization` (n rows, by project title) |
| `...MinimalProjectUserView` | project × user | `Project` (per user), `User` (per project) |
| `...MinimalUserItemView` | user × item, **any project** | `UserItem` (1 row), `User`, `UserItemType` |
| `...MinimalProjectItemView` | project × item, all users | `ProjectItem` (1 row), `Project`, `ProjectItemType` |

**Per-day breakdowns** — a `day` DATE column (`date_trunc('day', created)::DATE`) plus a single `amount`,
over the **last 30 days only**:

| View | Queries |
|---|---|
| `...MinimalUserItemDailyView` | `UserItem`, `UserItemType`, `User` |
| `...MinimalProjectItemDailyView` | `ProjectItem`, `ProjectItemType`, `Project` |
| `...MinimalOrganizationProjectDailyView` | `Project`, `Organization` |
| `...MinimalProjectUserDailyView` | `Project`, `ProjectUser` |

> **Tilda naming rule worth remembering:** a query whose where-clause covers the view's *full group-by key*
> and has no `orderBy` generates a single-row `lookupBy...`; anything else generates a paged
> `lookupWhere...` returning a `List`. That is why `UserItem`/`ProjectItem` are single-row while their
> `...ItemType` siblings are lists.

### 1.4 Indices

Eight on the ledger: `subscriptionRefnum`; `userRefnum, paymentSystemProductId`; `organizationRefnum`;
`organizationRefnum, userRefnum`; `projectRefnum`; `projectRefnum, userRefnum`;
`userRefnum, itemType, itemId`; `projectRefnum, itemType, itemId` — all `created desc`.

**Performance note (resolved):** the nullable columns cost effectively nothing on rows that don't populate
them — Postgres stores no data for a NULL, just a null-bitmap bit. Row width is *not* the lever here.
The real levers, in order, are **index count** (now 8, the thing to watch as write volume climbs), then at
scale partitioning / BRIN on `created`. Views save bandwidth, not heap I/O — Postgres is a row store, so a
narrow view still reads whole pages; index-only scans (covering index + visibility map) are what actually
cut I/O. Tilda's history/roll-off feature can keep the primary table windowed at ~90 days with older rows
moved to a secondary table, which defers all of this comfortably.

---

## 2. Integration guide — recording spend (the write side)

### 2.1 The API

`CreditHelper` is the **single choke-point** for all credit mutation. Never write ledger rows directly.

```java
// Short form: no container attribution. Charge is recorded but unattributed.
CreditStatus charge(Connection C, User_Data U, String paymentSystemProductId, BigDecimal credits,
                    String itemType, String itemId, String itemLabel)

// Full form: attributed to an org and/or project.
CreditStatus charge(Connection C, User_Data U, String paymentSystemProductId, BigDecimal credits,
                    long organizationRefnum, long projectRefnum,
                    String itemType, String itemId, String itemLabel)

// Same as charge() but refuses to go below zero (floor guard). No callers today.
boolean consume(Connection C, User_Data U, String paymentSystemProductId, BigDecimal credits,
                long organizationRefnum, long projectRefnum,
                String itemType, String itemId, String itemLabel)

// Admin correction. Deliberately carries NO org/project: it is a wallet-level financial
// action, not metered work inside a project, so it stays out of per-project spend reporting.
UserPlanCreditLedger_Data adjust(Connection C, UserPlanSubscription_Data UPS, BigDecimal amount,
                                 String itemType, String itemLabel)
```

Use `SystemValues.EVIL_VALUE` (-666) for a refnum that does not apply; the column is left NULL.

### 2.2 Checklist for wiring up a new app

1. **Servlet** — accept two *optional* request params:
   ```java
   long organizationRefnum = Req.getParamLong("organizationRefnum", false);
   long projectRefnum      = Req.getParamLong("projectRefnum", false);
   ```
2. **Do NOT ACL-check them.** They are reporting labels the caller attaches to their *own* spend, never a
   read path into anyone else's data. All access control lives in the report endpoint (§3). Adding a check
   here buys nothing and breaks legitimate unattributed calls.
3. **Pick an `itemType`** — a stable, coarse, low-cardinality bucket string. This is what users will filter
   the dashboard dropdown by, so keep it human-meaningful (`"Flow"`, not `"FLW_V2"`).
4. **Pick an `itemId`** — whatever key your app already has. A UUID if you have one, the refnum as a string
   if you don't (this is exactly why the column is free-form; see `GenAIDocumentPromptAgentic`).
5. **Pick an `itemLabel`** — the display identity, e.g. `title + " (" + fileName + ")"`. Not a storage URL,
   not a full path.
6. **Client** — send `projectRefnum` when there *is* a project context, and **omit the param entirely**
   (don't send null) when there isn't. Don't bother sending `organizationRefnum`: the client usually doesn't
   know it, and the server derives it from the project.

### 2.3 Why the org refnum is denormalized onto the row

`post()` derives `organizationRefnum` from `projectRefnum` when the caller supplies a project but no org
(the common case). This matters for two reasons:

- The org-level report branch reads `organizationRefnum` **straight off the ledger row** rather than joining
  back through `Project`, so without this the org report would silently return nothing.
- The row records attribution **as of the moment of the charge**. Moving a project between orgs later can
  never retroactively rewrite historical spend — which is the whole point of a WORM ledger.

It is best-effort: an unreadable project, or one with no org, just leaves the column NULL and never fails
the charge. Billing must not break because a lookup for a *reporting label* failed.

### 2.4 Currently instrumented call sites

| Call site | `itemType` | `itemId` | Attribution |
|---|---|---|---|
| `AgentCall` (`/svc/agentic/agent/call`) | `"Flow"` or `"Agent"` | `w.getId()` / `agent.getId()` | project sent by `agentic-flow-studio.js` when in a project |
| `GenAIDocumentPromptAgentic` | `"Document"` | `docRefnum` as string | **none yet** — client sends no project |
| `CreditHelper.grant` / signup bonus | `"order:<id>"` | null | n/a (wallet-level) |

CapSensa's `FlowTracked*` servlets never call `CreditHelper` — confirmed, no change needed there.

---

## 3. Integration guide — reporting spend (the read side)

### 3.1 `GET /svc/wanda/credits/report`

Generic Wanda endpoint. **All aggregation happens in Postgres** via the views in §1.3 — the servlet only
picks the right view/query and enforces access. It never sums anything in Java. That is deliberate: the
Tilda way is to push rollups into cacheable views rather than hand-rolling queries or looping in Java.

**Params:** `organizationRefnum`, `projectRefnum`, `itemType`, `itemId`, `perUser` (bool), `perItem` (bool),
`daily` (bool) — all optional, but at least one scoping combination is required.

A request is **item-scoped** when it carries an `itemType` *or* sets `perItem=true`. It then narrows in three
progressively tighter steps:

| Item params | Meaning |
|---|---|
| `perItem=true` alone | every item, whatever its type |
| `itemType` alone | every item of that type |
| `itemType` + `itemId` | one specific item (single row) |

`itemId` **without** `itemType` is rejected — an id is only unique within a type, so it can't be resolved.
The converse is fine and is the `...ItemType` report.

| Params | Returns | Requires |
|---|---|---|
| item-scoped + `projectRefnum` | that project's item spend, all users combined | project **OWNER/ADMIN** |
| item-scoped, no project | the **caller's own** item spend, across all projects | signed in |
| `organizationRefnum` only | n rows, one per project, by project title | org OWNER/ADMIN |
| `projectRefnum`, `perUser=false` | 1 row: project total | project member (READER+) |
| `projectRefnum`, `perUser=true` | n rows, one per user | project OWNER/ADMIN |
| `perUser=true`, no project | n rows, one per project, for the caller | signed in |

Note the project-side item shapes require **OWNER/ADMIN**, not the plain membership the project *total*
needs. An itemised breakdown is a materially finer lens on what a team is doing than a single number, and in
a small project it edges close to attributing work to individuals — so it is held to the same bar as the
per-user breakdown.

`daily=true` is **orthogonal** to all of the above: it swaps the three rollup columns for a *list* of rows
carrying `day` + a single `amount`, over the last 30 days. Access rules are identical — `daily` changes time
granularity, never scope.

**Org/project cross-validation.** When both `organizationRefnum` and `projectRefnum` are supplied, the ACL
check runs against the **project** (which alone fully determines scope), and then the project record is read
and its `organizationRefnum` asserted to match the one passed in. This is *not* load-bearing for access
control — it is a consistency assertion. A mismatch means the client is carrying stale state (the project
moved orgs) or the URL was tampered with; either way, silently answering with the project's real data would
be wrong. Because `Project.organizationRefnum` is **nullable**, an unaffiliated project is treated as a
mismatch rather than being allowed to slip through a `==` comparison. Returns `400`, or `404` if the project
doesn't exist.

### 3.2 Two things that will bite the front-end

> **① Amounts are NEGATIVE.** `USE` rows store signed debits, so every `amount`, `amount30Days`,
> `amount60Days` and `amount90Days` returned by these views is a **negative number**. Wrap in
> `Math.abs(Number(x) || 0)` before displaying, exactly as `module-payments.js` already does. Forgetting
> this yields charts that render downward or KPIs showing `-1,240 credits`.

> **② Days with no spend produce NO row.** The ledger is only written when credits actually move, so a
> `daily=true` response is sparse. Any continuous 30-day chart must **zero-fill the gaps client-side**.
> This is intentional: it keeps the payload proportional to real activity rather than to window length, and
> avoids a `generate_series` join in every view.

### 3.3 Response field names

Straight from the view columns (output map is `["*"]`):

- Rollup shapes: `organizationRefnum`, `organizationTitle`, `projectRefnum`, `projectTitle`, `userRefnum`,
  `userId`, `itemType`, `itemId`, `itemLabel`, `amount30Days`, `amount60Days`, `amount90Days`
- Daily shapes: the same identity columns, plus `day` (a DATE) and `amount`

An entity with no spend has **no row** in the `USE`-only views. That is *zero spend*, not an error — the
endpoint returns an empty payload, never a 404. Render it as `0`.

### 3.4 Suggested front-end shapes

- **Per-item spend badge** on a flow / agent / document page → `itemType`+`itemId`, plus `projectRefnum`
  when the page sits inside a project (needs OWNER/ADMIN), omitted for the user's own global figure.
- **"Top flows by cost" / "Top documents by cost" table** → `itemType` alone, which returns every item of
  that type ordered by 30-day spend. This is the `...ItemType` shape.
- **Full item leaderboard across all types** → `perItem=true`.
- **30-day sparkline** on any of the above → add `daily=true` (and zero-fill).
- **Project cost dashboard** → `projectRefnum`; add `perUser=true` for the admin breakdown.
- **Org-admin dashboard** → `organizationRefnum`.

### 3.5 Adjacent endpoints

| Endpoint | Purpose | Notes |
|---|---|---|
| `/svc/wanda/credits/balance` | wallet balance | Short-TTL cached, **cheap and poll-safe**. |
| `/svc/wanda/credits/usage` | raw `USE` rows | Uncached, up to 5000 rows, bucketed client-side. |
| `/svc/wanda/credits/history` | raw ledger rows | Same shape change as usage. |
| `/svc/wanda/credits/report` | pre-aggregated rollups | This document. |

`balance` and `usage` were deliberately **not merged** into one call: balance is short-TTL cached and safe
to poll, usage is uncached and scans the ledger. Merging them would force every cheap balance poll to pay
the expensive scan.

`usage` / `history` return raw rows and so expose the renamed fields directly: `reference`/`notes` are now
`itemType`/`itemLabel`, and `organizationRefnum`/`projectRefnum`/`itemId` are new. Both remain strictly
user-scoped by design; cross-user reporting only ever goes through `/report`.

---

## 4. What changed (historical record)

**Schema.** Added `organizationRefnum` and `projectRefnum` as nullable FKs. Renamed `reference` → `itemType`
and `notes` → `itemLabel` (recorded in `migrations.renames`, so historical rows survive untouched — no data
change, same values). Added `itemId`, NULL on every historical row. Added 6 indices and 6 views.

**Java.** `CreditHelper` renamed params throughout, gained `itemId` / `organizationRefnum` / `projectRefnum`
on `charge`/`consume`/`post`, and `post` gained the org-derivation described in §2.3.

**Servlets.** All Wanda credit services moved into the `wanda` namespace: `/svc/user/credits/*` →
`/svc/wanda/credits/*`. `UserCreditsReport` is new.

**Client (phase 2).** `module-payments.js`'s `UsageDashboard` was renamed *throughout*, not just at the row
reads — `state.reference`/`state.note` → `state.itemType`/`state.itemLabel`, `byReference`/`byNotes` →
`byItemType`/`byItemLabel`, `data-reference` → `data-item-type` — so no half-old vocabulary remains.
`itemId` is surfaced but deliberately **not** used as a bucketing key: `itemLabel` stays the grouping key so
historical rows (whose `itemId` is NULL) still aggregate alongside new ones. It rides along as the first
non-null value per bucket, shown as a tooltip and as a CSV column.

`agentic-flow-studio.js`'s `launchFlowChat` threads its existing `orgProjectRefnum` into
`/svc/agentic/agent/call` as `projectRefnum` on both charge paths. `flow-studio/js/backend.js` needed no
change (it only reads `amount`/`balance`).

### Design decisions resolved along the way

- **`itemId` without `itemType`** → rejected as a bad request; an id is only unique within a type, so a
  half-supplied pair can never be safely read as a wildcard. **`itemType` without `itemId`** is the opposite
  case and is fully supported — it is the "all items of this type" report.
- **`organizationRefnum` + `perUser=true`, no project** → falls through to the self-scoped branch; the org
  param is ignored, since that branch can only ever return the caller's own rows.
- **`projectRefnum` + `organizationRefnum` together** → the project drives both the ACL check and the scope,
  and the org is then **cross-validated** against the project record (see §3.1). Originally these were simply
  not compared; asserting the match turns a class of latent client bug into an immediate, obvious one.
- **`perUser` alongside item params** → ignored; the item shapes have no per-user breakdown by design.
- **No scoping params at all** → rejected. There is deliberately no "everything, everywhere" report, which
  would be a cross-tenant read.
- **`adjust()` carries no org/project** — a wallet-level financial action is not project work.

---

## 5. Still open

Nothing blocking. All of the below is new surface area on top of what exists.

- **No client consumer for `/svc/wanda/credits/report` yet.** The endpoint, all its shapes and its access
  rules are done; the dashboards in §3.4 still need building. *This is the current workstream.*
- **`GenAIDocumentPromptAgentic`'s client** (Health Buddy Docs) sends no `projectRefnum`, so those charges
  stay unattributed. Not blocked though — `itemType="Document"` / `itemId=docRefnum` *are* written, so
  per-document global spend reports correctly today; only the project rollup is missing.
- **Cross-user global item spend** ("what has this flow cost across the whole platform, all users, no
  project") has no shape in the endpoint. A genuine site-admin question, but it needs a real platform-admin
  role check rather than the project/org ACLs used today, so it was deliberately deferred.
- **Roll-off / partitioning** unnecessary for now; the 90-day view window plus Tilda's history feature
  covers it. Revisit if the ledger turns write-heavy.
