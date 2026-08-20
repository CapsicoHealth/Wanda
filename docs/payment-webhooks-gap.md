# Payment Gap: No PayPal Webhook Handler (deferred)

**Status: known gap, deferred.** This document exists so the gap isn't forgotten, not because it needs
fixing right now.

## The gap

The entire payment capture flow is driven **synchronously, client-side only**:

1. `module-payments.js` (`FloriaPayments.PayPalSDK.initButtons`) renders the PayPal Buttons and, on
   `onApprove`, calls `POST /web/svc/payments/order/capture`.
2. `PaymentOrderCapture.java` then calls `PayPalHelper.captureOrder(...)` (`POST
   /v2/checkout/orders/{id}/capture`) and records the result in `UserPlanBilling` /
   `UserPlanSubscription` / `UserPlanCreditLedger`.

There is **no PayPal webhook (IPN-equivalent) endpoint anywhere in `wanda/servlets`**. PayPal's own
recommended integration pattern is to treat webhooks as the source of truth for order/capture state, with
the client-side "capture" call as an optimization/fast-path only — precisely because the browser cannot be
trusted to always complete that last round trip.

## Why this matters

If the browser tab is closed, the network drops, or the user's device dies **after** PayPal shows the
"approved" confirmation but **before** the `/svc/payments/order/capture` call finishes:

- PayPal considers the order **approved** (and, depending on timing, possibly already **captured** on
  PayPal's side if a delayed/async capture ever gets configured).
- Wanda has recorded **nothing**: no `UserPlanBilling` row, no wallet grant, no subscription activation.
- The user may have been charged (or will be, once PayPal captures) with no corresponding entitlement
  granted on our side, and no automated process will ever notice or reconcile this.

Today, the only mitigation is `UserPlanPreOrder` + `PaymentOrderCreate.isPreOrderStillValid(...)`: if the
user retries the same product's purchase, the existing pre-order can be reused (or cleaned up and
recreated) rather than creating a duplicate PayPal order. This reduces *duplicate charges* on retry, but
does **nothing** to recover a purchase that was approved/paid on PayPal's side and never got captured or
recorded here, and does nothing for a user who simply never retries.

Related, smaller gaps in the same area (see `payment-documentation.md` §5.4 for the credits-specific one):
- No reconciliation job compares PayPal's order/capture state against `UserPlanBilling` to find and repair
  orphaned approvals.
- No handling for **asynchronous** capture outcomes (e.g. a capture that comes back `PENDING` and later
  resolves to `COMPLETED` or `DECLINED` purely on PayPal's side) — `PaymentOrderCapture` maps
  `PAYER_ACTION_REQUIRED`/`SAVED` to `Pending` but nothing ever revisits a `Pending` billing row later.
- No handling for **refunds/disputes/chargebacks** initiated from PayPal's side (merchant dashboard or
  buyer dispute) — these would need to flow back into `UserPlanBilling.status` / wallet credit reversal,
  and today they simply never reach Wanda at all.

## What a real fix would look like (for when this is picked up)

1. **Register a webhook** in the PayPal Developer Dashboard (per app, sandbox and live separately) pointing
   at a new servlet, e.g. `/svc/payments/paypal/webhook`, subscribed at minimum to:
   - `CHECKOUT.ORDER.APPROVED`
   - `PAYMENT.CAPTURE.COMPLETED`
   - `PAYMENT.CAPTURE.DENIED`
   - `PAYMENT.CAPTURE.PENDING`
   - `PAYMENT.CAPTURE.REFUNDED`
   - `PAYMENT.CAPTURE.REVERSED`
2. **Verify webhook signatures** server-side via PayPal's
   `POST /v1/notifications/verify-webhook-signature` API (never trust an unverified payload) — this needs
   a `webhookId` (from the dashboard registration) added to `PaymentSystem`/`wanda.config.json`.
3. **Make `PaymentOrderCapture`'s logic idempotent and callable from two entry points**: the existing
   client-driven path, and a new webhook-driven path, both converging on the same
   "find-or-create `UserPlanBilling` by `orderId`, apply status" logic — this mostly already exists
   (`UserPlanBilling_Factory.lookupByOrder` + the `isStatusPaid()` idempotency guard in
   `PaymentOrderCapture`), it just needs to be reachable without a live user session/request context.
4. **Add a reconciliation sweep** (a `Beacon`/scheduled job, following the existing pattern in
   `wanda.beacons.*`) that periodically lists recent PayPal orders/captures and cross-checks them against
   `UserPlanBilling`, flagging (or auto-repairing) any approved-but-never-recorded orders.
5. **Test plan once implemented**: use the PayPal Developer Dashboard's sandbox **"Webhooks simulator"**
   to fire each event type at the new endpoint without needing a real end-to-end checkout, in addition to
   the manual "approve then kill the tab before capture" scenario described in the payment sandbox testing
   notes.

## Decision

Deferred. Tracked here so it is visible next time the payment flow is revisited, and so anyone debugging a
"user says they were charged but got nothing" support ticket knows this is the known, not-yet-closed gap
to suspect first.
