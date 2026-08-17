# Payment Infrastructure Plan — Phase 2: Servlets & Scheduling Helper

**Status:** Draft for review — no implementation yet. Depends on Phase 1 schema.
**Target locations:**
- `Wanda/src/wanda/servlets/` (user-facing) and `Wanda/src/wanda/servlets/admin/` (catalog admin)
- `Wanda/src/wanda/servlets/helpers/` (`CatalogHelper`, `SubscriptionHelper`, `CreditLedgerHelper`,
  `SchedulingHelper`)

All servlets follow the existing conventions in this codebase: extend `SimpleServlet`, use
`RequestUtil`/`ResponseUtil`, `req.throwIfErrors()`, `Connection C` passed in, JSON responses via
`JSONPrinter`/`res.successJson(...)`, admin endpoints gated with
`throwIfUserInvalidRole(U, RoleHelper.ADMINROLES)` (see `PlanListServlet`, `PromoCreate`).

---

## 1. Catalog (read-only, public/authenticated browsing)

### `GET /svc/services/list`
- Lists active `Service` rows (via `UserServiceCatalogView`), each with its nested `ServicePricing`
  tiers, optionally filtered by `?currency=`.
- Mirrors the shape of the current `UserPlanList` but keyed off the new catalog view instead of
  `PlanHelper.getAvailablePlans`.
- No auth required beyond normal session (guest-allowed, matching current `UserPlanList` posture).

### `GET /svc/services/details?serviceCode=`
- Single-service detail, all tiers/currencies, for a "subscribe" button/modal on any app screen to
  fetch pricing before submitting an order.

---

## 2. User Subscription Lifecycle

### `POST /svc/services/subscribe`
Replaces `PaymentOrderCreate` for the "start a new subscription" case.
- **Params:** `serviceCode`, `tierCode`, `currency`, `cycle`, `paymentProvider`.
- Looks up `ServicePricing` matching the four selectors (via new `CatalogHelper.getPricing(...)`,
  the equivalent of today's `PlanHelper.getPlanPrice`).
- Creates a `PaymentOrder` (`orderType=NEW_SUBSCRIPTION`, `initiatedBy=USER`, `status=CRTD`) with a
  fresh `orderUUID` — **no more single-slot pre-order per user**; a user can have several of these
  in flight (e.g., subscribing to two different services back to back).
- Delegates the actual PayPal order creation to Phase 3's helper, but this servlet owns the
  `PaymentOrder` bookkeeping.
- Returns `orderUUID` + provider order id + approve link (client redirects/opens PayPal button).

### `POST /svc/services/subscribe/capture`
Replaces `PaymentOrderCapture` for the new-subscription case.
- **Params:** `orderUUID`, `paymentProvider`, `providerOrderId`.
- Loads `PaymentOrder` by `orderUUID` (not by user!), validates it matches the user/provider/id.
- Calls Phase 3's capture helper; on success:
  - Creates `UserServiceSubscription` (active, `nextChargeDt` = start + cycle length).
  - Vaults the payment method (`PaymentVaultToken`) if not already vaulted for this
    provider/user — this is what enables scheduler-driven renewals later.
  - Writes a `UserServiceCreditLedger` `GRANT` entry for `ServicePricing.includedCredits`, and
    updates `UserServiceCreditBalance`.
  - Marks `PaymentOrder.status = PAID`.
- On failure, mirrors current behavior: record `FAIL`/`VOID` status, log via `PaymentEvent`
  (Phase 1 table) instead of only emailing an admin — the email notification becomes a
  *secondary* alert, not the only record.

### `POST /svc/services/subscription/cancel`
New endpoint — **no equivalent exists today**.
- **Params:** `subscriptionRefnum`.
- Validates the subscription belongs to the requesting user and is active.
- Sets `cancelRequestedDt = now`, `effectiveEndDt` = end of current paid cycle (derived from
  `nextChargeDt` minus one cycle, or a stored `currentCycleEndDt` — to be finalized in schema
  review), leaves `active = true` and `nextChargeDt` untouched for now.
- Does **not** deactivate immediately — access remains until `effectiveEndDt`. No proration.
- The scheduler (`SchedulingHelper.expireCancelledSubscriptions`, see below) is responsible for
  flipping `active = false` once `effectiveEndDt` passes, and for skipping the next renewal charge.
- Returns confirmation with the effective end date for client display ("active until March 31").

### `POST /svc/services/subscription/change-tier`
New endpoint.
- **Params:** `subscriptionRefnum`, `newTierCode`/`newPricingRefnum`.
- Sets `pendingPricingRefnum`; the change takes effect at the next renewal (no proration), applied
  by `SchedulingHelper.applyPendingTierChanges`.

### `GET /svc/services/subscription/list`
Replaces `UserPlanList`/`UserPlanStatus`.
- Returns all of the user's subscriptions (active + historical) via `UserServiceSubscriptionView`,
  each including current tier, credit balance, next charge date, cancel/pending-change state.
- This is the primary feed for the future dashboard (Phase 4).

---

## 3. Credits: Top-Up & Metering

### `POST /svc/services/credits/topup`
- **Params:** `subscriptionRefnum`, `credits` (or `amount`, computed from `overageAmountPerCredit`).
- Creates a `PaymentOrder` (`orderType=TOP_UP`, `initiatedBy=USER`), same create/capture pattern as
  subscribe (two servlets: `.../topup` and `.../topup/capture`, or a single servlet reusing the
  generic order-create/capture helpers parameterized by `orderType` — **implementation detail to
  decide in Phase 3 based on how much PayPal flow can be shared**).
- On capture success: `UserServiceCreditLedger` `PURCHASE` entry, balance updated.

### `POST /svc/services/credits/consume`  *(internal/service-to-service API, not end-user facing)*
- Called by application code elsewhere (not part of this payment subsystem's UI) whenever a
  metered action happens.
- **Params:** `subscriptionRefnum`, `credits`, `reference`.
- Writes `UserServiceCreditLedger` `CONSUMPTION` entry (negative amount), updates
  `UserServiceCreditBalance.balance` and `cycleConsumed`.
- Checks `autoRefill`/`autoRefillThreshold`: if balance drops below threshold and auto-refill is
  enabled, creates a scheduler-flagged top-up (see `SchedulingHelper.processAutoRefills`) rather
  than charging synchronously inline with the consuming request (keeps metering calls fast and
  decoupled from payment processing).
- Checks `spendCapPerCycle`: if exceeded, raises an alert (email/notification — reuse
  `EMailSender` pattern already in the codebase) without blocking consumption unless product
  decision says otherwise (flag as an open item).

### `GET /svc/services/credits/balance?subscriptionRefnum=`
- Fast read from `UserServiceCreditBalance` for UI display (e.g., a credit meter widget usable
  from any application screen, per your "subscribe buttons from various screens" requirement).

### `GET /svc/services/credits/history?subscriptionRefnum=`
- Paged read of `UserServiceCreditLedger` for a user-facing usage history / statement view.

---

## 4. Admin Catalog Management

Modeled directly on `PlanListServlet` (list) and `PromoCreate` (create/update via
`Factory.init(params, errors)`), under `wanda.servlets.admin`:

### `GET /svc/admin/services/list`
All services (active + inactive) for admin catalog management.

### `POST /svc/admin/services/create` (create or update via optional `refnum` param)
Create/update a `Service` row. Uses `Service_Factory.init(params, errors)` pattern like
`PromoCreate`.

### `GET /svc/admin/services/pricing/list?serviceRefnum=`
All `ServicePricing` tiers/currencies for a service.

### `POST /svc/admin/services/pricing/create`
Create/update a `ServicePricing` row (tier/currency/cycle/baseline/includedCredits/overage rate).

### `GET /svc/admin/payments/orders?userRefnum=&status=&dateRange=`
Admin order-history browser over `PaymentOrder`/`PaymentOrderHistoryView`, for support/finance.

### `GET /svc/admin/payments/events?since=`
Admin view over `PaymentEvent` for monitoring scheduler charge attempts/failures (feeds the
Phase 4 admin dashboard).

---

## 5. `SchedulingHelper` (static-methods class, not wired to any trigger yet)

Per your direction, this phase only produces the **logic**, invoked manually/by tests for now;
wiring it to an actual cron/timer/queue is deferred. Location:
`wanda.servlets.helpers.SchedulingHelper`.

- `List<UserServiceSubscription_Data> findDueRenewals(Connection C, LocalDate asOf)`
  — subscriptions where `nextChargeDt <= asOf` and `active = true` and not cancelled-and-past-end.

- `PaymentOrder_Data createRenewalOrder(Connection C, UserServiceSubscription_Data sub)`
  — builds a `PaymentOrder` (`orderType=RENEWAL`, `initiatedBy=SCHEDULER`) for the subscription's
  current `ServicePricing.baselineAmount`, ready for Phase 3's charge-execution helper to process
  against the vaulted payment token.

- `void applyRenewalSuccess(Connection C, PaymentOrder_Data order)`
  — on successful scheduler charge: writes `GRANT` ledger entry for `includedCredits`, resets
  `cycleConsumed = 0`, advances `nextChargeDt` by one cycle, applies any `pendingPricingRefnum`
  tier change (clearing the pending field), and updates `PaymentOrder.status = PAID`.

- `void applyRenewalFailure(Connection C, PaymentOrder_Data order, String reason)`
  — records `PaymentEvent(CHARGE_FAILURE)`, increments `retryCount`, and (policy TBD — see Open
  Items) either retries on a short backoff or suspends the subscription after N failures.

- `List<UserServiceSubscription_Data> findDueOverageCharges(Connection C, LocalDate asOf)`
  — subscriptions with `cycleConsumed` in `UserServiceCreditBalance` exceeding
  `includedCredits` and no corresponding uncharged overage yet this cycle.

- `PaymentOrder_Data createOverageOrder(Connection C, UserServiceSubscription_Data sub, BigDecimal overageCredits)`
  — builds a `PaymentOrder` (`orderType=OVERAGE`, `initiatedBy=SCHEDULER`) priced at
  `overageAmountPerCredit * overageCredits`.

- `List<UserServiceSubscription_Data> findDueAutoRefills(Connection C)`
  — subscriptions with `autoRefill = true` and current balance below `autoRefillThreshold`.

- `PaymentOrder_Data createAutoRefillOrder(Connection C, UserServiceSubscription_Data sub)`
  — builds a `PaymentOrder` (`orderType=TOP_UP`, `initiatedBy=SCHEDULER`) for
  `autoRefillAmount` credits.

- `void expireCancelledSubscriptions(Connection C, LocalDate asOf)`
  — subscriptions with `cancelRequestedDt is not null` and `effectiveEndDt <= asOf`: set
  `active = false`, `endDt = effectiveEndDt`, clear `nextChargeDt` so no further renewal is
  attempted.

- `void applyPendingTierChanges(Connection C, UserServiceSubscription_Data sub)`
  — called from `applyRenewalSuccess`; folds `pendingPricingRefnum` into `pricingRefnum` at the
  renewal boundary.

- `void reconcilePendingOrders(Connection C, Duration olderThan)`
  — safety net sweep over `PaymentOrder` rows stuck in `CRTD`/`PNDG` beyond a threshold; logs a
  `PaymentEvent(RECONCILE_MISMATCH)` for admin follow-up rather than silently leaving them.

**Invocation:** left open. Options to evaluate when wiring this up later: a
`ServletContextListener` + `ScheduledExecutorService` (simplest, in-process, matches this
codebase's lack of an existing external scheduler), a dedicated admin-triggered endpoint for
manual/testing invocation, or delegation to the Tilda `Job` mechanism already used elsewhere
(`tilda.data.tilda.Job`, see `TenantJob`) if that framework supports recurring triggers. No
decision needed for Phase 2 itself.

---

## 6. Helper Classes Supporting the Servlets

- **`CatalogHelper`** — replaces `PlanHelper`'s catalog-lookup responsibilities
  (`getAvailablePlans`, `getPlanPrice`) against `Service`/`ServicePricing`.
- **`SubscriptionHelper`** — replaces the subscription-management logic currently inlined in
  `PaymentOrderCapture` (`getUserPlanSubscription`, `checkBilling`, `isStillValid`), generalized
  to multiple concurrent subscriptions per user and cancellation-aware.
- **`CreditLedgerHelper`** — encapsulates ledger writes + balance-cache updates so both the
  end-user `consume`/`topup` servlets and `SchedulingHelper` share one code path (avoids the two
  places drifting out of sync).

---

## 7. Open Items Before Implementation

1. Renewal failure policy: immediate suspension, N retries over a grace period, or notify-only?
   Affects `applyRenewalFailure` logic and whether `UserServiceSubscription` needs a `suspended`
   sub-state distinct from `active=false`.
2. Should `spendCapPerCycle` ever hard-block consumption, or is it always alert-only (never
   preventing usage, only notifying the user/admin)?
3. Confirm the single-servlet-vs-two-servlet question for create/capture flows (subscribe,
   top-up) — likely resolved once Phase 3's PayPal helper shape is finalized.
