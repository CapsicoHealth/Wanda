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

package wanda.servlets.organization;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.TextUtil;
import tilda.utils.json.JSONPrinter;
import wanda.data.OrganizationRoleView_Factory;
import wanda.data.Organization_Data;
import wanda.data.Organization_Factory;
import wanda.data.Promo_Data;
import wanda.data.Promo_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;
import wanda.web.exceptions.NotFoundException;

/**
 * Reports how many users are currently counted against the promo code bound to a given Organization's owner (its
 * creator, see {@link Organization_Data#getOwner(Connection)}), i.e., already-active users plus still-pending
 * Scenario B Organization invites (see {@link Promo_Data#countBoundUsers(Connection)}), together with the promo
 * code's maxUsers limit (if any). This lets an org Admin/Owner know upfront, before attempting to invite more new
 * (not-yet-registered) users, how many "slots" remain.<BR>
 * <BR>
 * Only an org Admin/Owner may call this endpoint (enforced via {@link OrganizationRoleView_Factory#checkOrganizationAcl}),
 * guest users are never allowed (guestAllowed=false), and if the Organization's owner has no promo code at all, the
 * response simply reports an unbounded/unlimited usage (no promo code gate to enforce).
 */
@WebServlet("/svc/wanda/organizations/promoCodeUsage")
public class PromoCodeUsage extends SimpleServlet
  {
    private static final long     serialVersionUID = 1L;
    protected static final Logger LOG              = LogManager.getLogger(PromoCodeUsage.class.getName());

    public PromoCodeUsage()
      {
        super(true, false, false);
      }

    @Override
    protected void justDo(RequestUtil Req, ResponseUtil Res, Connection C, User_Data U)
    throws Exception
      {
        long organizationRefnum = Req.getParamLong("organizationRefnum", true);
        Req.throwIfErrors();
        OrganizationRoleView_Factory.checkOrganizationAcl(C, U, organizationRefnum, OrganizationRoleView_Factory.OrganizationRole.ADMIN);

        Organization_Data org = Organization_Factory.lookupByPrimaryKey(organizationRefnum);
        if (org.read(C) == false)
          throw new NotFoundException("Organization", "" + organizationRefnum, "Organization " + organizationRefnum + " cannot be found.");

        User_Data owner = org.getOwner(C);
        String promoCode = owner.isNullPromoCode() == true ? null : owner.getPromoCode();

        JSONPrinter j = new JSONPrinter();
        j.addElement("organizationRefnum", organizationRefnum);
        j.addElement("promoCode", promoCode);

        if (TextUtil.isNullOrEmpty(promoCode) == true)
          {
            // Gate: no promo code on the owner means there is nothing to enforce/report.
            j.addElement("unlimited", true);
            j.addElement("maxUsers", 0L);
            j.addElement("count", 0L);
            j.addElement("reached", false);
          }
        else
          {
            Promo_Data promo = Promo_Factory.lookupByCode(promoCode);
            boolean found = promo.read(C);
            long maxUsers = found == true ? promo.getMaxUsers() : 0L;
            long count = found == true ? promo.countBoundUsers(C) : 0L;
            boolean unlimited = found == false || maxUsers <= 0;
            j.addElement("unlimited", unlimited);
            j.addElement("maxUsers", maxUsers);
            j.addElement("count", count);
            j.addElement("reached", found == true && promo.hasReachedMaxUsers(C));
          }

        Res.successJson(j);
      }
  }
