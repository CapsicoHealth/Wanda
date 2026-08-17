# Organization Invite — Promo-Code Lifecycle & "Dangling User" Follow-Up

**Status:** Discussion / design only. No code changes made as a result of this conversation.
**Relationship to `org-invite.md`:** That earlier doc was the original design plan for the org-invite
feature (session-overlay entitlement model). The feature was ultimately implemented differently/more
simply than that plan (see "What actually shipped" below). This doc captures a **follow-up design
conversation** about a gap discovered in the shipped implementation: promo-code entitlements handed out
via org invites can become permanent/"dangling" even when the underlying org membership never
materializes or is later removed. **Revisit this before shipping any promo-code/billing enforcement work
on top of the org-invite feature.**

---

## 1. What actually shipped (current behavior, as of this writing)

Endpoints (already renamed to the tightened convention; servlet classes have since also been renamed for
consistency — `InviteOrgUser`→`OrganizationInviteCreate`, `ListOrgInvites`→`OrganizationInviteList`,
`UpdateOrgInvite`→`OrganizationInviteUpdate` — same logic, new names only):

- `OrganizationInviteCreate` — `POST /svc/wanda/organizations/invite/create`
- `OrganizationInviteList` — `GET /svc/wanda/organizations/invite/list`
- `OrganizationInviteUpdate` — `POST /svc/wanda/organizations/invite/update` (consolidated accept/decline/cancel via an `action` param: `accept` | `decline` | `cancel`)
- `OrganizationACLCreate/Delete/List`, `OrganizationCreate/Delete/Details/List`, `PromoCodeUsage` — all under `/svc/wanda/organizations/...` (redundant `organization/` segment removed)
- `Project*` servlets already used a tight `/svc/wanda/project/...` convention — left unchanged.

Front-end: `FloriaJS/WebContent/floria.v2.0/module-login.js` (`FloriaLogin.PopupOrganizations`) — invite
form has first/last name (now required, persisted directly on `OrganizationInvite.inviteeNameFirst/Last`
so they survive cancel+resend), default role "Reader", Access Control tab has a CTA that jumps to Manage
Invitations and auto-opens the invite form.

### Two invite scenarios (as implemented)

**Scenario A — existing, already-registered user** (`OrganizationInviteCreate` finds a `User` row for the email):
- A token-based `OrganizationInvite` is created; invitee gets an email with accept/decline links.
- Resolved via `OrganizationInviteUpdate` (`action=accept` or `action=decline`), gated by: caller authenticated
  **and** `invite.getInviteeRefnum() == U.getRefnum()`.
- Declining just sets `invite.setStatusDeclined()`. No impact on the (pre-existing) `User` account —
  **no dangling-user risk here**, since the account predates the invite entirely.

**Scenario B — brand-new user, no account yet:**
- `OrganizationInviteCreate` calls `User_Data.inviteUserForOrg(C, email, orgTitle, ownerPromoCode, nameFirst, nameLast)`
  **immediately**, at invite-creation time (not at accept time):
  - Creates a real `User` row, `setInvitedUser(true)`.
  - **Sets `U.promoCode = ownerPromoCode` right away**, before the invitee has done anything at all —
    not even registered yet.
  - Creates `UserDetail`, sends a registration email.
- The `OrganizationInvite` created alongside is **tokenless** (matched later purely by e-mail).
- `UserOnBoarding` (`/svc/user/onboarding`, called when the invitee finally sets a password): looks up all
  still-pending invites matching the new user's email and **unconditionally auto-accepts every one** —
  creates the `OrganizationACL` and marks the invite Accepted. **There is no decline/opt-out step in the
  registration flow itself.**

---

## 2. The gap: promo-code entitlement can outlive the thing that justified it

Traced in code (all confirmed by reading the actual sources, not assumed):

1. **`OrganizationInviteCreate.java`** (Scenario B branch) → `User_Data.inviteUserForOrg(...)` sets `U.promoCode`
   unconditionally at invite-creation time, before the invitee has consented to anything.
2. **`UserOnBoarding.java`** auto-accepts every pending invite matching the registering email — no
   decline option exists for Scenario B at all (the only decline path, `OrganizationInviteUpdate`, requires a
   `token`, and Scenario B invites are tokenless by design).
3. **`OrganizationACLDelete.java`** (admin removes a member from an org) only soft-deletes the
   `OrganizationACL` row (`acl.setDeletedNow()`) — it **never touches `User.promoCode`**.
4. **`Promo_Data.hasReachedMaxUsers` → `countBoundUsers` → `countActiveUsers` →
   `User_Factory.countUsersByPromoCode`**:
   ```sql
   select count(*) from "..." where "promoCode" = ? and "deleted" is null
   ```
   This **only** filters on `promoCode` + non-deleted — it does **not** check `invitedUser`, org
   membership, ACL status, or login history at all.

**Net effect:** the moment an org Admin invites a brand-new email address, a real `User` row is created
and permanently bound to the inviting org owner's promo code (consuming one of its `maxUsers` slots)
— regardless of whether that person ever completes registration, ever joins the org, or is later removed
from it. There is currently no mechanism that reclaims that promo-code slot.

Practical risk: an org Admin can mint arbitrary numbers of promo-coded accounts just by inviting emails
to their org (even ones never intended to actually join), since acceptance is not required for the
`User`+`promoCode` to be created, and removal from the org never revokes the promo code.

## 3. EULA — confirmed NOT affected by this gap

Traced separately and confirmed: the EULA gate (`SessionFilter.checkAccountRequirements()` /
`SessionUtil.Attributes.EULA_CLEAR`) is completely generic and origin-agnostic. It runs on every
authenticated request (except `authPassthroughs`/API-key calls), regardless of how the `User` account was
created. `UserOnBoarding` doesn't touch `EULA_CLEAR` at all — it just registers the account and
auto-joins the org(s). The next time the user logs in via `Login`/SSO, `EulaHelper` gates them exactly
like any stand-alone signup. **Invited users get no EULA exemption.** (Same is true of `PLAN_CLEAR` /
plan-selection gating.)

## 4. Design direction agreed so far (still to be finalized)

User's stance (this conversation):
- **Keep the auto-join UX as-is** — don't add friction / a decline step to the onboarding flow for
  Scenario B. The simplicity is a deliberate product choice.
- **Keep "invited user inherits inviter's/owner's promo code" as the rule** — that's the correct, intended
  entry point for that user's entitlement.
- **But**: there needs to be a **garbage-collection-style mechanism** — a flag + counter — so that once a
  user's *only* justification for holding that promo code is fully gone (i.e., they were removed from
  every org they were auto-joined into via invite, and never independently established their own
  entitlement), the promo code (and by extension, presumably platform access) is revoked. "Eventually the
  counter is set to 0 and they lose all access."

### Open design questions for the counter/GC mechanism (not yet resolved)

1. **What exactly does the counter count?**
   - Option A: a single `inviteOriginCount` (or reuse `invitedUser` boolean + a live count of currently-
     active, invite-derived `OrganizationACL` rows) — decremented whenever one of those ACLs is removed.
   - Needs to distinguish "was auto-provisioned via invite and still depends on it" from "has since
     established independent standing" (e.g., completed their own paid plan, has a stand-alone promo
     signup unrelated to the org, etc.) — the latter should be permanently exempt from GC regardless of
     org membership changes.
2. **What triggers a decrement / GC check?**
   - `OrganizationACLDelete` (admin removes a member).
   - `OrganizationInviteUpdate`'s `cancel` action (invite rescinded before ever being joined/accepted) — currently
     leaves the already-created Scenario B `User` row untouched; this is arguably the simplest/first case
     to fix, since the user never even joined anything.
   - (Possible future) a self-service "leave organization" action, if/when that's added — same trigger.
3. **What happens at counter == 0?**
   - Clear `U.promoCode` (`setNullPromoCode()`) — reclaims the `maxUsers` slot.
   - Whether to go further (lock/deactivate the `User` account entirely, especially if `getLoginCount()==0`
     i.e. they never even completed onboarding) is a separate, more aggressive option to decide on.
4. **Scenario B `cancel` before acceptance** (today: `OrganizationInviteUpdate.cancel()` marks the invite Cancelled
   but leaves the stub `User`+promoCode dangling forever) is likely the cleanest first slice to implement,
   since there's no ambiguity — the user never joined anything, so there's nothing to weigh against.
5. **Decline is still not possible for Scenario B at all** (separate issue from the GC counter, still
   flagged as a gap): onboarding force-accepts every pending invite unconditionally. If a genuine "no
   thanks" decline path is ever wanted for brand-new invitees (as opposed to just relying on the org Admin
   to `cancel` on their behalf, or the invitee simply leaving the org after joining), that would need
   either (a) issuing a token for Scenario B invites too so the existing accept/decline flow can be reused
   post-registration, or (b) surfacing the pending invite(s) in the onboarding response and requiring an
   explicit follow-up call instead of auto-accepting. **Per this conversation, this is explicitly NOT
   wanted right now** — auto-join UX is to be kept as-is.

## 5. Solution idea: `WANDA.User.orgAccessCounter` (proposed 2026-08-16)

This is the current leading design candidate — captured here verbatim as a thread to resume, not yet
validated against every edge case or implemented.

### Core mechanism

Add a counter column directly on `WANDA.User` (working name: `orgAccessCounter`, `int`, not nullable,
default per creation path as below):

- **User created via a promo-code signup** (stand-alone, not org-invite driven) **or** via the
  pre-existing "system/admin invite" path (`User_Data.inviteUser(...)` — the *original*, non-org invite
  mechanism, e.g. platform-level admin invites) → **`orgAccessCounter` starts at `1`**.
  - Rationale: these users have an independent, non-org-revocable justification for platform access.
    Starting at `1` (not incrementable-from-`0` only by org joins) means their counter can **never**
    reach `0` purely from org membership churn — joining/leaving orgs can move it up but a
    corresponding leave can only bring it back down to its floor of `1`, never below. This is the crux
    of the design: **the starting value itself encodes "how they got in," and that origin can never be
    fully undone by later org membership changes alone.**
- **User created via `User_Data.inviteUserForOrg(...)`** (Scenario B org-invite, brand-new email, no
  pre-existing account) → **`orgAccessCounter` starts at `0`**.
  - This is the "fully dependent on org membership" starting state — they have no independent
    justification for access yet.

### Increment / decrement rules

- **Increment by 1** every time a user *joins* an organization, whether:
  - auto-joined at `UserOnBoarding` time (Scenario B, brand-new account completing registration), or
  - explicitly accepted (Scenario A, pre-existing account accepting via `OrganizationInviteUpdate`
    `action=accept`).
  - (Open question: should this also increment for a *pre-existing* user joining via Scenario A even
    though their counter already started at `1`? Current thinking: **yes** — every active org
    membership should count, so their floor of `1` plus however many orgs they're currently in gives
    the true "how many independent reasons does this user have to still have access" tally. This also
    naturally handles a promo/admin-invited user who joins **multiple** orgs and later leaves some but
    not all — see Worked Examples below.)
- **Decrement by 1** every time a user is *removed* from an organization (admin-initiated
  `OrganizationACLDelete`, or a future self-service "leave org" action).
  - Should **not** decrement on `OrganizationInviteUpdate` `action=cancel` or `action=decline` if the
    user never actually joined (i.e., no corresponding increment ever happened for that org) — only
    actions that reverse an actual, previously-counted join should decrement. (This still leaves the
    already-separately-flagged Scenario-B-cancel-before-registration case as its own cleanup path — see
    §6.2 — since that user's counter never even reached the "joined" increment in the first place; the
    stub `User`/promoCode cleanup there is independent of this counter mechanism.)

### At `orgAccessCounter == 0`

- The user's access control is **removed / reset to some baseline** (exact baseline TBD — e.g., clear
  `roles`, clear `promoCode`, revoke all `AppUser` entries — whichever combination constitutes "no
  meaningful platform access" in the current role/entitlement model).
- **An email is sent** notifying them of the change.
- **They can still log in** (account is not locked/deleted) — they simply land with no access to
  anything, presumably a bare/empty landing state prompting them to request a new invite, purchase a
  plan, or use a promo code to re-establish standing.

### Why this satisfies the original "dangling user" concern

- A promo-code or system-admin-invited user's counter **can never reach `0`** through org membership
  changes alone, because their floor is `1`, not `0` — org joins/leaves can only move them between `1`
  and higher, never down to `0`. Their access is tied to their own independent entitlement, exactly as
  intended.
- A user who **only** ever came to the platform through one or more org invites (Scenario B) starts at
  `0`, gets incremented on each org join, decremented on each org removal, and **can** legitimately reach
  back down to `0` if every org membership they ever held is removed — at which point they have no
  remaining justification for access, and the system now correctly reclaims it (with notice, without
  destroying the account outright).

### Worked examples (to sanity-check before implementing)

| Scenario | Counter trace | Ends at |
|---|---|---|
| Promo-code signup, never joins any org | starts `1` | `1` (untouched) |
| System-admin invite, never joins any org | starts `1` | `1` (untouched) |
| Promo-code signup, joins OrgA, later removed from OrgA | `1` → `2` (join) → `1` (removed) | `1` — **still has access**, correct |
| Org-invite-only (Scenario B) user, joins OrgA (auto-join), never removed | `0` → `1` (join) | `1` — has access as long as membership lasts |
| Org-invite-only user, joins OrgA, later removed from OrgA | `0` → `1` (join) → `0` (removed) | `0` — **access reclaimed**, correct, this is the target fix |
| Org-invite-only user, joins OrgA AND OrgB, removed from OrgA only | `0` → `1` (join A) → `2` (join B) → `1` (removed A) | `1` — still has access via OrgB, correct |
| Org-invite-only user, joins OrgA, removed, later independently buys a plan/promo | `0` → `1` → `0` (removed, access reset) → then independent signup would need its own path to set counter back to a `1`-equivalent floor | **needs a defined "re-establish independent standing" path** — not yet designed (see open questions below) |

### Open questions still to resolve before implementing this design

1. **Does a pre-existing (promo/admin) user's counter really need to increment on org-join at all?**
   Alternative simpler design: only Scenario-B-originated users are tracked by this counter at all
   (i.e., don't bother incrementing/decrementing for users who already started at `1`+, since it's
   provably a no-op for the "can they reach 0" question as shown in the worked examples above — their
   floor already guarantees non-zero). This would avoid touching `OrganizationACLDelete`/accept logic for
   the promo/admin-invited case entirely, reducing the blast radius of this change. **Leaning towards this
   simplification, but needs confirmation** — the increment-for-everyone version above is more uniform
   /simpler to reason about but touches more code paths for no behavioral difference.
2. **"Re-establish independent standing" path**: if an org-invite-only user (counter reached `0`, access
   reset) later independently obtains a promo code or is separately/system-invited, how does the counter
   (and previously-cleared access) get restored? Presumably any of those independent paths should re-set
   the counter to a `1` floor (same as a brand-new independent signup) rather than just incrementing from
   `0`, and re-provision access. Not yet designed.
3. **Exact "baseline" to reset to at `counter==0`**: clear `roles` entirely? Clear `promoCode`? Both?
   Leave `UserDetail`/profile data untouched (presumably yes, so nothing is lost if they regain access
   later)? Needs to be enumerated precisely against the current role/entitlement model.
4. **Where does the counter live / how is it maintained?** A denormalized column on `WANDA.User`
   (`orgAccessCounter int not null default 0`) that's incremented/decremented transactionally alongside
   every `OrganizationACL` create/soft-delete and every invite accept, vs. computed on-demand from a live
   count of active `OrganizationACL` rows plus a separate `hasIndependentAccess boolean` flag. The
   denormalized-counter version (as specified by the user) is simpler to query/gate on cheaply at
   `SessionFilter`-level on every request if ever needed, at the cost of needing careful maintenance at
   every mutation point (a classic reference-counting/GC hazard — must audit *every* code path that
   creates/removes an `OrganizationACL` row, including any bulk/admin tooling, migrations, etc., to keep
   it consistent).
5. **Email notification content/template**: new `wanda.config.json` text block needed (following the
   existing `orgInvite*Texts` pattern) — not yet drafted.
6. **Interaction with the still-separately-flagged Scenario-B-cancel-before-registration cleanup** (§6.2):
   that case never reaches the increment step at all (user never joined), so it remains a distinct,
   simpler cleanup (delete stub `User`/clear `promoCode` outright) rather than something this counter
   mechanism handles — the two are complementary, not overlapping.

## 6. Suggested next steps when resuming this work

1. Decide between the reference-counter design in §5 and/or the simpler per-mutation-point cleanup
   originally sketched below — they are complementary, not mutually exclusive (§5.6).
2. Resolve the open questions in §5 (esp. #1 simplification, #2 re-establish-standing path, #3 exact
   baseline) before writing any code.
3. Implement the simplest/least ambiguous slice first: reclaim the promo code (and consider deleting the
   never-onboarded stub `User`/`UserDetail` outright) when a Scenario B invite is **cancelled** before the
   invitee ever registers (`OrganizationInviteUpdate.cancel`, when `invite.getInviteeRefnum()`'s `User`
   row has `getLoginCount()==0` and `isInvitedUser()==true`). This is independent of, and should land
   before, the full §5 counter mechanism.
4. Extend to the harder case: `OrganizationACLDelete` (member removed *after* having joined) — implements
   the §5 counter decrement + baseline-reset-at-zero logic.
5. Leave the EULA/Plan gating alone — confirmed already correctly generic and unaffected (§3).
6. Leave the auto-join-on-onboarding UX alone — confirmed as an intentional, desired product decision, not
   a bug (§4).
