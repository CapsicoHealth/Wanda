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

Endpoints (already renamed to the tightened convention):

- `InviteOrgUser` — `POST /svc/wanda/organizations/invite/create`
- `ListOrgInvites` — `GET /svc/wanda/organizations/invite/list`
- `UpdateOrgInvite` — `POST /svc/wanda/organizations/invite/update` (consolidated accept/decline/cancel via an `action` param: `accept` | `decline` | `cancel`)
- `OrganizationACLCreate/Delete/List`, `OrganizationCreate/Delete/Details/List`, `PromoCodeUsage` — all under `/svc/wanda/organizations/...` (redundant `organization/` segment removed)
- `Project*` servlets already used a tight `/svc/wanda/project/...` convention — left unchanged.

Front-end: `FloriaJS/WebContent/floria.v2.0/module-login.js` (`FloriaLogin.PopupOrganizations`) — invite
form has first/last name (now required, persisted directly on `OrganizationInvite.inviteeNameFirst/Last`
so they survive cancel+resend), default role "Reader", Access Control tab has a CTA that jumps to Manage
Invitations and auto-opens the invite form.

### Two invite scenarios (as implemented)

**Scenario A — existing, already-registered user** (`InviteOrgUser` finds a `User` row for the email):
- A token-based `OrganizationInvite` is created; invitee gets an email with accept/decline links.
- Resolved via `UpdateOrgInvite` (`action=accept` or `action=decline`), gated by: caller authenticated
  **and** `invite.getInviteeRefnum() == U.getRefnum()`.
- Declining just sets `invite.setStatusDeclined()`. No impact on the (pre-existing) `User` account —
  **no dangling-user risk here**, since the account predates the invite entirely.

**Scenario B — brand-new user, no account yet:**
- `InviteOrgUser` calls `User_Data.inviteUserForOrg(C, email, orgTitle, ownerPromoCode, nameFirst, nameLast)`
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

1. **`InviteOrgUser.java`** (Scenario B branch) → `User_Data.inviteUserForOrg(...)` sets `U.promoCode`
   unconditionally at invite-creation time, before the invitee has consented to anything.
2. **`UserOnBoarding.java`** auto-accepts every pending invite matching the registering email — no
   decline option exists for Scenario B at all (the only decline path, `UpdateOrgInvite`, requires a
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
   - `UpdateOrgInvite`'s `cancel` action (invite rescinded before ever being joined/accepted) — currently
     leaves the already-created Scenario B `User` row untouched; this is arguably the simplest/first case
     to fix, since the user never even joined anything.
   - (Possible future) a self-service "leave organization" action, if/when that's added — same trigger.
3. **What happens at counter == 0?**
   - Clear `U.promoCode` (`setNullPromoCode()`) — reclaims the `maxUsers` slot.
   - Whether to go further (lock/deactivate the `User` account entirely, especially if `getLoginCount()==0`
     i.e. they never even completed onboarding) is a separate, more aggressive option to decide on.
4. **Scenario B `cancel` before acceptance** (today: `UpdateOrgInvite.cancel()` marks the invite Cancelled
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

## 5. Suggested next steps when resuming this work

1. Decide the exact counter semantics (§4.1) and where it lives (`User` table column(s) vs. derived from
   live `OrganizationACL` counts at read time — the latter avoids drift but costs a query; the former is
   cheaper but needs careful maintenance at every mutation point).
2. Implement the simplest/least ambiguous slice first: reclaim the promo code (and consider deleting the
   never-onboarded stub `User`/`UserDetail` outright) when a Scenario B invite is **cancelled** before the
   invitee ever registers (`UpdateOrgInvite.cancel`, when `invite.getInviteeRefnum()`'s `User` row has
   `getLoginCount()==0` and `isInvitedUser()==true`).
3. Extend to the harder case: `OrganizationACLDelete` (member removed *after* having joined) — requires
   the "still depends solely on this org's invite" check described in §4.1 before reclaiming.
4. Leave the EULA/Plan gating alone — confirmed already correctly generic and unaffected (§3).
5. Leave the auto-join-on-onboarding UX alone — confirmed as an intentional, desired product decision, not
   a bug (§4).
