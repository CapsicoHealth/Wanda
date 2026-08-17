# Payment Infrastructure Plan — Phase 4: Future UI Work (Notes Only)

**Status:** High-level guidance for a future UI implementation effort — not scheduled for
immediate work. No components/screens are being built now. This doc exists so that when UI work
starts, the API contracts from Phase 2/3 are already understood and don't need rediscovery.

---

## 1. Screens/Surfaces Needed

### 1.1 Service Catalog (browse/subscribe)
- A general-purpose catalog page listing all active `Service` entries (via
  `GET /svc/services/list`), each showing its tiers/currencies (`ServicePricing`).
- Per-service detail/modal (`GET /svc/services/details`) showing full tier comparison for the
  user's selected currency (currency selection likely inferred from locale/account setting, with
  a manual override).
- **"Subscribe" button component** — reusable across *any* application screen (per your
  requirement), not just the catalog page. Needs to accept `serviceCode` + optional
  `tierCode`/`currency` as props/config, and internally drive the
  `POST /svc/services/subscribe` → PayPal approve redirect →
  `POST /svc/services/subscribe/capture` flow. Should be built as a shared component early so
  individual app screens just drop it in with a service code.

### 1.2 Checkout / Payment Method Flow
- Standard PayPal button/redirect flow for new subscriptions and top-ups (interactive,
  user-present) — same UX pattern as today's PayPal integration, just pointed at the new
  endpoints.
- Needs explicit **consent messaging** when a payment method is being vaulted for future
  recurring/auto-refill charges ("By subscribing, you authorize recurring charges of $X per
  [month/year], plus any additional credits you purchase or enable auto-refill for.") — a
  compliance/trust requirement, not just a nicety.
- Currency selector at checkout, defaulting to the user's likely region but allowing override
  (since pricing is native per currency, not converted, the selector determines *which*
  `ServicePricing` row is used, not a computed conversion).

### 1.3 User Dashboard — "My Subscriptions"
- Fed by `GET /svc/services/subscription/list`.
- Per subscription, show: service name/tier, status (active / cancel-pending / expired), current
  credit balance + included-per-cycle amount (`GET /svc/services/credits/balance`), next charge
  date and amount, and action buttons:
  - **Cancel** → confirmation modal explicitly stating *"Your subscription will remain active
    until <effectiveEndDt>. No refund or proration applies."* — matches your no-proration
    decision; this messaging is important to get right so users aren't surprised.
  - **Change tier** → shows the pending tier change take-effect date if one is already queued.
  - **Buy credits now** (manual top-up) → `POST /svc/services/credits/topup`.
- **Usage/credit history** — paged table from `GET /svc/services/credits/history`, useful for
  users to understand what consumed their credits.

### 1.4 User Dashboard — "Limits & Auto-Refill" (your explicit ask)
- Per subscription, expose:
  - **Spend cap per cycle** (`spendCapPerCycle`) — "alert me if my overage spend this cycle
    exceeds $X" (confirm with Phase 2 Open Item #2 whether this ever hard-blocks usage or is
    always alert-only before finalizing this UI's copy/behavior).
  - **Auto-refill policy** (`autoRefill`, `autoRefillThreshold`, `autoRefillAmount`) — the exact
    scenario you described: *"if balance falls below $10, refill 10 credits"*. UI should present
    this as a simple threshold + amount pair, with a preview of "at your current rate, this
    happens roughly every N days" if usage history allows estimating that (nice-to-have, not
    required for v1).
  - Payment method status (`PaymentVaultToken.active`) — if the vaulted method has failed/expired
    (per Phase 3 §1.3), prompt the user to re-authorize before their next renewal/auto-refill
    fails.

### 1.5 Admin — Catalog Management
- CRUD screens over `Service` (`/svc/admin/services/*`) and `ServicePricing`
  (`/svc/admin/services/pricing/*`), modeled on the existing admin patterns (e.g., current
  `PlanListServlet`/`PromoCreate`-style admin screens already in the app for other entities).
- Pricing editor needs a **matrix-style UI**: rows = tiers, columns = currencies, since a service
  can have multiple tiers × multiple currencies simultaneously (per your explicit multi-tier +
  regional pricing requirement) — a flat form won't scale well once there are more than 2–3
  currencies.

### 1.6 Admin — Orders & Payment Monitoring
- Order history browser (`/svc/admin/payments/orders`) with filters by user/status/date range —
  support/finance tool for investigating a specific user's billing.
- **Scheduler health dashboard** (`/svc/admin/payments/events`) — surfaces `PaymentEvent` rows,
  especially `CHARGE_FAILURE`/`RECONCILE_MISMATCH`, so ops can see recurring-billing health at a
  glance once the scheduler is eventually wired up (Phase 2 §5). This is the operational
  replacement for today's single "email admin on failure" approach.

---

## 2. Cross-Cutting UI Notes

- **Reusable "subscribe" widget**: since you specifically want subscribe buttons on arbitrary
  app screens (not just a central store page), design this as a standalone, embeddable
  component/module early, backed only by `serviceCode` — avoid coupling it to any one app's
  layout.
- **Credit balance indicator**: likely wanted as a small persistent UI element (header/nav badge)
  for services with metered usage, so users always see their remaining credits without visiting
  the dashboard — worth designing as a shared widget alongside the subscribe button.
- **No-proration messaging consistency**: cancellation and tier-change both need the same "takes
  effect at the end of the current cycle" language; keep this copy centralized (shared component)
  so it stays consistent as more services are added.
- **Multi-currency display**: since amounts are native per currency (no conversion), avoid any UI
  affordance that implies a computed exchange rate (e.g., don't show "$20 (~€18)") — show only the
  actual native price for the selected currency's tier.

---

## 3. Explicitly Deferred / Not Yet Decided

These depend on product decisions still open in Phase 2/3 and should be revisited before UI work
starts:
- Whether exceeding `spendCapPerCycle` ever blocks usage (affects whether the UI needs a hard
  "you've hit your limit" blocking state vs. an informational banner only).
- Renewal failure/retry policy (affects whether the dashboard needs a "payment retry in
  progress" status distinct from "active"/"cancelled").
- Whether unused included credits roll over or expire at cycle end (affects how the credit
  balance widget explains "why did my balance reset").

No UI implementation should begin until these are resolved, since they materially affect what
states the dashboard needs to represent.
