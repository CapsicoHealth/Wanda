# Payment / Credits — App Integration Touchpoints

This is the practical, "what do I actually call" companion to
`payment_infra_simple_enhancement_plan.md`. That document explains the design and the *why*; this one
is for an application developer wiring a metered feature into the credit system.

It covers exactly three touchpoints:
1. **Java — check** the balance is positive before doing metered work.
2. **Java — charge** for work done, and read back whether the balance is still positive.
3. **Front-end — pop up** the "pick a plan / top up" UI when the client is told to.

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

## 4. ⚠ Concurrency: multiple tabs, the same wallet, and deadlocks

**Status: the lost-update risk described below is FIXED.** Every balance mutation
(`CreditHelper.charge`/`consume`/`grant`/`adjust`) now goes through
`UserPlanSubscription_Factory.incrementCreditsBalance(...)`, a single atomic
`UPDATE ... SET creditsBalance = creditsBalance + ?` built with `tilda.db.QueryHelper.setIncrement()` —
not the traditional `getCreditsBalance()` → compute in Java → `setCreditsBalance()`+`write()` chain. The
rest of this section explains the problem that fix removes, and what is still worth knowing.

### 4.1 The problem this replaces (no longer present, kept for context)

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

### 4.2 How the fix works

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

### 4.3 When this becomes an actual deadlock rather than a lost update

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

### 4.4 What is still NOT covered

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

---

## 5. Quick reference

```java
// Entry gate — read-only
CreditHelper.check(C, U, productId).isOK()

// Exit charge — always applies, may go negative
CreditHelper.charge(C, U, productId, cost, reference)

// Strict pre-authorization — refuses if insufficient
CreditHelper.consume(C, U, productId, cost, reference)
```

```javascript
// Front-end top-up, reusing the whole plan-picker + PayPal flow
FloriaLogin.PopupLogin.topUpCredits(productId, onComplete)
```
