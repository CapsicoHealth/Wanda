# Organisation Invitation — Full Implementation Plan

**Status:** Data model change complete. All other work pending.  
**Data model change:** `OrganizationInvite` object added to `_tilda.Wanda.json` and codegen must be re-run before proceeding.  
**Scope:** Server-side only. Front-end is a separate workstream.

---

## 1. Design Principles

- **Two distinct invitation scenarios** drive the entire design (see §3).
- **Entitlements are always live, never snapshotted.** The org owner's `promoCode → Promo_Data` is read at the moment a new user is created. If a super-admin later modifies the promo, all subsequent invites pick up the change automatically.
- **Org-context switching is session-level, not schema-level.** When a user selects an org, `SessionFilter` overlays the org owner's promo roles/promoCode on the in-memory `User_Data` object before passing it to any servlet. No per-org entitlement columns exist in `OrganizationACL`.
- **`OrganizationACL` remains a resource-permission table only** (O/A/W/R roles governing access to the org resource). App-level entitlements are derived from the org owner's promo at session-switch time.
- **Inviter auth rule:** Any org ADMIN or OWNER may send invites, but the entitlement configuration (promoCode → roles/apps/contents) **always originates from the OWNER** (`Organization.creatorRefnum → User → Promo_Data`), never from the inviting ADMIN.
- **Role ceiling — ADMIN and OWNER have equivalent operational power.** There is no functional difference between `A` and `O` in terms of what a member can *do* within the org (both can invite, cancel invites, and grant up to `A`). The **only** thing that distinguishes `O` is that it is a **singleton per organization** — exactly one member holds it at a time. Consequently:
  - Both ADMIN and OWNER may grant `A`, `W`, or `R` via invite.
  - **`O` is never grantable via the regular invite flow**, by anyone, regardless of caller role. Installing a new owner (and correspondingly demoting the previous one, e.g. to `A`) is a separate, not-yet-designed **transfer-ownership workflow** (out of scope here — see §12).
  - **Known pre-existing gap (not part of this workstream):** the existing `OrganizationACLCreate` servlet (`/svc/wanda/organizations/acl/create`) currently allows any ADMIN to grant `O` directly with no ceiling, which can violate the singleton-owner invariant. This should be closed off when the transfer-ownership workflow is designed, not as part of this invite workstream.
- **Multi-tenant is out of scope.** Multi-tenancy as implemented in `SessionFilter` (`TenantDbUser`/`TenantConnection`) is not currently in active use, and the Organization concept was not designed with it in mind. The org-context overlay (§4) applies only to `MasterDbUser` / single-tenant request paths; no `TenantDbUser` handling is included or implied.
- **Payment logic excluded** from this workstream deliberately. The `Promo.plans` and billing fields on the owner's promo are ignored during org invite provisioning.

---

## 2. Data Model (DONE)

### 2a. `OrganizationInvite` — NEW TABLE

Added to `_tilda.Wanda.json` between `OrganizationACL` and `Project`. Codegen required.

| Column | Type | Notes |
|--------|------|-------|
| `organizationRefnum` | FK→Organization | invariant |
| `inviterRefnum` | FK→User | invariant — the admin/owner who sent the invite |
| `inviterId` | denorm User.id | |
| `inviteeEmail` | STRING(255) | invariant — canonical lookup key before User record exists |
| `inviteeRefnum` | FK→User nullable | null until User account is created (Scenario B) or until existing user is identified (Scenario A) |
| `inviteeUserId` | denorm User.id nullable | null until account exists |
| `role` | CHAR | A/W/R only — values: Admin, Writer, Reader |
| `inviteToken` | STRING(20) nullable | one-time token for Scenario A accept/decline link; null for Scenario B |
| `inviteTokenCreated` | DATETIME nullable | for TTL enforcement |
| `status` | STRING(2) | PE(Pending)/AC(Accepted)/DC(Declined)/CN(Cancelled)/EX(Expired); default PE |

**Indices:**

| Name | Columns | Unique? | Partial where |
|------|---------|---------|---------------|
| `OrgEmailPending` | organizationRefnum, inviteeEmail | **YES** | `status='PE' and deleted is null` |
| `InviteToken` | inviteToken | **YES** | `inviteToken is not null and deleted is null` |
| `InviteePending` | inviteeRefnum | no | `status='PE' and deleted is null` |
| `OrgPending` | organizationRefnum | no | `status='PE' and deleted is null`, orderBy created desc |

**Queries generated:**

- `OrgEmailPending(organizationRefnum, inviteeEmail)` — duplicate-invite check
- `ByToken(inviteToken)` — accept/decline link lookup
- `ByInviteePending(inviteeRefnum)` — UserOnBoarding auto-join (Scenario B)
- `ByOrgPending(organizationRefnum)` — admin listing

### 2b. No changes to `OrganizationACL`

The existing `OrganizationACL` schema (O/A/W/R resource-level role) is used as-is. ACL records are created only when an invite is definitively accepted (Scenario A) or automatically on onboarding completion (Scenario B).

---

## 3. Two Invitation Scenarios

### Scenario A — Existing Active User

**Trigger:** `inviteOrgUser` called; target email found in `User` table with `loginCount > 0`.

```
InviteOrgUser
  → check: user is already a member? → error
  → check: pending invite already exists? → resend (regenerate token)
  → create OrganizationInvite (status=PE, inviteeRefnum=user.refnum,
      inviteToken=random 18-char token, inviteTokenCreated=now)
  → send Email A ("you've been invited to join [org]") to invitee

AcceptOrgInvite (invitee clicks link)
  → load OrganizationInvite by token → validate status=PE, token not expired,
      logged-in user matches inviteeRefnum
  → create OrganizationACL (organizationRefnum, inviteeRefnum, role, inviterRefnum)
  → mark OrganizationInvite status=AC, clear inviteToken

DeclineOrgInvite (invitee clicks decline link)
  → load OrganizationInvite by token → validate as above
  → mark OrganizationInvite status=DC, clear inviteToken
```

### Scenario B — New / Unregistered User

**Trigger:** `inviteOrgUser` called; target email NOT found in `User` table, OR found with `loginCount == 0`.

```
InviteOrgUser
  → resolve entitlements from org OWNER's promo (live read):
      owner = User_Factory.lookupByPrimaryKey(org.creatorRefnum)
      promo = Promo_Factory.lookupByCode(owner.promoCode)  [if non-null and active]
      → roles[], apps[], contents[]
      fallback if no promo: owner.getRolesAsArray(), AppUser table for apps
  
  If user does NOT exist:
    → User_Data.inviteUser(C, promoCode, null, null, email, firstName, lastName,
          roles, tenantRefnums, appRefnums, contents, null)
      [creates User + UserDetail + TenantUser + AppUser, fires invite email]
    → look up newly created User by email to get refnum
  
  If user exists but loginCount == 0 (stuck in limbo):
    → User_Data.updateDetailsAndInvite(C, user, ...) [re-issues token, re-sends email if loginCount==0]
  
  → create OrganizationInvite (status=PE, inviteeRefnum=newUser.refnum,
        inviteToken=null, inviteTokenCreated=null)
      [no separate accept needed — org join fires automatically at onboarding]

UserOnBoarding (end of justDo, after U.write(C)):
  → List<OrganizationInvite_Data> pending =
        OrganizationInvite_Factory.lookupWhereByInviteePending(C, U.getRefnum(), 0, -1)
  → for each invite:
      create OrganizationACL (organizationRefnum, U.getRefnum(), U.getId(),
                               invite.getRole(), invite.getInviterRefnum(), invite.getInviterId())
      mark invite status=AC
      upsert/write both
```

---

## 4. Org-Context Switching (Session-Level Switcharoo)

This is the mechanism that allows a user to "switch" into an org context, dynamically overlaying the org owner's entitlements onto the in-memory `User_Data` without any DB writes.

### 4a. `SessionUtil` additions

Add `ACTIVE_ORG_REFNUM` to the `SessionUtil.Attributes` enum.

```java
// In SessionUtil.Attributes:
ACTIVE_ORG_REFNUM
```

### 4b. `SessionFilter.doFilter()` — overlay block

**Precise insertion point:** immediately after the locked-check / `UD` block (i.e., right after `MasterDbUser.setUserDetail(UD)`, the end of the `else` block that starts at the current `isUserLocked(MasterDbUser)` check) and **before** the `isAppAuthorized(request, MasterConnection, MasterDbUser)` call. This ordering is critical: `isAppAuthorized()` (§4c) must see the org-overlaid roles/`activeOrgRefnum` on `MasterDbUser` to compute the correct app-authorization list. Inserting it any later (e.g., merely "before `request.setAttribute(USER, mainUser)`") would place it after `isAppAuthorized()` has already run and silently break org-context app authorization.

```java
HttpSession S = SessionUtil.getSession(request);
Long activeOrgRefnum = (Long) S.getAttribute(SessionUtil.Attributes.ACTIVE_ORG_REFNUM.name());
if (activeOrgRefnum != null && MasterDbUser != null)
  {
    Organization_Data org = Organization_Factory.lookupByPrimaryKey(activeOrgRefnum);
    if (org.read(MasterConnection) == true && org.isNullHardDeleted() == true)
      {
        User_Data orgOwner = User_Factory.lookupByPrimaryKey(org.getCreatorRefnum());
        if (orgOwner.read(MasterConnection) == true)
          {
            String ownerPromoCode = orgOwner.getPromoCode();
            if (TextUtil.isNullOrEmpty(ownerPromoCode) == false)
              {
                Promo_Data promo = Promo_Factory.lookupByCode(ownerPromoCode);
                if (promo.read(MasterConnection) == true && promo.isActiveAndValid() == true)
                  {
                    MasterDbUser.setRoles(new HashSet<>(Arrays.asList(promo.getRolesAsArray())));
                    MasterDbUser.setPromoCode(promo.getCode());
                    // apps are handled in isAppAuthorized() and ConfigServlet
                  }
                else
                  {
                    // promo inactive/missing — fall through to owner's raw roles
                    MasterDbUser.setRoles(new HashSet<>(Arrays.asList(orgOwner.getRolesAsArray())));
                    MasterDbUser.setNullPromoCode();
                  }
              }
            else
              {
                // no promo on owner — use owner's raw roles
                MasterDbUser.setRoles(new HashSet<>(Arrays.asList(orgOwner.getRolesAsArray())));
              }
          }
        // Signal org context to all downstream code (servlets, ACL checks, etc.)
        MasterDbUser.setAlternateRefnum(activeOrgRefnum);
      }
    else
      {
        // Org gone or deleted — clear the session attribute
        S.removeAttribute(SessionUtil.Attributes.ACTIVE_ORG_REFNUM.name());
      }
  }
```

> **Note:** `MasterDbUser.setAlternateRefnum()` already exists on `User_Data` and is the hook for "operating in a specific resource context". Servlets can call `U.getAlternateRefnum()` to know which org is active.

### 4c. `SessionFilter.isAppAuthorized()` — org-context branch

`isAppAuthorized()` currently queries `AppUserView_Factory.getUserApps()` (which reads `AppUser` table — global, not org-specific), caching the result in `_USER_APPS_CACHE` (`Cache<Long, String[]>` keyed by `userRefnum`, `expireAfterWrite(5, MINUTES)`, no explicit invalidation on `Promo` edits today — see `PromoCreate.java`, which writes the `Promo` record but never calls any cache eviction; the system already tolerates up to 5 minutes of staleness for promo-driven role/app changes by design).

**Important simplification:** because entitlements in org context are always inherited from the org OWNER (never per-invited-member — see §1), the org-context app-path list depends only on **which org** is active, not on which member is viewing it. This means a composite `(userRefnum, orgRefnum)` cache key or nested per-user cache is unnecessary. Instead:

- Leave `_USER_APPS_CACHE` completely unchanged (still keyed by `userRefnum` alone, still used for the non-org path).
- Add a second, independent flat cache:
  ```java
  static private Cache<Long, String[]> _ORG_APPS_CACHE = CacheBuilder.newBuilder().maximumSize(200).expireAfterWrite(5, TimeUnit.MINUTES).build();

  public static void evictOrgFromAppCache(long organizationRefnum)
    {
      _ORG_APPS_CACHE.invalidate(organizationRefnum);
    }
  ```
  keyed by `organizationRefnum`, built from the org owner's promo apps, shared across all members of that org. Same TTL-based staleness tolerance as `_USER_APPS_CACHE` — no new explicit invalidation-on-promo-edit hook is required (consistent with existing behavior).
- In `isAppAuthorized()`: if `U.getAlternateRefnum() != SystemValues.EVIL_VALUE` (org context active), consult/populate `_ORG_APPS_CACHE.get(U.getAlternateRefnum())` instead of `_USER_APPS_CACHE`.
- `evictUserFromAppCache(long userRefnum)` needs **no signature change**.

When `activeOrgRefnum != null` and the request is for a `.jsp`:
```java
// Instead of AppUserView_Factory.getUserApps(), build paths from org owner's promo apps
Promo_Data promo = /* load owner's promo as above */;
Long[] promoAppRefnums = promo != null ? promo.getAppsAsArray() : null;
// Then load AppView records for those refnums to get their paths
```

If the promo has no app list (null `apps`), fall back to the standard `AppUserView_Factory.getUserApps()` call.

### 4d. `ConfigServlet` — org-context app list

`ConfigServlet` currently calls `AppUserView_Factory.getUserApps(C, U, U.getRefnum(), 0, -1)`.

When `U.getAlternateRefnum() != SystemValues.EVIL_VALUE` (i.e., in org context), the app list should instead be built from the org owner's promo:

```java
List<AppUserView_Data> AUVL;
if (U != null && U.getAlternateRefnum() != SystemValues.EVIL_VALUE)
  {
    // Org context: build app list from owner's promo
    AUVL = buildOrgContextAppList(C, U.getAlternateRefnum());
  }
else
  {
    // Standard: global AppUser table
    AUVL = U == null ? null : AppUserView_Factory.getUserApps(C, U, U.getRefnum(), 0, -1);
  }
```

`buildOrgContextAppList()` is a private helper that loads org → owner → promo → `promo.getAppsAsArray()` → filters the active `AppView` list to those refnums.

> **Important:** Since `U` already has org-context roles overlaid by `SessionFilter`, the `currentUser` block in `ConfigServlet` (`U.getUserDetails()`, `U` itself) is automatically correct — no extra handling needed there. Only the app list needs explicit org-context logic.

---

## 5. New Servlets

All new servlets go under `wanda/servlets/organization/`.

### 5a. `SetSelectedOrgServlet` — `/svc/wanda/organizations/select` (POST, authenticated)

**Purpose:** Sets or clears the active org context in the session. The client calls this when the user picks an org from a UI, then re-calls `/svc/config` to get the updated app list and user context.

**Parameters:**
- `organizationRefnum` (long, optional — absent or 0 clears org context)

**Logic:**
```
1. If organizationRefnum > 0:
   a. Load Organization — throw NotFoundException if not found / hard-deleted.
   b. checkOrganizationAcl(C, U, organizationRefnum, READER)
      → ensures the caller has at least READER access.
   c. session.setAttribute(ACTIVE_ORG_REFNUM, organizationRefnum)
2. Else:
   a. session.removeAttribute(ACTIVE_ORG_REFNUM)
3. Return success JSON: { "organizationRefnum": <long or null> }
```

> **Note:** No cache eviction call is needed here. `_ORG_APPS_CACHE` (§4c) is keyed by `organizationRefnum`, not by user — since org-context entitlements are shared across all members of an org (inherited from the owner), switching which org a user is looking through doesn't invalidate anything user-specific; it just changes which cache bucket `isAppAuthorized()` consults on the next request.

> **Analogy:** This is the org equivalent of the existing `GetSelectedTenantServlet` pattern.

---

### 5b. `InviteOrgUser` — `/svc/wanda/organizations/invite` (POST, authenticated)

**Parameters:**
- `organizationRefnum` (long, required)
- `email` (string, required)
- `firstName` (string, required)
- `lastName` (string, required)
- `role` (char, required — must be A, W, or R)

**Logic:**
```
1. Validate: role must be A, W, or R (reject O — O is never grantable via invite; see §1).
2. Canonicalise email to lowercase.
3. checkOrganizationAcl(C, U, organizationRefnum, ADMIN)
   → caller must be ADMIN or OWNER (both have equivalent power here — see §1).
   No further role-ceiling check is needed: an ADMIN caller may grant A, W, or R,
   same as an OWNER caller. The only universal restriction is that O can never be
   granted via this endpoint, by anyone.
4. Load org (Organization_Factory.lookupByPrimaryKey) to get creatorRefnum.
5. Check: is invitee already a member?
   OrganizationACL_Factory.lookupWhereOrganizationActiveByUser for this org — scan for inviteeEmail.
   OR: look up User by email → if found, check OrganizationACL for (orgRefnum, user.refnum). → error if active member.
6. Check for existing pending invite (OrgEmailPending query).
   If found: resend (regenerate inviteToken + inviteTokenCreated, re-save, re-send email).
7. Load org OWNER's entitlements:
   orgOwner = User_Factory.lookupByPrimaryKey(org.getCreatorRefnum()); orgOwner.read(C)
   promoCode = orgOwner.getPromoCode()
   promo = (promoCode != null) ? Promo_Factory.lookupByCode(promoCode) : null
   if (promo != null && promo.read(C) && promo.isActiveAndValid()):
       roles    = promo.getRolesAsArray()
       appRefnums = CollectionUtil.toPrimitiveArray(promo.getAppsAsArray())
       contents = promo.getContentsAsArray()
   else:
       roles    = orgOwner.getRolesAsArray()
       appRefnums = <load from AppUser table for orgOwner.refnum>
       contents = null
8. Look up invitee by email:

   CASE A (user exists AND loginCount > 0):
     - Create OrganizationInvite:
         OrganizationInvite_Factory.create(organizationRefnum, U.getRefnum(), U.getId(),
             email, user.getRefnum(), user.getId(), role)
         set inviteToken = EncryptionUtil.getToken(18, true)
         set inviteTokenCreated = now
         set status = PE
         invite.write(C)
     - OrganizationInvite_Data.sendOrgInviteEmailExistingUser(invite, org, U, user)

   CASE B (user does NOT exist):
     - User_Data.inviteUser(C, promoCode, null, null, email, firstName, lastName,
           roles, Wanda.getGuestRegistrationTenantRefnums(), appRefnums, contents, null)
       [creates stub User + UserDetail, fires standard invite email contextualised with org name]
     - Look up newly created user: User_Factory.lookupByEmail(email); user.read(C)
     - Create OrganizationInvite:
         OrganizationInvite_Factory.create(organizationRefnum, U.getRefnum(), U.getId(),
             email, user.getRefnum(), user.getId(), role)
         inviteToken = null, inviteTokenCreated = null
         status = PE
         invite.write(C)

   CASE B2 (user exists but loginCount == 0 — stuck unregistered):
     - User_Data.updateDetailsAndInvite(C, user, promoCode, null, null, email,
           firstName, lastName, roles, appRefnums, tenantRefnumList, oldTenantRefnums,
           contents, null)
       [re-issues pswdResetCode, re-fires invite email if loginCount==0]
     - Create OrganizationInvite as in CASE B (inviteToken=null).

9. Return success JSON: { invite refnum, status, scenario: "A" or "B" }
```

---

### 5c. `AcceptOrgInvite` — `/svc/wanda/organizations/invite/accept` (POST, authenticated)

**Scenario A only.** (Scenario B is auto-accepted in UserOnBoarding.)

**Parameters:**
- `token` (string, required)

**Logic:**
```
1. Load OrganizationInvite by token (ByToken query). 404 if not found.
2. Verify status == PE.
3. Verify the logged-in user (U.getRefnum()) matches invite.inviteeRefnum.
   (Prevents token use by a different logged-in user.)
4. Verify token not expired: OrganizationInvite_Data.isTokenExpired()
   Uses Wanda config key "orgInviteTokenTTLDays" (see §8).
5. Idempotency check: load OrganizationACL for (orgRefnum, U.getRefnum()).
   If already an active member → mark invite AC, return success.
6. Create OrganizationACL:
   OrganizationACL_Factory.create(invite.getOrganizationRefnum(), U.getRefnum(), U.getId(),
       invite.getRole(), invite.getInviterRefnum(), invite.getInviterId())
   acl.setNullDeleted(); acl.upsert(C)
7. Mark invite: invite.setStatus(AC), invite.setNullInviteToken(); invite.write(C)
8. Invalidate ACL cache: OrganizationRoleView_Factory evict for this org+user.
9. Return success.
```

---

### 5d. `DeclineOrgInvite` — `/svc/wanda/organizations/invite/decline` (POST, authenticated)

**Scenario A only.**

**Parameters:**
- `token` (string, required)

**Logic:**
```
1-4. Same as AcceptOrgInvite steps 1-4 (load, verify status, verify user, check TTL).
5. Mark invite: invite.setStatus(DC), invite.setNullInviteToken(); invite.write(C)
6. Return success.
```

---

### 5e. `CancelOrgInvite` — `/svc/wanda/organizations/invite/cancel` (POST, authenticated)

**Called by org admin/owner to rescind a pending invite.**

**Parameters:**
- `inviteRefnum` (long, required)

**Logic:**
```
1. Load OrganizationInvite by PK. 404 if not found.
2. checkOrganizationAcl(C, U, invite.getOrganizationRefnum(), ADMIN)
3. Verify status == PE.
4. Mark invite: invite.setStatus(CN), invite.setNullInviteToken(); invite.write(C)
5. NOTE: If the invitee was a Scenario B new user (loginCount == 0), their stub User
   record is intentionally LEFT as-is. They cannot log in without a password. An admin
   can clean up the User record separately via existing admin tooling.
6. Return success.
```

---

### 5f. `ListOrgInvites` — `/svc/wanda/organizations/invites/list` (GET, authenticated)

**Parameters:**
- `organizationRefnum` (long, required)
- `status` (string, optional — defaults to "PE")

**Logic:**
```
1. checkOrganizationAcl(C, U, organizationRefnum, ADMIN)
2. If status == "PE": OrganizationInvite_Factory.lookupWhereByOrgPending(C, organizationRefnum, 0, 250)
   Otherwise: query all non-deleted invites for the org with the given status.
   (A general "ByOrg" query may need to be added to the Tilda definition for non-PE statuses.)
3. Return success JSON list.
```

> **Note:** If listing by statuses other than PE is needed in practice, add a `ByOrgStatus` query to the Tilda definition: `organizationRefnum=?() and status=?() and deleted is null`.

---

## 6. Modifications to Existing Classes

### 6a. `UserOnBoarding.justDo()` — auto-join on Scenario B

At the end of `justDo()`, **after** `U.write(C)` and **before** building the JSON response, insert:

```java
// Auto-join any organisations the user was invited to before registration (Scenario B)
List<OrganizationInvite_Data> pendingOrgInvites =
    OrganizationInvite_Factory.lookupWhereByInviteePending(C, U.getRefnum(), 0, 50);
for (OrganizationInvite_Data invite : pendingOrgInvites)
  {
    OrganizationACL_Data acl = OrganizationACL_Factory.create(
        invite.getOrganizationRefnum(), U.getRefnum(), U.getId(),
        invite.getRole(), invite.getInviterRefnum(), invite.getInviterId());
    acl.setNullDeleted();
    if (acl.upsert(C) == false)
      LOG.error("Cannot create OrganizationACL for user " + U.getRefnum()
                + " org " + invite.getOrganizationRefnum() + " during onboarding.");
    invite.setStatus(OrganizationInvite_Data._statusAccepted);
    if (invite.write(C) == false)
      LOG.error("Cannot mark OrganizationInvite " + invite.getRefnum() + " as Accepted.");
  }
```

Also: add `registrationInviteOrg` flag to `AccessLog` (optional — see §9 open decisions).

### 6b. `User_Data.sendInviteEmail()` — org-context variant

Add an overload (or a new method) that accepts an optional org name to contextualise the email:

```java
public void sendInviteEmailForOrg(String orgTitle, String inviterName)
  {
    // Same structure as sendInviteEmail(), but inserts org-invite text from
    // Wanda.getOrgInviteNewUserTexts() before the "click to set password" link.
    // The link itself is unchanged: ?action=signUp&token={pswdResetCode}
  }
```

The `InviteOrgUser` servlet calls `sendInviteEmailForOrg()` for Scenario B instead of relying on `inviteUser()` to fire the standard `sendInviteEmail()`. This may require a slight refactor of `User_Data.inviteUser()` to accept an optional callback or to return the created `User_Data` and let the caller send the email.

**Simpler alternative:** add a boolean `orgContext` flag and an `orgTitle` String to `User_Data.inviteUser()` — if `orgContext == true`, use `Wanda.getOrgInviteNewUserTexts()` in the email body.

### 6c. `OrganizationInvite_Data.java` — new application class

The codegen will create `OrganizationInvite_Data.java` and `OrganizationInvite_Factory.java`. The application class needs the following custom logic:

```java
public boolean isTokenExpired()
  {
    if (isNullInviteTokenCreated())
      return true;
    long ttlDays = Wanda.getOrgInviteTokenTTLDays(); // new config key, default 7
    return ChronoUnit.DAYS.between(getInviteTokenCreated(), ZonedDateTime.now()) > ttlDays;
  }

public boolean isTokenValid(String token)
  {
    return token != null
        && token.equals(getInviteToken())
        && isNullInviteToken() == false
        && isTokenExpired() == false;
  }

public void sendOrgInviteEmailExistingUser(Organization_Data org, User_Data inviter)
  {
    // Scenario A: fire Email A in a background thread.
    // Uses Wanda.getOrgInviteExistingUserTexts().
    // Link: {hostName}{appPath}{homePagePath}?action=acceptOrgInvite&token={inviteToken}
    // Also includes a decline link: ...?action=declineOrgInvite&token={inviteToken}
  }
```

### 6d. `OrganizationRoleView_Factory` — cache invalidation

After `AcceptOrgInvite` creates a new `OrganizationACL` record, the ACL cache in `OrganizationRoleView_Factory` (`_ACL_ORGANIZATION_CACHE`) must be invalidated for `(organizationRefnum, inviteeRefnum)`. Add a static `evict(long organizationRefnum, long userRefnum)` method:

```java
public static void evict(long organizationRefnum, long userRefnum)
  {
    Cache<Long, Character> userCache = _ACL_ORGANIZATION_CACHE.getIfPresent(organizationRefnum);
    if (userCache != null)
      userCache.invalidate(userRefnum);
  }
```

---

## 7. `wanda.config.json` Additions

Following the existing pattern (e.g., `inviteUserTexts`, `resetEmailTexts`):

```json
"orgInviteExistingUserTexts": [
  "<p>You have been invited by <strong>{{inviterName}}</strong> to join the organization <strong>{{orgName}}</strong> on {{appName}}.</p>",
  "<p>Click below to accept or decline this invitation.</p>"
],
"orgInviteNewUserTexts": [
  "<p>You have been invited by <strong>{{inviterName}}</strong> to join the organization <strong>{{orgName}}</strong> on {{appName}}. Please complete your registration to accept.</p>"
],
"orgInviteTokenTTLDays": 7
```

> **Note:** The `{{variable}}` placeholders are illustrative. The actual substitution must be done in Java before calling `EMailSender.sendMailUsr()`, since the existing email infrastructure does simple string concatenation (see `User_Data.sendInviteEmail()` as the pattern).

### `Wanda.java` config reader additions

Add the following getter methods to `Wanda.java` (following existing patterns):

```java
public static List<String> getOrgInviteExistingUserTexts()  { ... }
public static List<String> getOrgInviteNewUserTexts()       { ... }
public static long         getOrgInviteTokenTTLDays()       { ... } // default 7
```

---

## 8. `SessionUtil` Additions

Add `ACTIVE_ORG_REFNUM` to the `Attributes` enum in `SessionUtil.java`.

```java
// In the Attributes enum:
ACTIVE_ORG_REFNUM
```

---

## 9. AccessLog Tracking

The `setAccessLogServletFlags()` method in `SessionFilter` records flags for specific servlets (login, guest registration, invite, payments). Consider adding:

```java
else if (req.getServletPath().equals("/svc/wanda/organizations/invite") == true)
  AL.setRegistrationInvite(flag);  // reuse existing flag, or add a new one
```

**Open decision:** reuse `registrationInvite` (which currently tracks `UserOnBoarding`) or add a new `registrationOrgInvite` field to `AccessLog`. The latter requires a schema change. Reusing `registrationInvite` is simpler and acceptable for now.

---

## 10. Implementation Order

Execute strictly in this order to avoid compile-time dependencies on ungenerated classes:

| Step | Task | File(s) |
|------|------|---------|
| **DONE** | Add `OrganizationInvite` to `_tilda.Wanda.json` | `_tilda.Wanda.json` |
| **DONE** | Run codegen, refresh workspace | — |
| 1 | Add `ACTIVE_ORG_REFNUM` to `SessionUtil.Attributes` | `SessionUtil.java` |
| 2 | Add getters to `Wanda.java` config reader | `Wanda.java` |
| 3 | Add `evict()` to `OrganizationRoleView_Factory` | `OrganizationRoleView_Factory.java` |
| 4 | Implement `OrganizationInvite_Data` application class | `OrganizationInvite_Data.java` |
| 5 | Add org-context overlay block to `SessionFilter.doFilter()` | `SessionFilter.java` |
| 6 | Update `isAppAuthorized()` in `SessionFilter` for org-context | `SessionFilter.java` |
| 7 | Update `ConfigServlet` for org-context app list | `ConfigServlet.java` |
| 8 | Implement `SetSelectedOrgServlet` | `SetSelectedOrgServlet.java` |
| 9 | Add `sendInviteEmailForOrg()` to `User_Data` | `User_Data.java` |
| 10 | Implement `InviteOrgUser` servlet | `InviteOrgUser.java` |
| 11 | Implement `AcceptOrgInvite` servlet | `AcceptOrgInvite.java` |
| 12 | Implement `DeclineOrgInvite` servlet | `DeclineOrgInvite.java` |
| 13 | Implement `CancelOrgInvite` servlet | `CancelOrgInvite.java` |
| 14 | Implement `ListOrgInvites` servlet | `ListOrgInvites.java` |
| 15 | Add auto-join block to `UserOnBoarding.justDo()` | `UserOnBoarding.java` |
| 16 | Update `wanda.config.json` with new email/TTL keys | `wanda.config.json` |
| 17 | Wire `AccessLog` flag for `InviteOrgUser` in `SessionFilter` | `SessionFilter.java` |

---

## 11. Open Decisions

These were flagged during design and must be resolved before or during implementation:

| # | Question | Recommended default |
|---|----------|---------------------|
| 1 | ~~Role ceiling for ADMINs~~ | **RESOLVED (see §1):** ADMIN and OWNER have equivalent power and may both grant up to A. Only O is never grantable via invite (reserved for the future transfer-ownership workflow). |
| 2 | **Scenario B stub cleanup on cancel:** When `CancelOrgInvite` is called for a not-yet-registered user, should their stub `User` be locked? | Leave as-is (admin cleans up via existing tooling). |
| 3 | **Inviter has no promoCode:** Fall back to owner's raw `User.roles` + `AppUser` entries? | Yes — document the fallback clearly in `InviteOrgUser`. |
| 4 | **Expired token handling (Scenario A):** Should the invitee be able to request a resend? | Yes — re-calling `InviteOrgUser` with the same email re-generates the token. No separate resend endpoint. |
| 5 | **Multiple pending org invites for a new user:** If a new user is invited to OrgA and OrgB before they register, both orgs should be joined at onboarding. | Handled — `ByInviteePending` returns a list; `UserOnBoarding` iterates all. |
| 6 | **`AccessLog` flag:** Reuse `registrationInvite` or add new flag for org invites? | Reuse `registrationInvite` for now. |
| 7 | **`SetSelectedOrgServlet` clearing:** Should switching to a new org clear the previous org's context atomically (set + clear in one call)? | Yes — the servlet accepts `organizationRefnum=0` to clear, and any positive value replaces. |
| 8 | **Promo `allowedDomains` enforcement on org invite:** If the org owner's promo has `allowedDomains`, should the invitee's email domain be validated? | Yes — check in `InviteOrgUser` step 7 before creating the User record. |

---

## 12. Future Work (Out of Scope Here)

- **Transfer-ownership workflow** — a dedicated servlet allowing the current OWNER (or an ADMIN, per business rule) to transfer the singleton `O` role to another existing ADMIN/member of the org, atomically demoting the previous owner (e.g., to `A`) in the same transaction. This is the only path by which `O` may ever be (re-)assigned; it is explicitly not part of the invite flow. Also needs to close the pre-existing gap where `OrganizationACLCreate` currently allows direct-granting `O` with no ceiling.
- **Per-app roles within an org context** — a deeper `OrgUserAppRole` structure tying (org, user, app) to an app-specific role. Currently the system only differentiates SuperAdmin / Guest / Patient / "other". This is a separate workstream.
- **Front-end** — accept/decline UI, org switcher component, pending invites list.
- **Payment integration** — org-level billing, plan inheritance from org owner. Explicitly deferred.
- **Org invite audit view** — a view joining `OrganizationInvite` with `Organization` and `User` for admin reporting.

