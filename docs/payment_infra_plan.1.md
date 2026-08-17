# Payment Infrastructure Plan — Phase 1: Data Model (Tilda Schema)

**Status:** Draft for review — no implementation yet.
**Target file:** `Wanda/src/wanda/data/_tilda.Wanda.json`
**Approach:** Hard cutover. The following existing objects are **removed** and replaced:
`Plan`, `PlanPricing`, `UserPlanPreOrder`, `UserPlanSubscription`, `UserPlanBilling`.
Any views/servlets/helpers referencing them (`PlanHelper`, `UserBillingView`, `UserPlanList`,
`UserPlanStatus`, `PaymentOrderCreate`, `PaymentOrderCapture`) are addressed in Phase 2/3.

---

## 1. Design Principles

- **Native multi-currency pricing.** No currency conversion. Each sellable tier defines its own
  amount per currency (e.g., $20 USD, €20 EUR, ₹1500 INR) as independent, admin-entered values.
- **Multi-tier catalog.** A `Service` can have multiple `ServicePricing` rows (tiers), each with
  its own currency, cycle, baseline price, included credits, and overage rate.
- **Credits are ledger-based, not a mutable counter.** All grants/consumption/purchases/adjustments
  are appended to a WORM ledger; balance is derived (and cached) from it. This gives auditability
  and avoids race conditions on concurrent updates.
- **Orders are UUID-keyed, not user-keyed.** A user may have multiple concurrent in-flight orders
  (e.g., subscribing to two services, or a scheduled overage charge firing while the user manually
  tops up). This directly fixes the single-slot `UserPlanPreOrder.lookupByUser` limitation.
- **Cancellation / tier changes take effect at the next cycle boundary.** No proration. The
  subscription keeps its current tier/pricing until `effectiveEndDt` (cancel) or the cycle rolls
  over (tier change), at which point the scheduler (Phase 2) applies the change.
- **Scheduling is out-of-band.** This schema stores everything a scheduler needs (next charge
  date, amounts, vaulted payment reference) but does not assume any particular cron/trigger
  mechanism (see Phase 2 `SchedulingHelper`).

---

## 2. New Objects

### 2.1 `Service`
Catalog entry for something a user can browse and subscribe to. Purely descriptive; no pricing.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| code | STRING(100) | false | Unique business key, e.g. `"AI_ASSISTANT"` |
| label | STRING(255) | false | Display name |
| descr | STRING[] | false | Multi-line bullet description (mirrors old `Plan.descr`) |
| category | STRING(100) | true | Optional grouping for catalog browsing |
| pos | SHORT | false | Sort order for display |
| start | DATE | false | Valid-from date |
| end | DATE | true | Valid-until date (null = open-ended) |
| active | BOOLEAN | false, default true | Admin on/off switch independent of dates |

**Indices:** unique `code`; `pos` for ordering.
**Queries:** `AllByPositions` (active catalog listing), `Codes` (lookup by code list).
**outputMaps:** JSON `*` for catalog display.

---

### 2.2 `ServicePricing`
One row per sellable tier × currency for a `Service`. Replaces `PlanPricing`.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| serviceRefnum | FK → Service.refnum | false | |
| tierCode | STRING(100) | false | e.g. `"BASIC"`, `"PRO"` — lets a Service expose several tiers |
| tierLabel | STRING(255) | false | Display label for the tier |
| pos | SHORT | false | Sort order among tiers |
| currency | STRING(10) | false | ISO 4217 code: USD, CAD, GBP, EUR, INR (extensible, not enum-locked) |
| cycle | CHAR | false | `M` (monthly) / `Y` (yearly) — same enum pattern as old `UserPlanSubscription.cycle` |
| baselineAmount | NUMERIC | false | Native-currency price for the cycle (no conversion) |
| includedCredits | NUMERIC | false | Credits granted per cycle at baseline price |
| overageAmountPerCredit | NUMERIC | false | Native-currency price per credit beyond the included amount |
| minTopUpCredits | NUMERIC | true | Optional minimum for manual top-up purchases against this tier's rate |
| active | BOOLEAN | false, default true | |
| start / end | DATE / DATE | false / true | Same validity pattern as `Service` |

**Indices:** `(serviceRefnum, currency, tierCode)` unique-ish lookup; `(serviceRefnum, active)`.
**Queries:** `AllByService` (all tiers/currencies for a service), `ServiceCurrency` (tiers for a
service in one currency, for a specific storefront locale).
**outputMaps:** JSON `*`.

**Note:** A pure "credit pack" (no baseline subscription, just buy N credits) is modeled as a
`ServicePricing` row with `cycle` irrelevant/ignored and consumed only through the top-up flow
(Phase 2) rather than the renewal flow — avoids a whole separate table for now. If this proves
awkward once implemented, we can split into a dedicated `CreditPackPricing` later.

---

### 2.3 `UserServiceSubscription`
Replaces `UserPlanSubscription`. Unlike before, a user may have **multiple concurrent active
subscriptions** (one per `Service`), so lookups are keyed by `(userRefnum, serviceRefnum)`, not
just `userRefnum`.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| userRefnum | FK → User.refnum | false | |
| serviceRefnum | FK → Service.refnum | false | |
| pricingRefnum | FK → ServicePricing.refnum | false | Current active tier/currency/cycle |
| active | BOOLEAN | false | Whether this subscription is currently in effect |
| startDt | DATE | false | |
| endDt | DATE | true | Set when subscription actually ends (post cancellation) |
| cancelRequestedDt | DATETIME | true | When the user asked to cancel |
| effectiveEndDt | DATE | true | End of the current paid cycle; subscription stays active until this date |
| pendingPricingRefnum | FK → ServicePricing.refnum | true | Next-cycle tier change target, if any (no proration) |
| nextChargeDt | DATE | true | Date the scheduler should attempt the next baseline renewal |
| spendCapPerCycle | NUMERIC | true | Optional user-set ceiling on overage spend per cycle (alerts/cost control) |
| autoRefill | BOOLEAN | false, default false | Whether auto top-up is enabled |
| autoRefillThreshold | NUMERIC | true | "If balance falls below X credits..." |
| autoRefillAmount | NUMERIC | true | "...buy Y more credits automatically" |
| paymentProvider | STRING(128) | false | e.g. `"paypal"` |
| vaultTokenRefnum | FK → PaymentVaultToken.refnum | true | Stored payment method used for scheduled charges (see 2.7) |
| currency | STRING(10) | false | Denormalized from `ServicePricing` for quick filtering/display |

**Indices:**
- `(userRefnum, serviceRefnum)` with `subWhere: active = true` — "does this user have this service active"
- `(userRefnum)` ordered by `startDt desc` — "all subscriptions for a user" (dashboard)
- `(nextChargeDt)` — scheduler sweep query
- `(effectiveEndDt)` where `cancelRequestedDt is not null` — scheduler sweep for expirations

**Queries:** `UserActiveByService`, `UserAll`, `DueForCharge(date)`, `DueForExpiry(date)`.

---

### 2.4 `UserServiceCreditLedger`  *(WORM — append-only, like `TourUserClick`/`AccessLog`)*
The source of truth for credit movements. Never updated or deleted, only inserted.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| subscriptionRefnum | FK → UserServiceSubscription.refnum | false | |
| userRefnum | FK → User.refnum | false | Denormalized for direct user-scoped queries |
| type | STRING(20) | false | Enum: `GRANT` (cycle renewal), `PURCHASE` (top-up), `CONSUMPTION` (usage), `ADJUSTMENT` (manual/admin), `EXPIRY` (unused credits forfeited, if that policy is chosen later) |
| amount | NUMERIC | false | Positive for grants/purchases, negative for consumption/expiry |
| balanceAfter | NUMERIC | false | Running balance snapshot at time of insert, for fast audit without recomputation |
| reference | STRING(256) | true | Free-form pointer to what consumed/granted this (e.g. a job id, an order id) |
| orderRefnum | FK → PaymentOrder.refnum | true | Set when the entry corresponds to a paid grant/purchase |
| notes | STRING(1024) | true | Optional human-readable detail |

**Indices:** `(subscriptionRefnum)` ordered by `created desc`; `(userRefnum)` ordered by `created desc`.
**Queries:** `BySubscription`, `ByUser`, `BySubscriptionSince(date)` (for cycle-scoped overage calc).

---

### 2.5 `UserServiceCreditBalance`  *(fast-read cache, one row per subscription)*
Materialized balance so we don't scan the ledger on every metering check. Rebuildable from the
ledger at any time (reconciliation job), so this table is a cache, not the source of truth.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| subscriptionRefnum | FK → UserServiceSubscription.refnum, unique | false | |
| userRefnum | FK → User.refnum | false | |
| balance | NUMERIC | false | Current credit balance |
| cycleConsumed | NUMERIC | false | Credits consumed so far in the current cycle (reset on renewal) — drives overage calc |
| lastLedgerRefnum | FK → UserServiceCreditLedger.refnum | true | Last ledger row folded into this balance, for incremental updates/reconciliation |

**Indices:** unique `subscriptionRefnum`; `(userRefnum)`.
**Queries:** `BySubscription`, `ByUser`.

---

### 2.6 `PaymentOrder`
Replaces both `UserPlanPreOrder` and `UserPlanBilling`, generalized and UUID-keyed so multiple
concurrent orders per user are possible.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| orderUUID | STRING(64) | false, unique | Our own idempotency key, generated at order-create time |
| userRefnum | FK → User.refnum | false | |
| subscriptionRefnum | FK → UserServiceSubscription.refnum | true | Null for a brand-new subscription order (not yet created) |
| serviceRefnum | FK → Service.refnum | false | |
| pricingRefnum | FK → ServicePricing.refnum | false | Tier/currency/cycle in effect at order time |
| orderType | STRING(20) | false | Enum: `NEW_SUBSCRIPTION`, `RENEWAL`, `TOP_UP`, `OVERAGE`, `TIER_CHANGE` |
| initiatedBy | STRING(20) | false | Enum: `USER`, `SCHEDULER`, `ADMIN` |
| paymentProvider | STRING(128) | false | |
| providerOrderId | STRING(128) | true | PayPal order id once created |
| providerCaptureId | STRING(128) | true | PayPal capture id once captured |
| status | STRING(4) | false | `CRTD`, `PNDG`, `PAID`, `VOID`, `RFND`, `FAIL` (same enum shape as old `UserPlanBilling.status`) |
| amount | NUMERIC | false | Native-currency amount charged |
| currency | STRING(10) | false | |
| creditsGranted | NUMERIC | true | Credits this order grants upon success (baseline or top-up amount) |
| orderDt | DATETIME | false | |
| capturedDt | DATETIME | true | |
| message | STRING(4096) | true | Human-readable status/failure message |
| orderDetails | JSON | true | Raw provider create-order payload |
| orderCapture | JSON | true | Raw provider capture payload |
| retryCount | INTEGER | false, default 0 | For scheduler-initiated charges that fail and are retried |

**Indices:**
- unique `orderUUID`
- `(userRefnum)` ordered by `orderDt desc` — order history / dashboard
- `(subscriptionRefnum)` ordered by `orderDt desc`
- `(status)` where `status in ('CRTD','PNDG')` — recover/reconcile stuck orders
- `(providerOrderId)`

**Queries:** `ByUUID`, `ByUser`, `BySubscription`, `PendingOlderThan(timestamp)` (reconciliation sweep).

---

### 2.7 `PaymentVaultToken`
Stores a reference to a saved PayPal payment method so the scheduler can charge without the user
present. **No raw card/account data stored** — only the provider-issued vault token/id.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| userRefnum | FK → User.refnum | false | |
| paymentProvider | STRING(128) | false | |
| providerVaultId | STRING(256) | false | The PayPal-issued payment-token id |
| active | BOOLEAN | false, default true | Set false if the user removes/replaces their payment method |
| label | STRING(255) | true | e.g. masked payer email or last-4 if PayPal exposes it, for display |
| createdFromOrderRefnum | FK → PaymentOrder.refnum | true | The order during which this was vaulted |

**Indices:** `(userRefnum)` where `active = true`.
**Queries:** `ActiveByUser`.

---

### 2.8 `PaymentEvent`  *(WORM — append-only audit/log)*
Every scheduler-initiated charge attempt (and optionally user-initiated ones) is logged here,
independent of `PaymentOrder`'s mutable status, so we always have an immutable record even if
`PaymentOrder` bookkeeping has a bug. Directly addresses the "we can't fail here anymore" fragility
in the current `PaymentOrderCapture`.

| Column | Type | Nullable | Notes |
|---|---|---|---|
| orderRefnum | FK → PaymentOrder.refnum | true | Null if the attempt failed before an order row existed |
| userRefnum | FK → User.refnum | false | |
| subscriptionRefnum | FK → UserServiceSubscription.refnum | true | |
| eventType | STRING(30) | false | `CHARGE_ATTEMPT`, `CHARGE_SUCCESS`, `CHARGE_FAILURE`, `RECONCILE_MISMATCH` |
| paymentProvider | STRING(128) | false | |
| rawPayload | JSON | true | Raw request/response captured for debugging |
| message | STRING(4096) | true | |

**Indices:** `(userRefnum)` ordered by `created desc`; `(orderRefnum)`.
**Queries:** `ByUser`, `ByOrder`, `RecentFailures(since)` (admin monitoring dashboard, Phase 4).

---

## 3. Views to (re)build

- **`UserServiceCatalogView`** — joins `Service` + `ServicePricing`, filtered to active/current
  date range, for storefront browsing (replaces the ad hoc `PlanHelper.getAvailablePlans`).
- **`UserServiceSubscriptionView`** — joins `UserServiceSubscription` + `Service` + `ServicePricing`
  + `UserServiceCreditBalance`, for the "my subscriptions" dashboard (replaces `UserBillingView`).
- **`PaymentOrderHistoryView`** — joins `PaymentOrder` + `Service` + `ServicePricing`, for user and
  admin order-history screens.

(Exact column lists to be finalized in Phase 2 once servlet response shapes are settled.)

---

## 4. Cutover / Migration Notes

- Since this is a hard cutover and current usage is limited/test-stage per your description, no
  data migration script is planned by default — existing `UserPlan*` rows are abandoned. **Confirm
  before execution** if any production users currently hold a paid, active `UserPlanBilling`
  record that must be carried forward manually into `UserServiceSubscription` + a backfilled
  `UserServiceCreditLedger` grant.
- `AccessLog.paymentCreate` / `AccessLog.paymentCapture` boolean columns and the
  `AccessLogMonthlyView`/`AccessLogDailyView`/`AccessLogHourlyView` aggregates that reference them
  remain unchanged — they're generic flags set by servlet plumbing, not tied to the old table
  names.
- `importers.promos.Plan` (the Java helper class used by `PlanHelper`, distinct from the DB
  `Plan` object) will need a replacement/rework in Phase 2 once the new catalog servlets exist.

---

## 5. Open Items Before Implementation

1. Should `UserServiceCreditLedger.type = EXPIRY` (unused credits lost at cycle end) be supported
   now, or do included credits simply roll over indefinitely until consumed? Affects whether
   `UserServiceCreditBalance.cycleConsumed` needs a "credits remaining this cycle" companion field.
2. Confirm `currency` should remain a free-form `STRING(10)` (extensible) rather than an
   enumerated list restricted to `USD/CAD/GBP/EUR/INR`, in case more currencies are added later
   without a schema change.
3. Confirm whether `PaymentVaultToken` should allow multiple active tokens per user (e.g., one per
   payment provider) — current design already supports this via `(userRefnum, paymentProvider)`,
   just flagging for sign-off.
