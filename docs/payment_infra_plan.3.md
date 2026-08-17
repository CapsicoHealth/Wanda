# Payment Infrastructure Plan — Phase 3: PayPal Integration

**Status:** Draft for review — no implementation yet. Depends on Phase 1 schema & Phase 2 servlets.
**Target locations:**
- `Wanda/src/wanda/servlets/helpers/PayPalHelper.java` (extend existing)
- New DTOs alongside `PayPalOrderDetails.java` / `PayPalPreOrder.java`
- `Wanda/src/wanda/servlets/` — capture/charge servlets

**Decision recap (per your direction):** Pure PayPal **Orders API** throughout — no PayPal
Subscriptions/Billing-Plans API. All recurring logic (baseline renewal, overage, auto-refill) is
driven by **our own `SchedulingHelper`** (Phase 2), which needs to charge a user **without a
browser present**. That requires vaulting the payment method once, then reusing it.

---

## 1. What Changes vs. Today's `PayPalHelper`

Today's flow ([PayPalHelper.createOrder](../src/wanda/servlets/helpers/PayPalHelper.java) /
`captureOrder`) is a classic interactive checkout: create order → user approves in PayPal UI →
capture. This works fine for user-initiated actions (new subscription, manual top-up) and is
**kept as-is** for those cases. What's new is the ability for the **scheduler** to charge later
without that interactive approval step — this is the PayPal **Vault** (saved payment method) API.

### 1.1 Vaulting a payment method
- PayPal's Orders API supports requesting that a payment method be saved for future use by
  including a `payment_source` with vaulting attributes on order creation, or via the separate
  `/v3/vault/payment-tokens` API depending on integration style. Exact request shape to be
  confirmed against current PayPal API docs at implementation time (PayPal's vaulting API has
  evolved; the v2/v3 details should be re-verified against the live API reference before coding).
- Plan: extend `PayPalHelper.createOrder(...)` with an overload/flag
  `requestVaulting=true` used specifically by the **new-subscription** create flow (not one-off
  top-ups), so a `provider vault token` comes back attached to the order/capture response.
- On successful capture of a **new subscription** order, extract the vault token id from the
  capture response and persist it as a `PaymentVaultToken` row (Phase 1 schema), linked to the
  user.
- If the user already has an active `PaymentVaultToken` for the provider, skip re-vaulting on
  subsequent new-subscription orders (reuse the existing token) — but always allow the user to
  add/replace it (e.g., if a previous card was removed on PayPal's side and a charge failed).

### 1.2 Charging a vaulted payment method (scheduler-initiated)
- New method: `PayPalHelper.chargeVaultedOrder(String paymentProvider, String vaultTokenId,
  String currency, BigDecimal amount, String customId)`.
- Creates an order with `payment_source.paypal.vault_id` (or the equivalent token reference field
  per current API) and `intent=CAPTURE`, then immediately captures it server-side — no redirect,
  no user interaction, since the payment source is already authorized via the vault token.
- Returns the same `PayPalOrderDetails` shape as today's `captureOrder`, so
  `SchedulingHelper.applyRenewalSuccess`/`applyRenewalFailure` (Phase 2) can reuse the existing
  status-mapping switch (`COMPLETED → PAID`, etc.) already present in `PaymentOrderCapture`.
- This single method serves **all three** scheduler-initiated order types (`RENEWAL`, `OVERAGE`,
  `TOP_UP` auto-refill) — they only differ in amount/currency/reference, not in the charge
  mechanism.

### 1.3 Handling vault charge failures
- Declines/expired-token errors from PayPal are caught and translated into a `PaymentEvent`
  (`CHARGE_FAILURE`) with the raw error payload stored in `rawPayload`, per Phase 1 §2.8.
- If the failure reason indicates the vaulted token is no longer usable (e.g., card expired,
  payer revoked authorization), mark `PaymentVaultToken.active = false` so the UI (Phase 4) can
  prompt the user to re-authorize/re-vault before the next cycle.

---

## 2. Servlet-Level Changes

### 2.1 New-subscription capture (`/svc/services/subscribe/capture`, Phase 2 §2)
- Calls `PayPalHelper.captureOrder(...)` (existing method, unchanged) with `requestVaulting`
  honored at the earlier create step.
- On `COMPLETED` capture: persists `PaymentVaultToken` (§1.1), creates `UserServiceSubscription`
  with `nextChargeDt` set, writes the initial credit `GRANT`.
- Mirrors today's status-mapping switch in `PaymentOrderCapture` almost verbatim — this logic is
  reusable, just retargeted at `PaymentOrder`/`UserServiceSubscription` instead of
  `UserPlanBilling`/`UserPlanSubscription`.

### 2.2 Top-up capture (`/svc/services/credits/topup/capture`, Phase 2 §3)
- Same interactive create/capture pattern as today, no vaulting required (one-off), unless the
  user has no vault token yet and we choose to opportunistically vault here too (open item).

### 2.3 Scheduler charge execution (internal, called from `SchedulingHelper`, not a servlet)
- A new helper, `PaymentChargeExecutor` (or folded directly into `SchedulingHelper` — naming TBD),
  wraps `PayPalHelper.chargeVaultedOrder(...)`, updates `PaymentOrder` status, and calls the
  appropriate `SchedulingHelper.applyRenewalSuccess/Failure` continuation.
- Runs inside the same `Connection`/transaction conventions as other servlets, even though it's
  not triggered by an HTTP request — needs a small "batch job" entry point that opens a
  `Connection` the way `SimpleServlet` does today (to be modeled after however Tilda `Job`
  execution acquires connections elsewhere in the codebase, per `TenantJob`).

---

## 3. New/Extended DTOs

- **`PayPalVaultToken`** (new, alongside `PayPalOrderDetails`/`PayPalPreOrder`) — mirrors whatever
  fields PayPal's vault response includes (token id, status, payer info for display).
- **`PayPalOrderDetails`** — extend `PurchaseUnit`/`Payer` parsing if the vaulted-charge capture
  response includes additional fields not present in the interactive flow (to confirm once the
  real API responses are captured in sandbox testing).
- **Error DTO** — currently PayPal error responses aren't modeled at all (`PayPalHelper` just
  throws `IOException` with the status code). For scheduler-driven charges we need to actually
  parse the PayPal error body (`name`, `message`, `details[]`) to decide retry vs. permanent
  failure vs. "re-vaulting needed" — new `PayPalErrorResponse` DTO.

---

## 4. Currency Handling

- No conversion logic needed anywhere in the PayPal layer — `ServicePricing.currency` and
  `PaymentOrder.currency` are passed straight through to PayPal's `amount.currency_code`, exactly
  as today's `PayPalHelper.createOrder` already does.
- Confirm in sandbox testing which of USD/CAD/GBP/EUR/INR require any special PayPal
  merchant-account configuration (e.g., INR settlement eligibility depends on the merchant
  account's registered country — flagged previously, still an open validation item, not a code
  change).

---

## 5. What We're Explicitly NOT Building (per your decision)

- No PayPal Subscriptions/Billing Plans/Products API usage.
- No inbound PayPal webhook endpoint/signature verification — since PayPal isn't initiating any
  charges on its own schedule, there's nothing for it to notify us about asynchronously beyond
  the synchronous create/capture calls we already make. `PaymentEvent` logging is entirely
  populated by **our own** scheduler's call outcomes, not by PayPal callbacks.
- If this decision changes later (e.g., PayPal introduces a compelling reason to offload baseline
  billing to their side), this doc's §1–2 would need a follow-up webhook-handling addendum.

---

## 6. Open Items Before Implementation

1. Re-verify PayPal's current (2026) vaulting API shape in sandbox before coding — API surface
   for saved payment methods has changed across PayPal API versions historically.
2. Retry policy for declined vault charges (immediate single retry? scheduled retry next day?
   cap at N attempts before suspending the subscription?) — ties into Phase 2 Open Item #1.
3. Should top-up orders opportunistically vault a payment method if the user has none yet (so a
   later auto-refill can work without first requiring a subscription purchase)?
