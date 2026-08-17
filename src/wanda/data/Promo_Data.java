/* ===========================================================================
 * Copyright (C) 2024 CapsicoHealth Inc.
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

package wanda.data;

import java.time.LocalDate;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.utils.DateTimeUtil;

/**
This is the application class <B>Data_Promo</B> mapped to the table <B>WANDA.Promo</B>.
@see wanda.data._Tilda.TILDA__PROMO
*/
public class Promo_Data extends wanda.data._Tilda.TILDA__PROMO
 {
   protected static final Logger LOG = LogManager.getLogger(Promo_Data.class.getName());

   public Promo_Data() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   @Override
   protected boolean beforeWrite(Connection C) throws Exception
     {
       // Do things before writing the object to disk, for example, take care of AUTO fields.
       return true;
     }

   @Override
   protected boolean afterRead(Connection C) throws Exception
     {
       // Do things after an object has just been read form the data store, for example, take care of AUTO fields.
       return true;
     }

  /**
   * To be valid, a promo code must be active, and the start date must be in the past, and the end date is either null or in the future.
   * @return
   */
  public boolean isActiveAndValid()
    {
      LocalDate now = DateTimeUtil.nowLocalDate();
      return getActive() == true && now.compareTo(getStart()) >= 0 && (isNullEnd() == true || now.compareTo(getEnd()) <= 0);
    }

  /**
   * @param C
   * @return the number of active (non-deleted) users currently bound to this promo code, across the entire
   * platform, regardless of which Tenant/App/Organization they may belong to.
   * @throws Exception
   */
  public long countActiveUsers(Connection C) throws Exception
    {
      return User_Factory.countUsersByPromoCode(C, getCode());
    }

  /**
   * @param C
   * @return the number of still-pending, not-yet-onboarded Organization invites (Scenario B -- brand new users
   * with no account yet) across the entire platform whose inviting Organization's owner is bound to this promo
   * code. See {@link OrganizationInvite_Factory#countPendingByOwnerPromoCode}.
   * @throws Exception
   */
  public long countPendingOrgInvites(Connection C) throws Exception
    {
      return OrganizationInvite_Factory.countPendingByOwnerPromoCode(C, getCode());
    }

  /**
   * @param C
   * @return the total number of users effectively bound to this promo code, i.e., already-active users PLUS
   * still-pending Organization invites that will inherit this promo code once accepted. This is the number that
   * must be compared against {@link #getMaxUsers()} to enforce the bound consistently across the whole platform
   * (all Organizations, all users, all pending invites) -- not just within a single Organization.
   * @throws Exception
   */
  public long countBoundUsers(Connection C) throws Exception
    {
      return countActiveUsers(C) + countPendingOrgInvites(C);
    }

  /**
   * A promo code with a maxUsers of 0 (or less) is considered unbounded/unlimited.
   *
   * @param C
   * @return true if this promo code has a positive maxUsers value and the current count of users effectively
   * bound to it (active users + pending Organization invites, see {@link #countBoundUsers(Connection)}) has
   * reached (or would exceed) that value.
   * @throws Exception
   */
  public boolean hasReachedMaxUsers(Connection C) throws Exception
    {
      if (getMaxUsers() <= 0)
        return false;
      long count = countBoundUsers(C);
      return count > 0 && count >= getMaxUsers();
    }

 }
