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

package wanda.servlets;

import java.util.concurrent.TimeUnit;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.JsonObject;

import jakarta.servlet.ServletContext;
import jakarta.servlet.annotation.WebServlet;
import tilda.db.Connection;
import tilda.utils.SystemValues;
import tilda.utils.json.JSONUtil;
import wanda.data.Assessment_Data;
import wanda.data.Assessment_Factory;
import wanda.data.User_Data;
import wanda.web.RequestUtil;
import wanda.web.ResponseUtil;
import wanda.web.SimpleServlet;

@WebServlet("/svc/assessment/create")
public class AssessmentCreate extends SimpleServlet
  {

    private static final long serialVersionUID = -1745307937763620646L;

    public AssessmentCreate()
      {
        super(true, true, true);
      }

    static private Cache<String, JsonObject> _FORMS_CACHE = CacheBuilder.newBuilder().maximumSize(10).expireAfterWrite(30, TimeUnit.MINUTES).build();


    @Override
    protected void justDo(RequestUtil req, ResponseUtil res, Connection C, User_Data U)
    throws Exception
      {
        long refnum = req.getParamLong("refnum", false);

        String type = req.getParamString("type", true);
        String topic = req.getParamString("topic", true);
        String data = req.getParamString("data", true);

        req.throwIfErrors();

        JsonObject formObj = _FORMS_CACHE.getIfPresent(type);
        if (formObj == null)
          {
            ServletContext context = getServletContext();
            String filePath = context.getRealPath("/").replaceAll("CapsicoWebDynamic", "CapsicoWebStatic") + "json\\assessments\\form-" + type + ".json";
            formObj = JSONUtil.fromJSONFile(filePath);
            if (formObj == null)
              throw new Exception("An error occurred: cannot find the assessment form for type '" + type + "'");
            _FORMS_CACHE.put(type, formObj);
          }
        else
          LOG.debug("AppUserView list already cached for this user");

        Assessment_Data a = null;
        if (refnum != SystemValues.EVIL_VALUE)
          {
            a = Assessment_Factory.lookupByPrimaryKey(refnum);
            if (a.read(C) == false)
              throw new Exception("An error occurred: cannot find previous version of this assessment");
            if (a.getTopic().equals(topic) == false || a.getType().equals(topic) == false)
              throw new Exception("An error occurred: the previous version of assessment doesn't match the topic and/or type passed in");
            if (a.isNullCompletionDt() == true)
              throw new Exception("An error occurred: this assessment has already been completed.");
          }
        else
          a = Assessment_Factory.create(U.getRefnum(), topic, type);

        a.updateStats(formObj, data);

        // Assessment_Hist_Data ah = a.copyForHistory();
        // if (ah != null && ah.write(C) == false)
        // throw new Exception("Database error: cannot write the assessment history information to the database.");

        if (a.write(C) == false)
          throw new Error("There was an error writing the assessment to the database due to an unknown error.");

        res.successJson("status", a);
      }

  }
