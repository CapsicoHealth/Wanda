/* ===========================================================================
 * Copyright (C) 2026 CapsicoHealth Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package wanda.servlets;

import java.util.List;
import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.SystemValues;
import tilda.utils.TextUtil;
import wanda.data.OrganizationRoleView_Factory;
import wanda.data.ProjectRoleView_Factory;
import wanda.data.Project_Data;
import wanda.data.Project_Factory;
import wanda.data.UserPlanCreditLedgerMinimalOrganizationProjectDailyView_Data;
import wanda.data.UserPlanCreditLedgerMinimalOrganizationProjectDailyView_Factory;
import wanda.data.UserPlanCreditLedgerMinimalOrganizationProjectView_Data;
import wanda.data.UserPlanCreditLedgerMinimalOrganizationProjectView_Factory;
import wanda.data.UserPlanCreditLedgerMinimalProjectItemDailyView_Data;
import wanda.data.UserPlanCreditLedgerMinimalProjectItemDailyView_Factory;
import wanda.data.UserPlanCreditLedgerMinimalProjectItemView_Data;
import wanda.data.UserPlanCreditLedgerMinimalProjectItemView_Factory;
import wanda.data.UserPlanCreditLedgerMinimalProjectUserDailyView_Data;
import wanda.data.UserPlanCreditLedgerMinimalProjectUserDailyView_Factory;
import wanda.data.UserPlanCreditLedgerMinimalProjectUserView_Data;
import wanda.data.UserPlanCreditLedgerMinimalProjectUserView_Factory;
import wanda.data.UserPlanCreditLedgerMinimalUserItemDailyView_Data;
import wanda.data.UserPlanCreditLedgerMinimalUserItemDailyView_Factory;
import wanda.data.UserPlanCreditLedgerMinimalUserItemView_Data;
import wanda.data.UserPlanCreditLedgerMinimalUserItemView_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.BadRequestException;
import wanda.web.exceptions.NotFoundException;

/**
 * Rolled-up credit SPEND reporting over an organization / project / user / item hierarchy, for the last 30, 60
 * and 90 days, or day by day over the last 30 days.
 * <P>
 * This is the aggregate counterpart to the per-user endpoints in this package: {@link UserCreditsBalance} (what's
 * left in MY wallet), {@link UserCreditsUsage} / {@link UserCreditsHistory} (MY own raw ledger rows). Those are
 * always self-scoped and hand the client raw rows to slice; this one answers "what did this project/org/item
 * spend" and is therefore the only credits endpoint that can expose OTHER users' spend -- hence the per-branch
 * access control below.
 * <P>
 * <B>All aggregation happens in the database</B>, in a family of narrow, USE-only Tilda views over the WORM
 * ledger, rather than in Java, so Postgres can cache and reuse them across callers. This servlet therefore only
 * picks the right view/query for the requested shape and enforces access -- it never sums anything itself.
 * <P>
 * <B>On item identity:</B> {@code itemId} is only unique WITHIN an {@code itemType} -- a Flow and an Agent may
 * perfectly well carry the same id, because each app picks its own key model. An {@code itemId} without its
 * {@code itemType} is therefore meaningless and rejected. The reverse is fine and useful: an {@code itemType}
 * on its own means "every item of this type", and {@code perItem=true} with no type at all means "every item,
 * whatever its type".
 * <P>
 * The requested shape is inferred from which parameters are supplied, and each shape has its own access rule:
 * <TABLE border="1">
 * <TR><TH>Parameters</TH><TH>Returns</TH><TH>Requires</TH></TR>
 * <TR><TD>item-scoped (see below) + {@code projectRefnum}</TD>
 *     <TD>that project's item spend, all users combined</TD>
 *     <TD>project OWNER/ADMIN</TD></TR>
 * <TR><TD>item-scoped, no project</TD>
 *     <TD>the CALLER's own item spend, across all projects</TD>
 *     <TD>nothing beyond being signed in</TD></TR>
 * <TR><TD>{@code organizationRefnum} only</TD><TD>one row per project in the org, by project title</TD>
 *     <TD>org OWNER/ADMIN</TD></TR>
 * <TR><TD>{@code projectRefnum}, {@code perUser=false}</TD><TD>a single rolled-up row for the project</TD>
 *     <TD>any project member (READER+)</TD></TR>
 * <TR><TD>{@code projectRefnum}, {@code perUser=true}</TD><TD>one row per user in the project, by user id</TD>
 *     <TD>project OWNER/ADMIN -- it exposes other members' individual spend</TD></TR>
 * <TR><TD>{@code perUser=true}, no project</TD><TD>one row per project for the CALLER, by project title</TD>
 *     <TD>nothing beyond being signed in: it is always the caller's own spend</TD></TR>
 * </TABLE>
 * <P>
 * A request is <B>item-scoped</B> when it carries an {@code itemType} or sets {@code perItem=true}, and then
 * narrows in three steps: {@code itemType}+{@code itemId} → a single item; {@code itemType} alone → every item
 * of that type; {@code perItem=true} alone → every item. Note the project-side item shapes require OWNER/ADMIN
 * rather than the plain membership the project TOTAL needs: an itemised breakdown is a materially finer lens on
 * what the team is doing than a single number, and in a small project it edges close to naming who did what.
 * <P>
 * Orthogonally, {@code daily=true} switches every shape above from the 30/60/90-day rollup columns
 * ({@code amount30Days}/{@code amount60Days}/{@code amount90Days}) to a LIST of one row per calendar {@code day}
 * with a single {@code amount}, over the last 30 days only. The access rules are identical -- {@code daily} only
 * changes the time granularity, never the scope. Note that days with NO spend produce NO row at all (the ledger
 * is only written when credits move), so a client drawing a continuous 30-day chart must zero-fill the gaps
 * itself; that keeps the payload proportional to actual activity rather than to the window length.
 */
@WebServlet("/svc/wanda/credits/report")
public class UserCreditsReport extends SimpleServlet
  {
    private static final long serialVersionUID = 6620950265120885577L;

    public UserCreditsReport()
      {
        super(true, false, true);
      }

    /**
     * A generous cap: these are pre-aggregated rows (one per project, or one per user in a project), not raw
     * ledger rows, so even a large organization stays well within this.
     */
    private static final int _MAX_ROWS       = 500;

    /**
     * The daily shapes multiply the row count by up to 30 (one per day), and the un-narrowed variants also fan
     * out per project/user/item, so they get their own, larger cap.
     */
    private static final int _MAX_ROWS_DAILY = 5000;

    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        long organizationRefnum = req.getParamLong("organizationRefnum", false);
        long projectRefnum = req.getParamLong("projectRefnum", false);
        String itemType = req.getParamString("itemType", false);
        String itemId = req.getParamString("itemId", false);
        boolean perUser = req.getParamBoolean("perUser", false);
        boolean perItem = req.getParamBoolean("perItem", false);
        boolean daily = req.getParamBoolean("daily", false);

        req.throwIfErrors();

        boolean hasItemType = TextUtil.isNullOrEmpty(itemType) == false;
        boolean hasItemId = TextUtil.isNullOrEmpty(itemId) == false;

        // An itemId is only unique within its itemType (see the class comment), so an itemId on its own cannot be
        // resolved to anything and is rejected rather than silently widened. The converse is legitimate: an
        // itemType alone is the "all items of this type" report.
        if (hasItemId == true && hasItemType == false)
          throw new BadRequestException("itemId", "An itemId is only unique within an itemType, so itemType must be supplied alongside it.");

        // Item-scoped in three progressively wider steps: one item, one type, or everything.
        if (hasItemType == true || perItem == true)
          {
            if (projectRefnum != SystemValues.EVIL_VALUE)
              {
                // Itemising a project's spend is an administrative lens -- materially more revealing than the
                // single project total a plain member can see, and in a small project it verges on attributing
                // work to people -- so it is held to the same bar as the per-user breakdown.
                ProjectRoleView_Factory.checkProjectAcl(C, U, projectRefnum, ProjectRoleView_Factory.ProjectRole.ADMIN);
                checkProjectOrganization(C, projectRefnum, organizationRefnum);

                if (daily == true)
                  {
                    List<UserPlanCreditLedgerMinimalProjectItemDailyView_Data> L =
                    hasItemId == true ? UserPlanCreditLedgerMinimalProjectItemDailyView_Factory.lookupWhereProjectItem(C, projectRefnum, itemType, itemId, 0, _MAX_ROWS_DAILY)
                    : hasItemType == true ? UserPlanCreditLedgerMinimalProjectItemDailyView_Factory.lookupWhereProjectItemType(C, projectRefnum, itemType, 0, _MAX_ROWS_DAILY)
                    : UserPlanCreditLedgerMinimalProjectItemDailyView_Factory.lookupWhereProject(C, projectRefnum, 0, _MAX_ROWS_DAILY);
                    res.successJson("", L);
                    return;
                  }

                if (hasItemId == true)
                  {
                    UserPlanCreditLedgerMinimalProjectItemView_Data PI = UserPlanCreditLedgerMinimalProjectItemView_Factory.lookupByProjectItem(projectRefnum, itemType, itemId);
                    res.successJson("", PI.read(C) == true ? PI : null);
                    return;
                  }

                List<UserPlanCreditLedgerMinimalProjectItemView_Data> L =
                hasItemType == true ? UserPlanCreditLedgerMinimalProjectItemView_Factory.lookupWhereProjectItemType(C, projectRefnum, itemType, 0, _MAX_ROWS)
                : UserPlanCreditLedgerMinimalProjectItemView_Factory.lookupWhereProject(C, projectRefnum, 0, _MAX_ROWS);
                res.successJson("", L);
                return;
              }

            // No project: the "global" reading of item cost, i.e. what THIS user has spent wherever they ran it.
            // Scoped to U.getRefnum() by construction, so no access check is possible or needed. Cross-user
            // global item spend is a site-admin question and deliberately has no shape here yet.
            if (daily == true)
              {
                List<UserPlanCreditLedgerMinimalUserItemDailyView_Data> L =
                hasItemId == true ? UserPlanCreditLedgerMinimalUserItemDailyView_Factory.lookupWhereUserItem(C, U.getRefnum(), itemType, itemId, 0, _MAX_ROWS_DAILY)
                : hasItemType == true ? UserPlanCreditLedgerMinimalUserItemDailyView_Factory.lookupWhereUserItemType(C, U.getRefnum(), itemType, 0, _MAX_ROWS_DAILY)
                : UserPlanCreditLedgerMinimalUserItemDailyView_Factory.lookupWhereUser(C, U.getRefnum(), 0, _MAX_ROWS_DAILY);
                res.successJson("", L);
                return;
              }

            if (hasItemId == true)
              {
                UserPlanCreditLedgerMinimalUserItemView_Data UI = UserPlanCreditLedgerMinimalUserItemView_Factory.lookupByUserItem(U.getRefnum(), itemType, itemId);
                res.successJson("", UI.read(C) == true ? UI : null);
                return;
              }

            List<UserPlanCreditLedgerMinimalUserItemView_Data> L =
            hasItemType == true ? UserPlanCreditLedgerMinimalUserItemView_Factory.lookupWhereUserItemType(C, U.getRefnum(), itemType, 0, _MAX_ROWS)
            : UserPlanCreditLedgerMinimalUserItemView_Factory.lookupWhereUser(C, U.getRefnum(), 0, _MAX_ROWS);
            res.successJson("", L);
            return;
          }

        if (projectRefnum != SystemValues.EVIL_VALUE)
          {
            if (perUser == true)
              {
                // Breaking a project down BY USER exposes individual members' spend to each other, so this is an
                // administrative view: owner/admin only, unlike the project total below.
                ProjectRoleView_Factory.checkProjectAcl(C, U, projectRefnum, ProjectRoleView_Factory.ProjectRole.ADMIN);
                checkProjectOrganization(C, projectRefnum, organizationRefnum);
                if (daily == true)
                  {
                    List<UserPlanCreditLedgerMinimalProjectUserDailyView_Data> L = UserPlanCreditLedgerMinimalProjectUserDailyView_Factory.lookupWhereProject(C, projectRefnum, 0, _MAX_ROWS_DAILY);
                    res.successJson("", L);
                    return;
                  }
                List<UserPlanCreditLedgerMinimalProjectUserView_Data> L = UserPlanCreditLedgerMinimalProjectUserView_Factory.lookupWhereProject(C, projectRefnum, 0, _MAX_ROWS);
                res.successJson("", L);
                return;
              }

            // The project's own total tells a member nothing about who spent what, so plain membership is enough.
            ProjectRoleView_Factory.checkProjectAcl(C, U, projectRefnum, ProjectRoleView_Factory.ProjectRole.READER);
            checkProjectOrganization(C, projectRefnum, organizationRefnum);
            if (daily == true)
              {
                List<UserPlanCreditLedgerMinimalOrganizationProjectDailyView_Data> L = UserPlanCreditLedgerMinimalOrganizationProjectDailyView_Factory.lookupWhereProject(C, projectRefnum, 0, _MAX_ROWS_DAILY);
                res.successJson("", L);
                return;
              }
            UserPlanCreditLedgerMinimalOrganizationProjectView_Data P = UserPlanCreditLedgerMinimalOrganizationProjectView_Factory.lookupByProject(projectRefnum);
            // A project that has never been charged has no row in the (USE-only) view at all: that is "zero
            // spend", not an error, so report it as an empty payload rather than a 404.
            res.successJson("", P.read(C) == true ? P : null);
            return;
          }

        if (organizationRefnum != SystemValues.EVIL_VALUE && perUser == false)
          {
            // Whole-organization breakdown by project: an administrative view over every member's work.
            OrganizationRoleView_Factory.checkOrganizationAcl(C, U, organizationRefnum, OrganizationRoleView_Factory.OrganizationRole.ADMIN);
            if (daily == true)
              {
                List<UserPlanCreditLedgerMinimalOrganizationProjectDailyView_Data> L = UserPlanCreditLedgerMinimalOrganizationProjectDailyView_Factory.lookupWhereOrganization(C, organizationRefnum, 0, _MAX_ROWS_DAILY);
                res.successJson("", L);
                return;
              }
            List<UserPlanCreditLedgerMinimalOrganizationProjectView_Data> L = UserPlanCreditLedgerMinimalOrganizationProjectView_Factory.lookupWhereOrganization(C, organizationRefnum, 0, _MAX_ROWS);
            res.successJson("", L);
            return;
          }

        if (perUser == true)
          {
            // "My own spend, broken down by project". Scoped to the caller by construction (U.getRefnum() is
            // never taken from the request), so it needs no access check at all -- and for that same reason an
            // organizationRefnum passed here is meaningless and ignored: the caller can only ever see themselves.
            if (daily == true)
              {
                List<UserPlanCreditLedgerMinimalUserItemDailyView_Data> L = UserPlanCreditLedgerMinimalUserItemDailyView_Factory.lookupWhereUser(C, U.getRefnum(), 0, _MAX_ROWS_DAILY);
                res.successJson("", L);
                return;
              }
            List<UserPlanCreditLedgerMinimalProjectUserView_Data> L = UserPlanCreditLedgerMinimalProjectUserView_Factory.lookupWhereUser(C, U.getRefnum(), 0, _MAX_ROWS);
            res.successJson("", L);
            return;
          }

        // Nothing to scope by: no item, no project, no organization, and not asking for the caller's own
        // per-project breakdown. There is no sensible "everything, everywhere" report here (that would be a
        // cross-tenant read), so this is a bad request rather than an empty result.
        throw new BadRequestException("scope", "A credit report requires an itemType, perItem=true, an organizationRefnum, a projectRefnum, or perUser=true.");
      }

    /**
     * Cross-validates a project against an organization when the caller supplied both.
     * <P>
     * The project alone already fully determines scope -- a project belongs to exactly one organization -- and
     * the ACL check has already run against the project by the time we get here, so this is <B>not</B> load-bearing
     * for access control. It is a consistency assertion: a client that sends an org/project pair which doesn't
     * actually match is either carrying stale state (the project moved orgs) or has had its URL tampered with,
     * and in both cases silently answering with the project's real data would be the wrong thing to do. Failing
     * loudly turns a class of latent client bug into an immediate, obvious one.
     * <P>
     * No-ops when no organization was supplied, which is the common case.
     */
    private static void checkProjectOrganization(Connection C, long projectRefnum, long organizationRefnum)
    throws Exception
      {
        if (organizationRefnum == SystemValues.EVIL_VALUE)
          return;

        Project_Data P = Project_Factory.lookupByPrimaryKey(projectRefnum);
        if (P.read(C) == false)
          throw new NotFoundException("CreditsReport", "projectRefnum", "The project " + projectRefnum + " could not be found.");

        // A project's organizationRefnum is nullable: an unaffiliated project can never match an explicitly
        // requested org, so treat null as a mismatch rather than letting it slip through a == comparison.
        if (P.isNullOrganizationRefnum() == true || P.getOrganizationRefnum() != organizationRefnum)
          throw new BadRequestException("organizationRefnum", "The project " + projectRefnum + " does not belong to organization " + organizationRefnum + ".");
      }
  }
