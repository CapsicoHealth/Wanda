/* ===========================================================================
 * Copyright (C) 2025 CapsicoHealth Inc.
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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.*;

/**
This is the application class <B>Data_UserPlanPreOrder</B> mapped to the table <B>WANDA.UserPlanPreOrder</B>.
@see wanda.data._Tilda.TILDA__USERPLANPREORDER
*/
public class UserPlanPreOrder_Factory extends wanda.data._Tilda.TILDA__USERPLANPREORDER_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(UserPlanPreOrder_Factory.class.getName());

   protected UserPlanPreOrder_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

   /**
    * Deletes the in-flight pre-order for a user FOR A GIVEN PRODUCT. The product id is required: a user can
    * legitimately have one in-flight order per product (e.g., subscribing to GenAILearning while a credit
    * top-up for Agentic is mid-checkout), and deleting across products would silently cancel an unrelated
    * payment the user is in the middle of approving.
    */
   public static int delete(Connection C, long userRefnum, String paymentSystemProductId) throws Exception
    {
      DeleteQuery Q = newDeleteQuery(C);
      Q.where().equals(COLS.USERREFNUM, userRefnum)
               .and().equals(COLS.PAYMENTSYSTEMPRODUCTID, paymentSystemProductId);
      
      return Q.execute();
    }

 }
