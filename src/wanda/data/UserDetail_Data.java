/* ===========================================================================
 * Copyright (C) 2017 CapsicoHealth Inc.
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
import tilda.utils.DateTimeUtil;
import tilda.utils.TextUtil;

/**
This is the application class <B>Data_UserDetail</B> mapped to the table <B>WANDA.UserDetail</B>.
@see wanda.data._Tilda.TILDA__USERDETAIL
*/
public class UserDetail_Data extends wanda.data._Tilda.TILDA__USERDETAIL
 {
   protected static final Logger LOG = LogManager.getLogger(UserDetail_Data.class.getName());

   public UserDetail_Data() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   @Override
   public String getNameShortComp()
     {
       // TODO Auto-generated method stub
       return null;
     }

   @Override
   public String getNameStandard()
     {
       return TextUtil.standardizeFullName(getNameTitle(), getNameLast(), getNameFirst(), getNameMiddle());
     }

   @Override
   public int getCurrentAge()
     {
       return DateTimeUtil.computeAgeNow(getDob());
     }

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

   public static UserDetail_Data cloneWithCreateMode(UserDetail_Data src) throws Exception
     {
       UserDetail_Data NewObject = UserDetail_Factory.create(src.getUserRefnum(), src.getNameLast(), src.getNameFirst());
       src.copyTo(NewObject);
       return NewObject;
     }

   public void updateDetails(Connection C, String nameTitle, String nameFirst, String nameLast) throws Exception
     {
       if (TextUtil.isNullOrEmpty(nameTitle) == false)
         setNameTitle(nameTitle);
       
       if (TextUtil.isNullOrEmpty(nameFirst) == false)
         setNameFirst(nameFirst);
       
       if (TextUtil.isNullOrEmpty(nameLast) == false)
         setNameLast(nameLast);
       write(C);
     }
   
 }
