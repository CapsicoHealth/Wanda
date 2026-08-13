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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.utils.TextUtil;

/**
This is the application class <B>Data_UserView</B> mapped to the table <B>WANDA.UserView</B>.
@see wanda.data._Tilda.TILDA__USERVIEW
*/
public class UserView_Data extends wanda.data._Tilda.TILDA__USERVIEW
 {
   protected static final Logger LOG = LogManager.getLogger(UserView_Data.class.getName());

   public UserView_Data() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   @Override
   public String getNameStandard()
    {
      return TextUtil.standardizeFullName(getNameTitle(), getNameLast(), getNameFirst(), getNameMiddle());
    }

   @Override
   protected boolean afterRead(Connection C) throws Exception
     {
       // Do things after an object has just been read form the data store, for example, take care of AUTO fields.
       return true;
     }

 }
