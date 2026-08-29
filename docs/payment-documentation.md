# Payment / Credits — App Integration Touchpoints

This is the practical, "what do I actually call" companion to
`payment_infra_simple_enhancement_plan.md`. That document explains the design and the *why*; this one
is for an application developer wiring a metered feature into the credit system.

It covers exactly four touchpoints:
1. **Java — check** the balance is positive before doing metered work.
2. **Java — charge** for work done, and read back whether the balance is still positive.
3. **Front-end — pop up** the "pick a plan / top up" UI when the client is told to.
4. **Front-end — embed** a live credit-balance gauge widget anywhere in the app's own UI.

And one cross-cutting concern that applies to both Java touchpoints: **concurrent requests from the
same user (multiple browser tabs) updating the same wallet row.**

---

## 0. The model in one paragraph

A **wallet** is the user's active `UserPlanSubscription` row for a given `paymentSystemProductId`
(a `planType=C` product, e.g. `"CAPSICO-AGENTIC-01"`). It holds a `creditsBalance`. Every grant/use/
adjustment is also appended to the WORM `UserPlanCreditLedger`, which is the source of truth; the
balance on the subscription is a denormalized cache of it. Credits are **abstract units, not
currency** (1 credit = 1 US cent by convention) — see the main plan doc §3.3(b) for why.

**The balance is allowed to go negative.** The cost of an agentic operation is usually only known
once it finishes, so refusing to record an overrun would mean giving the work away for free and
losing the audit trail. The contract is: *you may start if you are not already in the red; the
operation you are about to run may push you into the red; you must settle up before the next one.*

All of this is app-driven. There is **no HTTP status code involved** — an "out of credits" response is
a perfectly valid, often *successful* request. The signal travels inside the app's own response
payload as an application-level code, exactly the same way a plain result would.

### 0.1 ⚠ `CreditHelper` is the ONLY sanctioned way to touch a wallet — no exceptions

**Every mutation of `UserPlanSubscription.creditsBalance` / `.creditsPurchased` /
`.creditsBonusGranted`, and every insert into `UserPlanCreditLedger`, MUST go through
`CreditHelper`** (`check`/`charge`/`consume`/`grant`/`grantSignupBonusIfEligible`/`adjust`, all of which
ultimately funnel through its private `post()`). This is not a style preference — it is what makes
every other guarantee in this document true:

- The **atomic increment / floor-guard concurrency fix** (§5) only protects writes that go through
  `UserPlanSubscription_Factory.incrementCreditsBalance`. A direct `UPS.setCreditsBalance(x); UPS.write(C);`
  anywhere else in the codebase silently reintroduces the exact lost-update bug §5.1 describes, for that
  one code path, with no error or warning.
- The **ledger being the source of truth** (§0) only holds if nothing ever changes `creditsBalance`
  without also appending the matching `UserPlanCreditLedger` row in the same call — which is exactly what
  `post()` does and a hand-rolled write would not.
- The **balance-display cache** (§5.5 below) is only ever correct because `post()` is the single choke
  point that invalidates it. A write that bypasses `CreditHelper` will not invalidate the cache, and the
  credit-meter widget (and anything else reading `CreditHelper.getSnapshot`) can then show a stale/wrong
  balance for up to `CreditHelper.CACHE_TTL_MS`.

There is no code-level mechanism (e.g. a package-private setter) preventing a direct write today — Tilda's
generated setters are `protected`, reachable from any class in `wanda.data`. **This must be caught in code
review**: any diff that touches `UserPlanSubscription`'s credit fields or writes a
`UserPlanCreditLedger_Data` from somewhere other than `CreditHelper` itself should be rejected. If a
legitimate new use case needs a balance mutation this class doesn't already support, add a new method to
`CreditHelper` (routed through its `post()`) rather than writing around it.

---

## 1. Java — Touchpoint #1: check the balance before starting work

```java
import wanda.servlets.helpers.CreditHelper;
import wanda.servlets.helpers.CreditHelper.CreditStatus;

CreditStatus status = CreditHelper.check(C, U, PRODUCT_ID);
if (status.isOK() == false)
  {
    JSONPrinter j = new JSONPrinter();
    status.toJSON(j);          // emits a "creditStatus" block, code = INSUFFICIENT_CREDITS
    res.successJson(j);        // still a normal 200: the request was valid, there's just no balance
    return;                    // do NOT start the expensive work
  }

// balance is OK: proceed with the metered operation
```

`isOK()` is `false` when either:
- the user has **no wallet at all** for that product (they have never bought a pack), or
- the wallet's balance is **negative**.

A **zero** balance is `OK` — that is the headroom that lets the exit-charge below land even when the
final cost isn't known until the operation completes.

This call is read-only. It does not lock, write, or hold anything open — call it as early as possible,
before any expensive work.

---

## 2. Java — Touchpoint #2: charge for work done, and confirm the balance is still positive

```java
// ... the metered operation has now run, and its actual cost is known ...
BigDecimal cost = results.getCreditCost();

CreditStatus status = CreditHelper.charge(C, U, PRODUCT_ID, cost, "job:" + results.getId());

JSONPrinter j = new JSONPrinter();
j.addElement("results", results, "");   // ALWAYS return the results: the work was done and is being paid for
status.toJSON(j);                        // code is null if still OK, INSUFFICIENT_CREDITS if now negative
res.successJson(j);
```

Key properties of `charge`:
- It **always** applies the debit, even if it drives the balance negative. This is deliberate (see §0).
  It is not the same call as `CreditHelper.consume`, which refuses and writes nothing if the balance
  can't cover it — `consume` is for the rarer case where the cost is known *before* the work starts and
  must be pre-authorized.
- The client must **never retry** on an `INSUFFICIENT_CREDITS` code from `charge`: the operation
  already happened and has already been billed. Retrying would run (and bill for) the work twice. The
  code only means "prompt the user to top up before the *next* one."
- `status.getCode()` is `null` whenever nothing is needed, so the app-side check is simply:
  `if (status.getCode() != null) { ... }`.

### `CreditStatus` JSON shape

```json
"creditStatus": { "productId":"CAPSICO-AGENTIC-01", "code":"INSUFFICIENT_CREDITS"
                , "ok":false, "hasWallet":true, "balance":-320 }
```

`code` is the one field an app's own front-end needs to inspect; the rest is there for a balance
display.

### Which of the three methods to call

| Situation | Method | Behavior |
|---|---|---|
| Before starting work | `CreditHelper.check` | Read-only |
| After work, cost now known | `CreditHelper.charge` | **Always** debits, may go negative |
| Cost known up front, must be pre-authorized | `CreditHelper.consume` | Refuses and writes nothing if insufficient |

---

## 3. Front-end — Touchpoint #3: pop up the "pick a plan / top up" UI

In `module-login.js`:

```javascript
FloriaAjax.ajaxUrl(myAppServiceUrl, "POST", null, function(data) {
    if (data.results != null)                 // Always render what we got: it happened and was billed.
     renderResults(data.results);

    if (data.creditStatus?.code != null)
     FloriaLogin.PopupLogin.topUpCredits(data.creditStatus.productId, function() {
        // Optional "resume" callback, called once the top-up purchase completes.
        refreshCreditMeter();
     });
  });
```

`FloriaLogin.PopupLogin.topUpCredits(productId, onComplete)`:
- Opens the same dialog infrastructure used for plan selection (`paintPlans` / `pricingSelect` / the
  PayPal buttons / `/svc/payments/order/*`), pre-filtered to the one product via
  `/svc/user/plan/status?productId=...` (which also returns the current `creditStatus` inline, so this
  is a single round trip).
- Shows a balance banner (including a "you have used more credits than you purchased" note when
  negative).
- If **no `onComplete` is supplied**, a successful purchase reloads the page (the original,
  subscription-era behavior).
- If **`onComplete` is supplied**, the dialog closes and your callback runs instead — use this whenever
  the user was mid-workflow, so the results already on screen are not thrown away by a reload.

There is nothing else to call: `topUpCredits` owns the entire purchase flow end to end.

---

## 4. Front-end — Touchpoint #4: embed a live credit-balance gauge

`FloriaPayments.CreditGauge` (in `module-payments.js`) is a small, self-contained, embeddable widget: a
slim SVG bar (fixed 3:1 aspect ratio, so it always reads as a wide "meter" rather than a tall/square
dial) that fetches and displays the signed-in user's credit balance for **one** product. It is scoped to a
single `productId` deliberately — a user's plan can span several products at once (subscriptions, flat
licenses, usage-based credits, or a mix), each with its own billing rules, so a single gauge mixing more
than one product's balance would be meaningless. Embed one gauge per product.

```html
<!-- The gauge sizes itself entirely off this host DIV's own width (aspect-ratio:3/1 in CSS) —
     anywhere from width:5em up to width:20em (or more) works without any other configuration. -->
<div id="creditGauge_AGENTIC" style="width: 12em;"></div>
```

```javascript
import { FloriaPayments } from "./module-payments.js";

// hostDivId, basePath (same convention as PlansDialog.pickPlan/topUpCredits above), productId, active
FloriaPayments.CreditGauge.render("creditGauge_AGENTIC", basePath, "CAPSICO-AGENTIC-01", true);
```

- **Data source**: `/svc/wanda/credits/balance?productId=...`, backed by `CreditHelper.getSnapshot` (see
  §5.5 below) — cheap enough to call `render()` again whenever you want to refresh it (e.g. right after a
  `topUpCredits` purchase completes, or after a metered operation's own response comes back), since it is
  served from cache far more often than it hits the database.
- **`active` (4th argument)**: when `true`, the widget shows a pointer cursor and clicking it opens a
  details popup (currently a "feature coming soon" placeholder — the full cost/activity breakdown is
  planned to be built on top of `/svc/wanda/credits/history`, which already exists). When `false` (or
  omitted), the gauge is purely informational: no cursor change, no click handler attached.
- **Colors are never hardcoded in the widget's JS.** The fill's color tier is expressed only as a CSS
  class (`creditGaugeFill--low` / `--mid` / `--high`); the actual colors are CSS custom properties defined
  in `module-login.css` (`--credit-gauge-low/mid/high`, with sensible fallbacks), so a host app can retheme
  the gauge without touching `module-payments.js`.
- **Tiers**: `FloriaPayments.CreditGauge._LOW_MAX` (default 300 credits / $3) and `._MID_MAX` (default 500
  credits / $5) are plain constants a host app may reassign before its first `render()` call to match its
  own product's pricing. Below `_LOW_MAX` is "low" (red), from there up to `_MID_MAX` is "mid" (yellow),
  and `_MID_MAX` and above is "high" (green, and also where the bar visually saturates at 100% full) — a
  fixed-tier design rather than a true "% of last top-up" gauge, since no per-wallet "last top-up amount"
  is tracked today (see §5.5's cache doc for the related `creditsPurchased` caveat: it is a lifetime total,
  not a last-purchase amount, so it is not used as the gauge's scale).
- Calling `render()` again on the same `hostDivId` (e.g. with a different `productId`, or just to refresh
  the balance) is safe: it fully repaints the host and does not leak duplicate click handlers.

---

## 5. ⚠ Concurrency: multiple tabs, the same wallet, and deadlocks

**Status: the lost-update risk described below is FIXED.** Every balance mutation
(`CreditHelper.charge`/`consume`/`grant`/`adjust`) now goes through
`UserPlanSubscription_Factory.incrementCreditsBalance(...)`, a single atomic
`UPDATE ... SET creditsBalance = creditsBalance + ?` built with `tilda.db.QueryHelper.setIncrement()` —
not the traditional `getCreditsBalance()` → compute in Java → `setCreditsBalance()`+`write()` chain. The
rest of this section explains the problem that fix removes, and what is still worth knowing.

### 5.1 The problem this replaces (no longer present, kept for context)

Every servlet request runs in its own database transaction. If a user has two tabs open and both fire a
metered request for the **same wallet** at close to the same time, the OLD code did:

1. Both requests read the wallet's `creditsBalance` — say both see `500`.
2. Both compute their own new balance in Java (e.g. `500 - 120 = 380` and `500 - 90 = 410`).
3. Both issue an `UPDATE ... SET creditsBalance = 410 WHERE ...` (a literal value, not an expression).

Tilda's generated `write()` does **not** guard this with an optimistic version/timestamp check — the
`UPDATE`'s `WHERE` clause matches on the natural/lookup key only (confirmed by reading
`TILDA__USERPLANSUBSCRIPTION.getWriteQuery()`), not on the value that was read. So the second
transaction's literal `410` would silently overwrite the first transaction's `380` once its row lock was
released — a **lost update**, with the final balance wrong (`410` instead of the correct `290`) and no
exception raised anywhere.

### 5.2 How the fix works

`incrementCreditsBalance` builds `SET creditsBalance = creditsBalance + ?` — the new value is **SQL
arithmetic against whatever the row holds at the moment the `UPDATE` actually executes**, under the
database's own row lock, not a value computed from a Java-side read taken earlier. Two concurrent calls
for the same wallet are simply serialized by Postgres: the second one blocks on the row lock, then applies
its own `+ delta` on top of whatever the first one just committed. No lost update is possible.

The same statement can optionally carry a **floor guard** —
`AND coalesce(creditsBalance,0) + delta >= 0` — used only by `CreditHelper.consume()`'s strict
pre-authorization: the sufficiency check and the debit are the same atomic operation, so "is there enough"
and "deduct it" can no longer race against each other the way a separate Java `if` followed by a write
could. `charge()`, `grant()`, and `adjust()` all pass `floorGuard=false`, since they must apply
unconditionally (see §0).

After a successful atomic update, `CreditHelper` calls `UPS.refresh(C)` to re-read the row's now-current
value directly from the database, rather than trusting a Java-computed number, before writing the ledger's
`balanceAfter` snapshot.

**If you write a new atomic increment for some other column**, the pattern to follow is
`tilda.db.QueryHelper.setIncrement(col, delta)` inside a hand-built `UpdateQuery` on the object's Factory —
see `UserPlanSubscription_Factory.incrementCreditsBalance` for a full example, including the floor-guard
`gte(...)` clause. `QueryHelper` did not previously support `NUMERIC`/`BigDecimal` columns at all
(`equals`/`gte`/`lte`/`plus`/`minus`/`set`/`setIncrement`) — that support was added to
`tilda/db/QueryHelper.java` as part of this fix and is now available for any other `NUMERIC` column in any
Tilda-based project, not just this one.

### 5.3 When this becomes an actual deadlock rather than a lost update

A **deadlock** (as opposed to a lost update) needs two transactions to acquire locks on **two or more
rows in opposite order**. The atomic increment above touches only the wallet row directly; the ledger
insert that follows references it via foreign key (which takes a `FOR KEY SHARE` lock on the wallet row
under Postgres) in the same order every time. As long as every caller goes through `CreditHelper`, a true
deadlock from this code path specifically is unlikely.

It is a real, previously-encountered failure mode in this codebase more generally, though: Tilda's
`Connection.handleCatch` explicitly detects a database deadlock error and marks it
(`QueryDetails.setLastQueryDeadlocked()`), and the front-end (`module-ajax.js`) already has a branch for
`error.type == 'DEADLOCKED'` that alerts the user their request was aborted. So: if your app service
touches other rows (billing, subscriptions, application-specific tables) **in addition to** a credit
charge, and does so in an order that differs between two different code paths that can run concurrently
for the same user, a genuine deadlock is still possible, and the user will see the existing "YOUR REQUEST
DEADLOCKED!" alert. Be consistent about row-touch order across code paths that can run for the same user.

### 5.4 What is still NOT covered

- **`creditsPurchased` and `creditsBalance` are updated together, atomically, in the same statement** when
  `post()` is called for a grant — so this particular pair is safe. Any OTHER field on the same row that
  some future code updates via the traditional `setXXX()+write()` chain is not automatically protected;
  the atomic pattern only covers what goes through `incrementCreditsBalance`.
- **A ledger reconciliation/rebuild helper does not exist.** The ledger is the source of truth and a
  `SUM(amount)` over `UserPlanCreditLedger` for a subscription would always tell you the "true" balance,
  but there is no code today that runs that query and compares/repairs the cached `creditsBalance`. This
  remains a manual, ad hoc operation if drift is ever suspected.
- **The double-submit race in `PaymentOrderCapture`** (two near-simultaneous captures of the same
  `orderId` both passing the idempotency check before either writes) is a separate, pre-existing race not
  addressed by this fix — see the main plan doc.

### 5.5 Balance-display cache (`CreditHelper.getSnapshot`) and the session-affinity assumption

For a display-only widget (e.g. the front-end credit-meter gauge, `FloriaPayments.CreditGauge` in
`module-payments.js`, backed by the `/svc/wanda/credits/balance` servlet) that may poll fairly often,
reading the database on every poll is wasteful. `CreditHelper.getSnapshot(C, U, productId)` serves these
reads from a small, bounded (`CACHE_MAX_ENTRIES`, default 50), in-process cache keyed by
`userRefnum|productId`, instead of hitting `getWallet()` fresh every time.

**This is a read-only, display-only path — never used by `check`/`consume`/`charge`, which always read
the database directly and are unaffected by any of the below.**

- **Invalidation, not expiry, is what keeps this correct.** `post()` — the single choke point every
  mutation goes through (see §0.1) — invalidates a wallet's cache entry synchronously the instant it
  changes. Within one JVM, the very next `getSnapshot()` call after any grant/charge/consume/adjust always
  sees the fresh value, no matter how long the TTL (`CACHE_TTL_MS`, currently 10 minutes) is. The TTL is
  not what makes this cache correct; invalidation is. The TTL only exists as a bound on the one failure
  mode invalidation can't cover:
- **This cache is plain in-process JVM memory — it is NOT shared/distributed across server instances.**
  If a mutation is handled by instance A but the next poll for the same user lands on instance B, B's copy
  (if it has one cached at all) will not have been invalidated, and could serve a stale balance for up to
  `CACHE_TTL_MS`.
- **This is a pre-existing, application-wide assumption, not a new one introduced by this cache**: the
  whole stack already assumes **session affinity** (sticky sessions) — a given user's requests are
  consistently routed to the same server instance for the lifetime of their session. Given that existing
  assumption, this cache's per-instance memory is exactly as safe as everything else already relying on
  session affinity, and a 10-minute TTL costs nothing in the normal case.
- **If that assumption is ever relaxed** (a load balancer without sticky sessions, or the cache is
  otherwise expected to serve a single user from more than one instance interchangeably), either lower
  `CACHE_TTL_MS` back down to something short enough to bound the staleness window acceptably, or move
  this cache to a shared store (e.g. the DB itself, or a distributed cache) — do not simply leave it as
  in-process memory and rely on the TTL alone to paper over cross-instance staleness.

---

## 6. Quick reference

```java
// Entry gate — read-only, always hits the DB
CreditHelper.check(C, U, productId).isOK()

// Exit charge — always applies, may go negative, always hits the DB
CreditHelper.charge(C, U, productId, cost, reference)

// Strict pre-authorization — refuses if insufficient, always hits the DB
CreditHelper.consume(C, U, productId, cost, reference)

// Display-only balance for a UI widget — served from a cache (see §5.5), NEVER for gating/mutation
CreditHelper.getSnapshot(C, U, productId)
```

```javascript
// Front-end top-up, reusing the whole plan-picker + PayPal flow
FloriaLogin.PopupLogin.topUpCredits(productId, onComplete)

// Front-end embeddable credit-balance gauge, scoped to one productId (see §4)
FloriaPayments.CreditGauge.render(hostDivId, basePath, productId, active)
```
