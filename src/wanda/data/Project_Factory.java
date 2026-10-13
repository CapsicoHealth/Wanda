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
This is the application class <B>Data_Project</B> mapped to the table <B>WANDA.Project</B>.
@see wanda.data._Tilda.TILDA__PROJECT
*/
public class Project_Factory extends wanda.data._Tilda.TILDA__PROJECT_Factory
 {
   protected static final Logger LOG = LogManager.getLogger(Project_Factory.class.getName());

   protected Project_Factory() { }

/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//   Implement your customizations, if any, below.
/////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


   private static final java.util.concurrent.ConcurrentHashMap<String, ProjectCallback> _CALLBACKS = new java.util.concurrent.ConcurrentHashMap<String, ProjectCallback>();

   /**
    * Registers the callback notified when projects of the given appScope are updated or deleted through the
    * generic Wanda project servlets. A single callback is kept per appScope: registering again replaces it.
    */
   public static void registerCallback(String appScope, ProjectCallback callback)
    {
      if (appScope == null || callback == null)
       throw new IllegalArgumentException("appScope and callback are required.");
      ProjectCallback previous = _CALLBACKS.put(appScope, callback);
      if (previous != null && previous.getClass() != callback.getClass())
       LOG.warn("The project callback for appScope '" + appScope + "' was replaced: " + previous.getClass().getName() + " -> " + callback.getClass().getName());
    }

   /** Returns the callback registered for the appScope, or null if none. */
   public static ProjectCallback getCallback(String appScope)
    {
      return appScope == null ? null : _CALLBACKS.get(appScope);
    }

   public static void init(Connection C) throws Exception
    {
      // Add logic to initialize your object, for example, caching some values, or validating some things.
    }

 }
