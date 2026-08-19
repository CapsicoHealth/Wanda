# Payment Infrastructure — Simple Enhancement Plan (Incremental, Ship-Fast)

**Status:** Proposed. Alternative to the 4-phase rewrite in `payment_infra_plan.1-4.md`.
**Goal:** Keep the existing `Plan` / `PlanPricing` / `UserPlanSubscription` / `UserPlanBilling`
infrastructure, fix its real bugs, and extend it *additively* to support:
1. Multiple **products** (today: only "CapsicoHealth GenAILearning"), each with its own set of tiers,
   and **one active subscription per user per product**.
2. **Pre-paid / credit plans** priced as a one-time amount (e.g. $10 / $25 / $50) with no month/year
   cycle, consumed by usage, re-bought when exhausted, with balances that **accumulate across tiers**.

**Explicitly NOT in scope** (stays in the 4-phase plan for later): PayPal vaulting, scheduler-driven
auto-renewal, auto-refill, overage billing, admin catalog CRUD screens, `PaymentEvent` audit table.

**Execution order agreed:** all `_tilda.Wanda.json` model changes land first → Tilda codegen → DB
migration → then the Java side. Tilda regenerating index-derived methods and breaking current call
sites is expected and desirable; the compiler errors are the work list.

**Progress:**
- ✅ §5.1 model changes applied, code re-generated, database migrated.
- ✅ §5.2 Java complete: 9 compile errors resolved; product-scoping throughout; `CreditHelper` +
  grant-on-capture (§3.5–3.6); credit servlets (§3.7); app contract (§3.9); `Plan_Factory` overloads;
  `productId` filter + `creditStatus` on `UserPlanStatus`/`UserPlanList`.
- ✅ Bug fixes §1.1, §1.2/1.3, §1.4, §1.5, §1.6 done. §1.7 **partially**: the duplicate
  `getAvailablePlans` call is removed, but the `return null` for a logged-in user with no promo code is
  **unchanged pending your decision** (see Open Question 7).
- ✅ §4 data payload updated (GenAILearning tagged `S`/`X`, Agentic credit packs added).
- ✅ Front-end: `planCode` on capture; `planType=C` rendering; one-time rows in billing history;
  `PopupLogin.topUpCredits(productId, onComplete)`; `initButtons`/`paintPlans` accept an `onComplete`
  so a mid-workflow top-up doesn't reload the page and discard on-screen results.
- ✅ `RequestUtil.getParamBigDecimal()` added.
- ⬜ **Not done / needs you:**
  - Agentic plan codes must be added to the relevant `Promo.plans[]` before anything is visible.
  - Nothing has been exercised against the PayPal sandbox — see the test checklist in §9.
  - Credit-balance widget (needs a host screen to live in) and admin catalog UI (§7).
  - `ParseUtil.parseBigDecimal` reference-comparison bug in Tilda (Open Question 8).
- ✅ §10 (addendum) **model + Java authored** for the promo signup credit bonus (`Promo.initialCredits`):
  `_tilda.Wanda.json` updated (`Promo.initialCredits`, `UserPlanSubscription.creditsBonusGranted`,
  `UserPlanCreditLedger.type` value `BONUS`); `CreditHelper.grantSignupBonusIfEligible`,
  `UserPlanSubscription_Factory.incrementCreditsBonusGranted`, `PlanHelper.getUserPromo`, and the
  `PaymentOrderCapture` wiring are all written against the **not-yet-generated** accessors, same as every
  other change in this doc — ⬜ **still needs Tilda codegen + DB migration** before it compiles/runs. Data
  payload seeded: `AGENTIC_INTERNET_USER_SELF.initialCredits = 500` ($5.00).

### Decisions locked
| # | Decision | Ref |
|---|---|---|
| D1 | Credits are **abstract units**, not money. Convention: 1 credit = 1 US cent | §3.3(b) |
| D2 | Product grouping key is **`paymentSystemProductId`** (existing column, promoted to NOT NULL), not a new `product` column | §2.1 |
| D3 | Credit balances **accumulate across tiers** in a single per-product wallet; no per-purchase lots | §3.3(a) |
| D4 | One active subscription/billing/pre-order per user **per product** | §2.3 |
| D5 | A promo signup credit bonus is **first-purchase-only per (user, product) wallet**, guarded by a column on the wallet itself, not a global per-user flag | §10.1 |

---

## 0. Why this instead of the 4-phase rewrite

The 4-phase plan is a **hard cutover**: it deletes `Plan`, `PlanPricing`, `UserPlanPreOrder`,
`UserPlanSubscription`, `UserPlanBilling` and replaces them with 8 new objects. Nothing compiles until
Phases 1–3 are all complete (14 servlets, 4 helpers, plus PayPal Vault integration that its own doc
says must be re-verified against the live API in sandbox first). Add the 9 unresolved open items
across those docs and it is a multi-week effort.

The existing stack already does more than the docs give it credit for:

| Capability | Already works today |
|---|---|
| Multi-currency **native** pricing (USD/EUR/INR), no conversion | ✅ `PlanPricing`, one row per plan × currency |
| Multi-tier catalog | ✅ `Plan.pos` / `Plan.code`, seeded from the promos data payload |
| Catalog as data, not code | ✅ Tilda JSON ↔ Java ↔ DB symmetry: `RootImporter` → `Plan_Data`/`PlanPricing_Data` |
| Interactive PayPal create → approve → capture | ✅ `PaymentOrderCreate` / `PaymentOrderCapture` |
| Access lapses at end of cycle | ✅ `UserPlanBilling.expiryDt` + `UserBillingView.active` formula |
| Discounts / promo-gated plans | ✅ `Promo.plans[]`, `discountPct`, `discountMonths`, `discountYearPct` |
| Billing history | ✅ `UserPlanStatus` returns `billingHistory` (last 24) + `billingCurrent` |

Nothing auto-renews today, so "cancellation" costs nothing to support: a plan simply expires.

Because the Tilda model is the single source of truth across JSON, Java and SQL, every schema change
below propagates to the data payload format and the generated accessors automatically — that symmetry
is what makes this plan cheap.

---

## 1. Bug fixes (do these first — they are live defects)

### 1.1 Inverted expiry test in `PaymentOrderCapture.checkBilling`
`PaymentOrderCapture.java` ~L140-148:

```java
LocalDate today = DateTimeUtil.nowLocalDate();
if (UPB.getExpiryDt().isAfter(today) == true) // is the current billing still active?
  {
    // If no, the billing has expired
    UPB.setActive(false);
    ...
    return null;
  }
return UPB;
```

The condition is backwards relative to its own comment. A billing whose `expiryDt` is **in the future**
(i.e. still valid) is deactivated and `null` is returned → a *second* charge is created for a user who
already paid. Conversely an **expired** billing is returned as still-good → the capture block is
skipped entirely and the user keeps access without being charged.

### 1.2 `checkBilling` blocks legitimate repeat purchases (blocker for credit top-ups)
When `checkBilling` returns a non-null `UPB`, the whole create-billing + `PayPalHelper.captureOrder`
block (L75–114) is **skipped**, yet the servlet still responds `completed: true` with the *old* order's
`orderId`. The user approved a payment in the PayPal UI that we then never capture. For subscriptions
that's an edge case; for a credit product where **re-buying is the normal flow**, it is every purchase.

### 1.3 Fix for both: make capture idempotent on `orderId`
`UserPlanBilling` already has an `Order` index on `orderId`. Replace `checkBilling(C, U, UPS)` with:

1. `UserPlanBilling_Factory.lookupByOrder(orderId)` — if it exists **and** `isStatusPaid()`, return that
   record's response payload unchanged (true idempotency; protects against double-submit and PayPal
   retries).
2. Otherwise deactivate any currently-active billing for the user **for that product** (see §2.3),
   because the partial index allows only one active row per user per product.
3. Then always create the new billing row, capture, and set `active = true` only on `PAID`.

This removes the buggy date comparison entirely, makes repeat purchases work, and prevents
double-charging on retry.

### 1.4 Pre-order deleted before the capture succeeds
`PaymentOrderCapture.java` L70: `UserPlanPreOrder_Factory.delete(...)` runs *before*
`PayPalHelper.captureOrder(...)`. If the capture throws (network blip, PayPal 5xx), the pre-order is
gone and the user is stranded mid-payment — and if PayPal did take the money we have no local record.
**Fix:** move the delete to after a successful capture and successful billing write.

### 1.5 Capture-failure path has no durable record
L108–113: if the final `UPB.write(C)` fails, the only trace is a log line and an email to sysadmin.
**Minimal fix (no new table):** before emailing, write `ppod.toJsonString()` into `UPB.setMessage(...)`
truncated to 4096, and log at `error` with `orderId` + `customId` so the capture can be reconciled by
hand from PayPal's dashboard. The proper fix is the `PaymentEvent` table — deliberately deferred.

### 1.6 `Plan_Factory._PLANS` static cache is never cleared
`Plan_Factory.init(C)` appends to the static `_PLANS` list without clearing it, so calling `init` twice
duplicates every plan in the catalog. **Fix:** `_PLANS.clear()` at the top of `init`. Note also that
because this is a startup-time cache, editing the promos data payload requires an app restart to take
effect — worth remembering when changing prices under time pressure.

### 1.7 `PlanHelper.getAvailablePlans` returns `null` for logged-in users with no promo code
L137–147: if `U != null` and `U.isNullPromoCode() == true`, the method falls through to `return null` —
the user sees no plans at all, and `needsPlan` then returns `false`, silently letting them through
without a plan. Confirm whether this is intentional; if not, fall back to `Plan_Factory.getPlans()`.
Also note the redundant duplicate call to `getAvailablePlans` at `PaymentOrderCreate.java` L62–63.

---

## 2. Multi-product support

### 2.1 The grouping key: `paymentSystemProductId` (D2)

Reusing the existing `paymentSystemProductId` as the product grouping key, rather than introducing a
new `product` column. Rationale, after verifying the current code:

- **Nothing in the application reads it.** `PayPalHelper.createOrder` posts only `intent`, `custom_id`,
  `amount` and `application_context` — no product or plan reference. `paymentSystemProductId`,
  `paymentSystemProductName`, `paymentSystemMonthlyPlanId` and `paymentSystemYearlyPlanId` have **zero
  references** anywhere outside the generated Tilda classes. They are vestigial metadata from the
  Subscriptions/Billing-Plans design that `payment_infra_plan.3.md` §5 explicitly rejected.
- **The values are merchant-authored, not provider-generated.** `CAPSICO-GENAILEARNING-PERSONAL-01` is
  our own naming convention (contrast the PayPal-generated plan ids like `P-1JT30264KG444845MNDSEEEA`).
  It is therefore stable and under our control — which was the main objection to using it as a key.
- An id is stabler than a display name, and `paymentSystemProductName` may surface in the UI.

**Changes to `Plan`:**

| Column | Change | Notes |
|---|---|---|
| `paymentSystemProductId` | nullable → **not null**; consider narrowing STRING(255) → STRING(100) | Now the product grouping key. Gets denormalized into 4 tables and 4 indices, so the narrower the better |
| `paymentSystemProductName` | unchanged (stays nullable) | Display only. Never a key |
| `planType` | **new**, CHAR, not null, default `S` | `S` = Subscription (monthly/yearly), `C` = Credits (one-time pre-paid), `X` = Contact-us / not purchasable |

New index `Product`, **non-unique** (see §2.5 — it needs an `orderBy` to be non-unique):

```json
{ "name":"Product", "columns":["paymentSystemProductId"], "orderBy":["pos asc"], "db": true }
```

**Two data-hygiene actions to take now, while nothing references these values:**

1. **`ENTERPRISE_TIER` gets a product id.** It's the same product ("contact us" tier of GenAILearning),
   so it takes the same id as the other two. Give it `planType = X` so the front end can branch on the
   type explicitly instead of inferring "not purchasable" from an absent `pricings` array.
2. **Keep the existing product id value as-is.** `CAPSICO-GENAILEARNING-PERSONAL-01` is a poor name — it
   says *PERSONAL* but is shared by `INDIVIDUAL_TIER` and `PROFESSIONAL_TIER` — but it is **not being
   renamed**. Although no application code reads it today, a PayPal catalog product may exist under this
   exact id, and `payment_infra_plan.3.md` leaves the door open to adopting the Subscriptions API later.
   Renaming would silently desync our key from the provider's registered product with no compile-time
   error to catch it. An accurate-but-ugly id beats a pretty one that drifts.
   **Accepted consequence:** `ENTERPRISE_TIER` takes the same value, so the "contact us" tier carries an
   id that says `PERSONAL`. Cosmetic only — it is a key, not a label.

**Migration note:** `paymentSystemProductId` becomes NOT NULL, so the migration needs a backfill for
`ENTERPRISE_TIER` and the rename of the other two.

**Multi-provider escape hatch (not needed now):** if a second payment provider is ever added, this
column is best understood as "our product id, which we also register with the payment system". A
provider-specific mapping would then be added alongside it, leaving the key column — and all the
history rows carrying it — untouched.

### 2.2 New cycle value
`UserPlanPreOrder.cycle` and `UserPlanSubscription.cycle` currently allow `M` / `Y`. Add:

```json
{"name":"OneTime", "value":"O", "description":"One-time pre-paid purchase" }
```

Yields `isCycleOneTime()` / `_cycleOneTime` on both generated classes.

**No change needed** to `UserPlanSubscription_Data.getExpiryDtFrom(...)`: it already returns `null` for
any non-M/Y cycle. And `UserBillingView.active` is defined as
`... AND (expiryDt is null OR expiryDt >= today)`, so a credit billing with a null `expiryDt` reads as
**active indefinitely** — correct, since a credit plan ends when credits run out, not on a date.

### 2.3 Denormalizing the product key onto the user-side objects (D4)
A partial unique index on `(userRefnum, paymentSystemProductId)` requires the key to live **on the
indexed object**, so it must be denormalized from `Plan` onto all three user-side tables via
`sameAs: Plan.paymentSystemProductId`. It is immutable per row (a subscription never changes product),
so there is no update-anomaly risk.

| Object | Change |
|---|---|
| `UserPlanPreOrder` | + `paymentSystemProductId` (not null); index `User` → `["userRefnum","paymentSystemProductId"]` → one in-flight order per user **per product** |
| `UserPlanSubscription` | + `paymentSystemProductId` (not null); `UserActivePlan` → `["userRefnum","paymentSystemProductId"]` with `subWhere: active = true` |
| `UserPlanBilling` | + `paymentSystemProductId` (not null); `UserActive` → `["userRefnum","paymentSystemProductId"]` with `subWhere: active = true` |

**This is the change that lets a user hold a GenAILearning subscription and an Agentic credit wallet at
the same time**, and it is also what makes the top-up flow in §3 possible.

Generated-code breakage to expect after regen (this is the work list):
`UserPlanPreOrder_Factory.lookupByUser(...)`, `UserPlanPreOrder_Factory.delete(...)`,
`UserPlanSubscription_Factory.lookupByUserActivePlan(...)` and
`UserPlanBilling_Factory.lookupByUserActive(...)` all gain a product-id argument. Call sites:
`PaymentOrderCreate` (×3), `PaymentOrderCapture` (×4), `PlanHelper.needsPlan` (×1).

### 2.5 Tilda index semantics — verified, and one trap in the existing model

From `Tilda/src/tilda/parsing/parts/Index.java` L95:

```java
_Unique = _OrderBy == null || _OrderBy.length == 0;
```

**An index is UNIQUE if and only if it declares no `orderBy`.** There is no explicit `unique`
property. Consequences for this plan:

- The three partial indices in §2.3 (`UserActivePlan`, `UserActive`, `UserPlanPreOrder.User`) have no
  `orderBy`, so they are **unique partial indices** — exactly the intended "one active row per user per
  product" constraint, and they keep generating single-object `lookupByXXX` methods.
- `UserPlanBilling.Order` on `["orderId"]` has no `orderBy` → unique → `lookupByOrder` returns a single
  object. This confirms the §1.3 idempotency approach works as written.
- Any index intended as a plain non-unique lookup **must** declare an `orderBy` — hence the `Product`
  index in §2.1 is defined with `"orderBy":["pos asc"]`.
- `nullsNotDistinct` (default false) is available if a unique index over nullable columns should treat
  NULLs as equal. Not needed here once `paymentSystemProductId` is NOT NULL.

**⚠ Trap: `Plan.Position` is currently a UNIQUE index.**

```json
{ "name":"Position", "columns":["pos"], "db": true }
```

No `orderBy` → unique → **`Plan.pos` must be globally unique across every product**. That is survivable
today (1000 / 2000 / 3000) but becomes a landmine the moment a second product's tiers are added, and
the failure mode is an opaque unique-constraint violation at import time. Two options:

- **(a) Recommended:** make it non-unique by giving it an order — `{"name":"Position",
  "columns":["paymentSystemProductId"], "orderBy":["pos asc"], "db":true}` — at which point it *is* the
  new `Product` index and only one index is needed.
- **(b) Minimal:** leave it unique and allocate a disjoint `pos` range per product (Agentic uses
  100/200/300 in §4.2, which does not collide with 1000/2000/3000).

The payload in §4.2 is written to be safe under either choice.

### 2.6 Column `values` and the enum-with-default pattern
`planType` follows the existing convention already used by `UserPlanBilling.status` and
`UserPlanSubscription.cycle`:

```json
,{ "name":"planType", "type":"CHAR", "nullable": false, "description":"The nature of this plan"
  ,"values": [ {"name":"Subscription", "value":"S", "description":"Recurring subscription (monthly/yearly)", "default":"CREATE" }
              ,{"name":"Credits"     , "value":"C", "description":"One-time pre-paid credit pack" }
              ,{"name":"ContactUs"   , "value":"X", "description":"Not purchasable online" }
             ]
 }
```

`"default":"CREATE"` supplies the value at row-creation time, and the generated class gains
`isPlanTypeSubscription()` / `setPlanTypeCredits()` / `_planTypeCredits` style accessors — the same
shape `isCycleMonthly()` / `_statusPaid` already have.

### 2.4 `Plan_Factory` and servlets
- `Plan_Factory`: add `getPlans(String productId)` and
  `getPlans(String productId, String[] planCodes, short discountPct, ...)` overloads, plus the
  `_PLANS.clear()` fix from §1.6.
- `UserPlanList` (`/svc/user/plan/list`) and `UserPlanStatus` (`/svc/user/plan/status`): accept an
  optional `productId` request param and filter. Absent = all products, so no existing caller breaks.
- `PaymentOrderCreate`: derive the product id from the selected plan; no new required param. Reject
  orders for `planType = X` plans.
- `Promo.plans[]` still governs *who* sees *which* plans, unchanged. A user who should see both products
  lists the plan codes of both.

---

## 3. Pre-paid / credit plans

### 3.1 Schema — `PlanPricing`

| New column | Type | Notes |
|---|---|---|
| `oneTime` | NUMERIC, nullable | One-time price in the row's currency, used *instead of* `monthly`/`yearly` |
| `oneTimeCredits` | NUMERIC, nullable | **Credit units** granted by this purchase (D1) |
| `oneTimeDesc` | STRING(255), nullable | Display blurb, mirrors `yearlyDesc` |

**Required migration:** `monthly`, `yearly` and `yearlyDesc` are currently `"nullable": false`. A
credit-plan pricing row defines only `oneTime`, so all three must become nullable. Dropping NOT NULL is
a safe migration; Java must then null-check before use (`PlanHelper.SelectedPlan.getBillingPrice`,
front-end rendering).

### 3.2 Schema — the wallet lives on `UserPlanSubscription`

| New column | Type | Notes |
|---|---|---|
| `creditsBalance` | NUMERIC, nullable | Current remaining credit units. Null for subscription plans |
| `creditsPurchased` | NUMERIC, nullable | Lifetime credits granted on this subscription (reporting) |

With the `(userRefnum, paymentSystemProductId)` index from §2.3, the single active
`UserPlanSubscription` row for the Agentic product **is** the user's credit wallet. Nothing else is
needed to make top-ups accumulate.

### 3.3 How top-up works

#### (a) Wallet, not lots (D3)

A credit construct with `initialAmount`/`currentAmount` **per purchase** would be *lot tracking*: each
purchase is a bucket that depletes, and consumption must pick which bucket to drain (normally FIFO).
Lot tracking is only needed when credits expire per purchase, or refunds must claw back a specific
purchase's unused remainder, or finance needs per-purchase deferred-revenue recognition. None are in
scope.

So: **one running balance per user per product** (§3.2), with per-purchase history from the ledger
(§3.4) rather than mutable lot rows. `initialAmount` becomes `creditsPurchased` (lifetime granted,
monotonic); `currentAmount` becomes `creditsBalance`.

Worked example — a $20-equivalent tier, 1800 credits consumed, then a top-up with the $50 tier:

| Step | `creditsPurchased` | `creditsBalance` |
|---|---|---|
| Buys the 2000-credit tier | 2000 | 2000 |
| Consumes 1800 | 2000 | 200 |
| Tops up with the 5000-credit tier | 7000 | **5200** |

The result falls out of `balance += grant` on a wallet keyed by product, not by plan. This is *why*
§2.3's index change matters: without it, the second purchase would end the first subscription and
strand the remaining 200 credits.

`UserPlanSubscription.planRefnum` keeps pointing at the **most recently purchased** tier (informational
only — per-purchase truth lives in `UserPlanBilling`). And `isStillValid(...)` in `PaymentOrderCapture`
must be relaxed for `planType = C` to compare **product id** rather than `planRefnum`, so a different
tier of the same product reuses the wallet instead of replacing it.

If per-lot expiry is ever needed, the ledger holds every grant with its date, so lots can be
reconstructed retroactively — this decision is reversible.

#### (b) Credits are units, not money (D1)

If the balance were denominated in currency, a user who buys the $10 USD pack and later the ₹2000 INR
pack would have a wallet where `10 + 2000` is meaningless; each wallet would have to be pinned to the
currency of its first purchase, and every metered operation would need a price *per currency* — a
second pricing matrix to keep in sync with the first.

As abstract units, the pack price stays native per market (the original design intent, untouched), the
credits granted are identical everywhere, and metering is priced once with no currency awareness.

Convention: **1 credit = 1 US cent**, so USD-market UI can display "$52.00" for a 5200-credit balance
by dividing by 100. Non-USD markets display credits, or a native-currency equivalent derived from that
market's own pack pricing — never a computed FX rate.

### 3.4 Schema — one new object: `UserPlanCreditLedger` (append-only)
The only new table in this plan. Makes the balance auditable and rebuildable if `creditsBalance` drifts.

| Column | Type | Notes |
|---|---|---|
| `userRefnum` | FK → User | |
| `subscriptionRefnum` | FK → UserPlanSubscription | |
| `billingRefnum` | FK → UserPlanBilling, nullable | Set for `GRANT` rows — links a grant to the purchase that paid for it |
| `paymentSystemProductId` | `sameAs Plan.paymentSystemProductId`, not null | Denormalized for direct product-scoped queries |
| `type` | STRING(10) | `GRANT` (purchase), `USE` (consumption), `ADJ` (manual/admin) |
| `amount` | NUMERIC | Positive for GRANT, negative for USE |
| `balanceAfter` | NUMERIC | Snapshot after applying this row |
| `reference` | STRING(256), nullable | What consumed it (job id, feature code, …) |
| `notes` | STRING(1024), nullable | |

Indices: `Subscription` on `["subscriptionRefnum"]` ordered `created desc`; `User` on
`["userRefnum","paymentSystemProductId"]` ordered `created desc`.

Deliberately **no** balance-cache table (Phase 1 §2.5 of the rewrite) — the denormalized column on the
subscription plus this ledger is enough at current volume, and the balance is recomputable with a
single `SUM(amount)`.

> **Known limitation, accepted:** balance updates are read-modify-write inside the servlet's
> transaction, not an atomic `UPDATE ... SET creditsBalance = creditsBalance - ?`. Two truly concurrent
> consume calls for the same user could lose an update. Acceptable for launch; revisit if metering goes
> high-frequency or multi-threaded per user.

### 3.5 New helper — `wanda.servlets.helpers.CreditHelper`
Single code path for all balance mutations so nothing drifts:

- `BigDecimal getBalance(Connection C, User_Data U, String productId)` — reads the active credit
  subscription for the product; returns `ZERO` if none.
- `void grant(Connection C, UserPlanSubscription_Data UPS, UserPlanBilling_Data UPB, BigDecimal credits)`
  — ledger `GRANT` + bump `creditsBalance` / `creditsPurchased`. Called from `PaymentOrderCapture` on `PAID`.
- `boolean consume(Connection C, User_Data U, String productId, BigDecimal credits, String reference)`
  — ledger `USE` + decrement; returns `false` and writes nothing if the balance is insufficient.
- `void adjust(...)` — admin correction, ledger `ADJ`.

### 3.6 Changed behaviour in `PaymentOrderCapture`
After the §1.3 rework: if the plan is `planType = C`, then on `isStatusPaid()` call
`CreditHelper.grant(...)` with the pricing row's `oneTimeCredits`, and leave `UPB.expiryDt` null. Plus
the product-scoped `isStillValid` relaxation from §3.3(a).

### 3.7 New servlets

| Endpoint | Purpose |
|---|---|
| `GET /svc/user/credits/balance?productId=` | Balance for the credit meter widget |
| `POST /svc/user/credits/consume` | Internal/service-to-service. Params `productId`, `credits`, `reference`. Returns new balance + `sufficient` flag. **Must be role-gated or internal-only** — it mutates money-equivalent state |
| `GET /svc/user/credits/history?productId=` | Paged `UserPlanCreditLedger` read for a usage statement |

Also extend `UserPlanStatus` to include `creditsBalance`, so one call feeds the whole dashboard.

### 3.8 Gating
`PlanHelper.needsPlan` currently returns true when a user has available plans but no active billing.
Keep credit-exhaustion **out** of `needsPlan` (which drives the login-time plan picker) — otherwise a
zero balance becomes a login blocker. Instead each metered feature gates itself via the app contract in
§3.9, and surfaces a top-up prompt rather than an error.

### 3.9 App integration contract (metered operations)

Metering is **app-driven**, not framework-intercepted. Wanda supplies the wallet, the gate and the
top-up UI; the app decides what an operation costs and when to charge. Nothing here uses an HTTP
status code: an "out of credits" response is a perfectly valid, usually *successful* request, so the
signal travels inside the app's own response payload as an application-level code.

**Why the balance is allowed to go negative.** The cost of an agentic operation generally isn't known
until it finishes. Refusing to record an overrun would mean giving the work away and losing the audit
trail. So the rule is: *you may start if you are not in the red; the last operation may take you into
the red; you settle up before the next one.*

#### Server side — the app's service

```java
// 1. ENTRY GATE: refuse before doing any work at all.
CreditStatus status = CreditHelper.check(C, U, PRODUCT_ID);
if (status.isOK() == false)
  {
    JSONPrinter j = new JSONPrinter();
    status.toJSON(j);          // emits "creditStatus" incl. code=INSUFFICIENT_CREDITS
    res.successJson(j);        // a normal 200: the request was valid, there is just no balance
    return;
  }

// 2. Do the actual work. The cost only becomes known here.
MyResults results = doTheExpensiveAgenticThing(...);

// 3. EXIT CHARGE: always recorded, even if it overdraws.
status = CreditHelper.charge(C, U, PRODUCT_ID, results.getCreditCost(), "job:" + results.getId());

// 4. Return the results AND the status. When isOK() is false the client shows the results, then
//    prompts for a top-up. It must NOT retry: the work was done and has been paid for.
JSONPrinter j = new JSONPrinter();
j.addElement("results", results, "");
status.toJSON(j);
res.successJson(j);
```

`CreditStatus.toJSON` emits a uniform block so one piece of client code handles every app:

```json
"creditStatus": { "productId":"CAPSICO-AGENTIC-01", "code":"INSUFFICIENT_CREDITS"
                , "ok":false, "hasWallet":true, "balance":-320 }
```

`code` is `null` whenever nothing is needed, so the client test is simply
`if (data.creditStatus?.code != null)`.

#### Client side — the app's front-end

```javascript
FloriaAjax.ajaxUrl(myAppUrl, "POST", null, function(data) {
    if (data.results != null)          // Always render what we got: the work was done and paid for.
     renderResults(data.results);

    if (data.creditStatus?.code != null)
     FloriaLogin.PopupLogin.topUpCredits(data.creditStatus.productId, function() {
        // Optional: resume. Without a callback the page reloads, which would discard the results
        // that were just rendered above.
        refreshCreditMeter();
     });
  });
```

`topUpCredits(productId, onComplete)` reuses the entire existing stack — `/svc/user/plan/status`
filtered to that one product, `paintPlans`, `pricingSelect`, the PayPal buttons and
`/svc/payments/order/*`. It adds a balance banner at the top and, when given an `onComplete`, closes
the dialog and calls back instead of reloading the page.

#### Which method to call

| Situation | Method | Behavior |
|---|---|---|
| Before starting work | `CreditHelper.check` | Read-only. `isOK()` false ⇒ return the code, do nothing |
| After work, cost now known | `CreditHelper.charge` | **Always** debits, may go negative, returns the new status |
| Cost known *up front* and must be pre-authorized | `CreditHelper.consume` | Strict: refuses and writes nothing if the balance won't cover it |

`isOK()` is false when the user has **no wallet** (never bought a pack — send them to the packs) or
the **balance is negative**. A zero balance is deliberately OK: that is the headroom that lets a
post-hoc charge land.

---

## 4. Data payload changes

### 4.1 Existing GenAILearning plans
- `INDIVIDUAL_TIER`, `PROFESSIONAL_TIER`: unchanged (`paymentSystemProductId` stays
  `"CAPSICO-GENAILEARNING-PERSONAL-01"` per §2.1). Optionally add `"planType":"S"` (the default).
- `ENTERPRISE_TIER`: add `"paymentSystemProductName":"CapsicoHealth GenAILearning"`,
  `"paymentSystemProductId":"CAPSICO-GENAILEARNING-PERSONAL-01"`, `"planType":"X"`.

### 4.2 New "CapsicoHealth Agentic" product

Pricing per your direction: **$10 / $25 / $50**, EUR at 1:1, INR at 1:80 — placeholder conversions to
be replaced with real native market prices before launch. Credits at 1 credit = 1 US cent (D1),
strictly proportional across tiers for now (no volume bonus — adding one is a single-field edit).

| Tier | USD | EUR | INR | Credits |
|---|---|---|---|---|
| Starter | $10 | €10 | ₹800 | 1000 |
| Standard | $25 | €25 | ₹2000 | 2500 |
| Pro | $50 | €50 | ₹4000 | 5000 |

Added to the same `plans` array in
`/CapsicoWebDynamic/src/data_payloads/_tilda.Wanda.initdata.promos.json`:

```jsonc
,{ "plan": { "code":"AGENTIC_CREDITS_10", "planType":"C"
            ,"paymentSystemProductName":"CapsicoHealth Agentic"
            ,"paymentSystemProductId":"CAPSICO-AGENTIC-01"
            ,"start":"2026-08-01", "active":true, "pos":100
            ,"label":"Starter", "descr":["1,000 credits.", "Credits never expire.", "Top up any time."]
           }
  ,"pricings": [
      { "currency":"USD", "oneTime":  10, "oneTimeCredits":1000, "oneTimeDesc":"Best for trying things out." }
     ,{ "currency":"EUR", "oneTime":  10, "oneTimeCredits":1000, "oneTimeDesc":"Best for trying things out." }
     ,{ "currency":"INR", "oneTime": 800, "oneTimeCredits":1000, "oneTimeDesc":"Best for trying things out." }
    ]
 }
,{ "plan": { "code":"AGENTIC_CREDITS_25", "planType":"C"
            ,"paymentSystemProductName":"CapsicoHealth Agentic"
            ,"paymentSystemProductId":"CAPSICO-AGENTIC-01"
            ,"start":"2026-08-01", "active":true, "pos":200
            ,"label":"Standard", "descr":["2,500 credits.", "Credits never expire.", "Top up any time."]
           }
  ,"pricings": [
      { "currency":"USD", "oneTime":  25, "oneTimeCredits":2500 }
     ,{ "currency":"EUR", "oneTime":  25, "oneTimeCredits":2500 }
     ,{ "currency":"INR", "oneTime":2000, "oneTimeCredits":2500 }
    ]
 }
,{ "plan": { "code":"AGENTIC_CREDITS_50", "planType":"C"
            ,"paymentSystemProductName":"CapsicoHealth Agentic"
            ,"paymentSystemProductId":"CAPSICO-AGENTIC-01"
            ,"start":"2026-08-01", "active":true, "pos":300
            ,"label":"Pro", "descr":["5,000 credits.", "Credits never expire.", "Top up any time."]
           }
  ,"pricings": [
      { "currency":"USD", "oneTime":  50, "oneTimeCredits":5000 }
     ,{ "currency":"EUR", "oneTime":  50, "oneTimeCredits":5000 }
     ,{ "currency":"INR", "oneTime":4000, "oneTimeCredits":5000 }
    ]
 }
```

`monthly` / `yearly` / `yearlyDesc` are simply **absent** — which is why the NOT NULL constraints in
§3.1 must be dropped. Credit plans must also be listed in the relevant `Promo.plans[]` arrays.

> **Do not apply this payload before the codegen + migration.** Gson silently ignores unknown keys, so
> `oneTime` / `oneTimeCredits` would be dropped and the rows would then fail the current NOT NULL
> constraints on `monthly`/`yearly`.

---

## 5. Change list

### 5.1 `_tilda.Wanda.json` (all model changes land first)
| Object | Change |
|---|---|
| `Plan` | `paymentSystemProductId` → not null (+ optional narrow to STRING(100)); + `planType` (CHAR, default `S`, values `S`/`C`/`X`); + non-unique index `Product` on `["paymentSystemProductId"]` ordered `pos asc`; **resolve the unique `Position` index per §2.5** |
| `PlanPricing` | + `oneTime`, `oneTimeCredits`, `oneTimeDesc` (all nullable); **drop NOT NULL** on `monthly`, `yearly`, `yearlyDesc` |
| `UserPlanPreOrder` | + `paymentSystemProductId`; cycle value `O`; index `User` → `["userRefnum","paymentSystemProductId"]` |
| `UserPlanSubscription` | + `paymentSystemProductId`, `creditsBalance`, `creditsPurchased`; cycle value `O`; index `UserActivePlan` → `["userRefnum","paymentSystemProductId"]` |
| `UserPlanBilling` | + `paymentSystemProductId`; index `UserActive` → `["userRefnum","paymentSystemProductId"]` |
| `UserPlanCreditLedger` | **new object** per §3.4 |
| `UserBillingView` | + expose `paymentSystemProductId`, `planType` and `creditsBalance` |
| *migrations* | `notNulls` backfills for `Plan.paymentSystemProductId`, `Plan.planType`, and the three new denormalized `paymentSystemProductId` columns — all defaulting to `CAPSICO-GENAILEARNING-PERSONAL-01` / `S`, since every existing row is GenAILearning |

### 5.2 Java (after codegen + migration)
| File | Change | Size |
|---|---|---|
| `PaymentOrderCapture.java` | §1.3 idempotent-on-orderId rework, §1.4 delete ordering, §1.5 failure logging, §3.6 credit grant + product-scoped `isStillValid`, product-scoped factory lookups | ~70 lines |
| `PaymentOrderCreate.java` | Remove duplicate `getAvailablePlans` (L62); allow cycle `O`; reject `planType = X`; pass product id to pre-order lookup/create | ~25 lines |
| `PlanHelper.java` | `SelectedPlan.getBillingPrice()` returns `oneTime` for cycle `O`; add `getCredits()`; `getPlanPrice` validates the pricing row defines the requested cycle; §1.7 fallback; product-scoped `needsPlan` | ~40 lines |
| `Plan_Factory.java` | `_PLANS.clear()`; `getPlans(productId, ...)` overloads | ~25 lines |
| `CreditHelper.java` (new) | grant / consume / adjust / getBalance | ~150 lines |
| `UserCreditsBalance/Consume/History.java` (new) | Thin `SimpleServlet`s per §3.7 | ~60 lines each |
| `UserPlanStatus.java` / `UserPlanList.java` | Optional `productId` filter; add `creditsBalance` | ~25 lines |

**Front-end (FloriaJS / CapsicoWebDynamic):** the plan-picker needs a `planType` branch — `S` renders
the monthly/yearly toggle, `C` renders a single one-time price + credit count and posts `cycle=O` to
`/svc/payments/order/create`, `X` renders the "contact us" card. Plus a credit-balance indicator fed by
`/svc/user/credits/balance`. *(Exact file not yet confirmed — locate the caller of
`/svc/payments/order/create` in the FloriaJS `module-*.js` set before estimating.)*

---

## 6. Suggested order of work

1. **Bug fixes §1.1, 1.4, 1.5, 1.6** — pure Java, no model change, independently shippable. Half a day.
2. **Model changes §5.1** in one pass → codegen → migration. The compile errors from the regenerated
   index methods are the §2/§3 Java work list.
3. **Multi-product §2.4 + §1.3 capture rework.** Half a day.
4. **Credit plans §3.5–3.8** + payload §4. One to two days.
5. **Front-end.** One day, parallelizable with 4.

---

## 7. What this consciously leaves on the table

Unaffected by the above — the schema here is additive, so an eventual migration to
`Service`/`ServicePricing` remains possible (and easier as a dual-write than the hard cutover currently
specified in the 4-phase docs):

- Automatic recurring billing (needs PayPal Vault + scheduler).
- Overage billing and auto-refill.
- More than one active subscription **per product** per user (still one, by design).
- `PaymentEvent` immutable audit trail and admin monitoring dashboard.
- Admin catalog CRUD (catalog stays file-seeded, requiring an app restart per §1.6).

---

## 8. Open questions

Items 3, 5, 6 and 7 are now **resolved** and are kept only as a record of the decision.

1. **Do credits expire?** Assumed no ("Credits never expire" in the payload copy). If they should, the
   wallet model needs per-lot tracking after all (§3.3(a)) plus a sweep — which pushes toward the
   scheduler this plan avoids.
2. **Real native prices for EUR/INR** — §4.2 uses the 1:1 and 1:80 placeholders; these need real market
   prices before launch, per the original native-pricing intent.
3. ~~Who may call `/svc/user/credits/consume`?~~ **Resolved:** gated on `RoleHelper.ADMINROLES` and takes
   an explicit `userRefnum`. In-process app code should call `CreditHelper` directly and bypass it.
4. **Refunds.** Not modelled. If a credit purchase is refunded, does the balance get clawed back (and
   what if already spent)? Today `UserPlanBilling.status` has `RFND` but nothing acts on it. Note the
   ledger makes a clawback easy to post (`ADJ`), the question is purely the policy.
5. ~~Narrow `paymentSystemProductId` to STRING(100)?~~ **Resolved:** done.
6. ~~`planType = X` for `ENTERPRISE_TIER`~~ **Resolved:** done; the front end branches on the type.
7. ~~`Plan.Position` uniqueness~~ **Resolved:** `Product` is unique on `(paymentSystemProductId, pos)`,
   so `pos` is unique *within* a product rather than globally.
8. **`PlanHelper.getAvailablePlans` returns `null`** for a logged-in user with no promo code, so they
   see no plans and `needsPlan` waves them through with no plan at all. Intentional (free/internal
   users) or a hole? If a hole, fall back to `Plan_Factory.getPlans()`. **Left unchanged pending your
   call** — it is a product decision, not a bug I should decide unilaterally.
9. **`ParseUtil.parseBigDecimal` bug (Tilda).** `if (v == new BigDecimal(SystemValues.EVIL_VALUE) && Mandatory == true)`
   compares object references against a freshly allocated instance, so it is always false and a
   mandatory-but-invalid BigDecimal never raises an error. Needs `.compareTo(...) == 0`. Affects the
   new `getParamBigDecimal(name, true)`. Not fixed: it is in Tilda, not Wanda.

---

## 9. Test checklist (nothing below has been exercised yet)

None of this has run against the PayPal sandbox. In rough priority order:

**Payment flow**
1. Subscribe to a GenAILearning tier → capture → `UserPlanSubscription` + `UserPlanBilling` created,
   `expiryDt` set one cycle out, `billingCurrent` true.
2. Buy an Agentic pack → wallet created, `creditsBalance` = pack credits, ledger has one `GRANT`.
3. **Cross-tier top-up:** buy Starter, spend some, buy Pro → balance ACCUMULATES onto the same
   subscription row (the §3.3(a) behaviour; a regression here silently destroys credits).
4. **Both products at once:** hold a GenAILearning subscription and an Agentic wallet simultaneously —
   this is what the `(userRefnum, paymentSystemProductId)` indices exist for.
5. **Idempotent capture:** replay the same `orderId` → second call returns the recorded result and does
   NOT charge again.
6. **Failed capture:** previous billing must remain active (the deactivate-only-on-PAID ordering).

**Credits**
7. `check` on a user with no wallet → `isOK()` false, code returned, no work done.
8. `charge` beyond the balance → goes negative, ledger records it, next `check` blocks.
9. `topUpCredits(productId, onComplete)` → dialog shows balance banner, purchase completes, callback
   fires and the page does NOT reload.

**Regression**
10. An existing GenAILearning user still sees the right plans and billing history after migration.

---

## 10. Addendum: promo signup credit bonus (`Promo.initialCredits`)

**Request:** for a credit-based product (e.g. "CapsicoHealth Agentic"), a `Promo` should be able to say
"whoever signs up under this promo gets an extra $N of credits, once, on top of whichever pack they buy."
Concretely: `AGENTIC_INTERNET_USER_SELF` grants +500 credits ($5.00) the first time a user buys *any* of
the $10/$25/$50 Agentic packs — buy Starter and you actually land with 1500 credits in the wallet, not
1000; the bonus is **not** re-granted on a later top-up of the same wallet.

This slots into the existing credit-plan machinery (§3) exactly the way a purchase's own
`oneTimeCredits` does, with one new idea: the grant must fire **once per (user, product) wallet**, not
once per purchase.

### 10.1 Design decisions

- **The bonus lives on `Promo`, not on `Plan`/`PlanPricing`.** It is a property of *how a user signed up*
  ("this promo"), applicable across every tier of a product the promo lists in `plans[]` — not a property
  of any one pack. A user who buys Starter or Pro first gets the same bonus either way.
- **Units, not money (reuses D1).** `Promo.initialCredits` is a credit UNIT count, exactly like
  `PlanPricing.oneTimeCredits` (1 credit = 1 US cent). No new currency concept, no per-market pricing: the
  bonus is the same size everywhere a promo is used, by design (mirrors the Agentic packs themselves,
  which are only converted to native currency for *display/charge*, never for the credits granted).
- **First-purchase-only, scoped per wallet (new decision D5).** "First time" could have been read as
  "first time ever for this user" (a single global flag on `User`) or "first time for this product" (a
  flag on the wallet). The wallet-scoped reading was chosen because:
  - It reuses the existing `(userRefnum, paymentSystemProductId)` wallet identity from §2.3/§3.2 instead
    of introducing a second, overlapping notion of "has this user ever gotten a bonus" on `User`.
  - It naturally extends to a future second credit product: a user could get a signup bonus on Agentic
    AND, independently, on some other future credit product, if both promos define one — each wallet
    tracks its own.
  - It is the same idempotency shape already proven out for `creditsBalance`/`creditsPurchased`: a
    denormalized counter on the wallet row, updated atomically alongside the ledger write.
- **A new ledger `type` (`BONUS`), not `ADJ` or a second `GRANT`.** Reusing `GRANT` would make it
  indistinguishable from a paid purchase in the audit trail (`billingRefnum` would look like it paid for
  bonus credits, when the same `UserPlanBilling` row already accounts for the pack's own credits).
  Reusing `ADJ` would blur it with unrelated admin corrections. A dedicated `BONUS` type keeps the ledger
  self-describing without adding a new table.
- **`creditsBonusGranted` is a separate counter from `creditsPurchased`.** `creditsPurchased` keeps its
  existing meaning ("lifetime credits ever granted", already documented that way, so bonus credits
  legitimately count toward it for reporting). `creditsBonusGranted` exists purely so
  `grantSignupBonusIfEligible` has something cheap and reliable to test — "has this wallet's bonus already
  fired?" — without querying the ledger for a `BONUS` row every time.
- **Fires from the same `PaymentOrderCapture` code path as the pack's own grant, not a separate servlet.**
  There is no new user-facing action: the bonus is layered onto whichever purchase happens to be the
  user's first for that product. No UI change is needed either — the wallet balance the user already sees
  simply reflects pack + bonus after their first purchase.

### 10.2 Model changes (`_tilda.Wanda.json`) — done

| Object | Change |
|---|---|
| `Promo` | + `initialCredits` (NUMERIC, nullable): the one-time bonus, in credit units, granted per §10.1. |
| `UserPlanSubscription` | + `creditsBonusGranted` (NUMERIC, nullable): cumulative bonus credits granted on this wallet; the idempotency guard. |
| `UserPlanCreditLedger.type` | + value `Bonus` / `"BONUS"`. |

No new table, no new index, no change to `UserBillingView`. Existing NOT NULL/nullable shapes on every
touched column are unchanged elsewhere.

### 10.3 Java changes — written, pending Tilda codegen + migration

Same convention as the rest of this document: these are written against accessors Tilda has not generated
yet (`Promo_Data.getInitialCredits()`/`isNullInitialCredits()`,
`UserPlanSubscription_Data.getCreditsBonusGranted()`/`isNullCreditsBonusGranted()`,
`UserPlanCreditLedger_Data._typeBonus`, `UserPlanSubscription_Factory.COLS.CREDITSBONUSGRANTED`). They will
not compile until codegen + migration run; the resulting compiler errors are the confirmation that codegen
picked up the new columns, not a sign anything is wrong.

| File | Change |
|---|---|
| `UserPlanSubscription_Factory.java` | + `incrementCreditsBonusGranted(C, subscriptionRefnum, bonusDelta)`, a single-UPDATE atomic increment mirroring `incrementCreditsBalance`. |
| `CreditHelper.java` | + `grantSignupBonusIfEligible(C, UPS, UPB, bonusCredits)`: no-ops if `bonusCredits` is null/≤0, or if `UPS.creditsBonusGranted` is already positive; otherwise posts a `BONUS` ledger row (via the existing private `post()`), links it to `UPB`, and bumps `creditsBonusGranted`. |
| `PlanHelper.java` | + `getUserPromo(C, U)`, factored out of `getAvailablePlans` (which now calls it) so the bonus check and the plan-availability check agree on what "the user's promo" means. |
| `PaymentOrderCapture.java` | Inside the existing `isPlanTypeCredits()` branch, right after the pack's own `CreditHelper.grant(...)`: look up `PlanHelper.getUserPromo(C, U)` and, if it defines `initialCredits`, call `CreditHelper.grantSignupBonusIfEligible(...)`. Fires on every credit purchase for the product; the wallet-level guard makes it a no-op on top-ups. |

### 10.4 Data payload — done

`AGENTIC_INTERNET_USER_SELF` in
`/CapsicoWebDynamic/src/data_payloads/_tilda.Wanda.initdata.promos.json` now carries
`"initialCredits": 500` (= $5.00), matching the request's example. Front-end: no change required — the
existing balance/credit-status display already reflects whatever the wallet's `creditsBalance` is.

### 10.5 Not done / open questions

- ⬜ Tilda codegen + DB migration (same blocker as everything else in this document not yet exercised).
- ⬜ Test checklist addition once compiling: buy Starter under `AGENTIC_INTERNET_USER_SELF` → wallet =
  1000 + 500 = 1500; buy Pro next (top-up) → wallet = 1500 + 5000 = 6500, with only ONE `BONUS` ledger row
  ever, from the first purchase.
- **Refunds** interact with this the same way they do with the base grant (§8 item 4, still unmodeled): if
  the first purchase is refunded, the bonus is not automatically clawed back. The ledger makes a manual
  `ADJ` clawback straightforward if that policy is wanted later.
- **Promo change mid-life:** if a user's `promoCode` changes after their first credit purchase, the bonus
  from the *new* promo is still blocked by `creditsBonusGranted` already being positive on that product's
  wallet. This was judged correct (the bonus is per-wallet, not per-promo), but is worth confirming against
  intent if promo reassignment for existing paying users becomes a real workflow.
