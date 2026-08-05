/*
 Tilda V2.5 template application class.
*/

package wanda.data;

import java.util.concurrent.TimeUnit;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

import tilda.db.Connection;
import wanda.web.exceptions.NotFoundException;

/**
 * This is the application class <B>Data_ProjectRoleView</B> mapped to the table <B>PROJECTS.ProjectRoleView</B>.
 * 
 * @see wanda.data._Tilda.TILDA__PROJECTROLEVIEW
 */
public class ProjectRoleView_Factory extends wanda.data._Tilda.TILDA__PROJECTROLEVIEW_Factory
  {
    protected static final Logger LOG = LogManager.getLogger(ProjectRoleView_Factory.class.getName());

    protected ProjectRoleView_Factory()
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

    // Simplified Cache Definition
    static private Cache<Long, Cache<Long, Character>> _ACL_PROJECT_CACHE = CacheBuilder.newBuilder().maximumSize(100).expireAfterAccess(30, TimeUnit.MINUTES).build();

    public static enum ProjectRole
      {
      READER, WRITER, ADMIN, OWNER
      };

    public static void checkProjectAcl(Connection C, User_Data U, long projectRefnum, ProjectRole requiredRole)
    throws Exception
      {
        if (requiredRole == null)
          throw new IllegalArgumentException("Required Role cannot be null");

        // 1. Get or Create the project-specific cache atomically
        Cache<Long, Character> userCache = _ACL_PROJECT_CACHE.get(projectRefnum, () -> {
          return CacheBuilder.newBuilder().maximumSize(100).expireAfterAccess(15, TimeUnit.MINUTES).build();
        });

        // 2. Get or Compute the specific user's role atomically
        char r = userCache.get(U.getRefnum(), () -> {
          ProjectRoleView_Data PRV = ProjectRoleView_Factory.lookupByProjectCreatorOrAccess(projectRefnum, U.getRefnum(), U.getRefnum());
          if (PRV.read(C) == false)
            return Character.valueOf((char) Character.UNASSIGNED);
          if (PRV.getCreatorRefnum() == U.getRefnum())
            return Character.valueOf('O');
          return Character.valueOf(PRV.getRole());
        });

        if (requiredRole == ProjectRole.OWNER && ProjectRoleView_Data._roleOwner == r)
          return;
        if (requiredRole == ProjectRole.READER && r != Character.UNASSIGNED)
          return;
        if (requiredRole == ProjectRole.WRITER && (ProjectRoleView_Data._roleOwner == r || ProjectRoleView_Data._roleAdmin == r || ProjectRoleView_Data._roleWriter == r))
          return;
        if (requiredRole == ProjectRole.ADMIN && (ProjectRoleView_Data._roleOwner == r || ProjectRoleView_Data._roleAdmin == r))
          return;

        throw new NotFoundException("MSLProject", "" + projectRefnum, "Project " + projectRefnum + " is not " + (requiredRole == ProjectRole.READER ? "accessible" : "updatable") + " by this user.");
      }

  }
