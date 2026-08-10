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

import java.util.List;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import tilda.db.Connection;
import tilda.db.SelectQuery;
import tilda.types.Type_StringPrimitive;
import tilda.utils.TextUtil;

/**
 * This is the application class <B>Data_ProjectACLView</B> mapped to the table <B>PROJECTS.ProjectACLView</B>.
 * 
 * @see wanda.data._Tilda.TILDA__PROJECTACLVIEW
 */
public class ProjectACLView_Factory extends wanda.data._Tilda.TILDA__PROJECTACLVIEW_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(ProjectACLView_Factory.class.getName());

    protected ProjectACLView_Factory()
      {
      }

    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // Implement your customizations, if any, below.
    /////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


    public static void init(Connection C)
    throws Exception
      {
        // Add logic to initialize your object, for example, caching some values, or validating some things.
      }


    protected static Type_StringPrimitive[] _SEARCH_COLS = new Type_StringPrimitive[] { COLS.TITLE, COLS.DESCRIPTION
    };


    public static List<ProjectACLView_Data> lookupWhereComplexQueryAcl(Connection C, User_Data U, String search, String[] tags, boolean recent, boolean deleted, int i, int max)
    throws Exception
      {
        SelectQuery Q = newWhereQuery(C);
        Q.openPar()
        .equals(COLS.CREATORREFNUM, U.getRefnum())
        .or()
        .in(COLS.USERS, U.getRefnum())
        .closePar();

        search = TextUtil.isNullOrEmpty(search) == true ? "%" : "%" + search.replaceAll("\\W+", "%") + "%";
        Q.and().like(_SEARCH_COLS, search, true);

        if (TextUtil.isNullOrEmpty(tags) == false)
          {
            Q.and().openPar();
            for (String tag : tags)
              Q.and().in(COLS.TAGS, tag.trim(), false, true);
            Q.closePar();
          }

        if (deleted == true)
          Q.and().isNotNull(COLS.DELETED);
        else
          Q.and().isNull(COLS.DELETED);

        if (recent == true)
          Q.orderBy(COLS.LASTUPDATED, false);
        else
          Q.orderBy(COLS.TITLE, true);

        return runSelect(C, Q, i, max);
      }


  }
