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

package wanda.data;

import tilda.db.Connection;

/**
 * Lets an application that owns the assets of a given project {@code appScope} take part in project
 * updates and deletes done through the generic Wanda project servlets, so that the generic endpoints
 * cannot change a project behind the application's back.
 * <P>
 * Register an implementation with {@link Project_Factory#registerCallback(String, ProjectCallback)}.
 * <P>
 * Both callbacks run inside the request's database transaction. Throwing from either aborts the
 * request and rolls the transaction back. Since the transaction commits only after the callbacks return,
 * any external work (e.g. renaming a storage folder) should be done in {@link #afterUpdate}, after the
 * database changes have been successfully written and could not fail on a conflict.
 */
public interface ProjectCallback
  {
    /**
     * Called before a project update is written. Throw to veto the change.
     * 
     * @param C the transaction's connection
     * @param oldProject the project as currently stored in the database
     * @param newProject the project with the requested changes applied, not yet written
     */
    default void beforeUpdate(Connection C, Project_Data oldProject, Project_Data newProject)
    throws Exception
      {
      }

    /**
     * Called after a project update was written to the database, still within the transaction. Throw to
     * roll everything back.
     * 
     * @param oldProject the project as it was before the change
     * @param newProject the project as written
     */
    default void afterUpdate(Connection C, Project_Data oldProject, Project_Data newProject)
    throws Exception
      {
      }

    /**
     * Whether projects of this app scope can be hard-deleted through the generic project delete servlet.
     */
    default boolean isHardDeleteAllowed(Project_Data project)
      {
        return true;
      }
  }
