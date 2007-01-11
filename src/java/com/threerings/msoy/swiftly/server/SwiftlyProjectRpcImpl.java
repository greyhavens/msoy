//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.data.WebCreds;

import com.threerings.msoy.swiftly.client.SwiftlyProjectRpc;
import com.threerings.msoy.swiftly.server.persist.SwiftlyProjectRecord;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * Project Management XML-RPC Methods
 */
public class SwiftlyProjectRpcImpl
    implements SwiftlyProjectRpc
{
    /**
     * Provides a list of the user's projects.
     */
    public ArrayList<Map<String,Object>> getProjects (String authtoken)
    {
        WebCreds creds;
        ArrayList<Map<String,Object>> projects;
        MemberRecord mrec = null;

        projects = new ArrayList<Map<String,Object>>();

        try {
            mrec = MsoyServer.memberRepo.loadMemberForSession(authtoken);
        } catch (PersistenceException e) {
            // TODO: Return an auth denied error
        }

        if (mrec == null) {
            // TODO: Session has expired.
            return projects;
        }

        // TODO: Projects for this user
        for (SwiftlyProjectRecord record : MsoyServer.swiftlyMan.findProjects(mrec)) {
            HashMap<String,Object> project = new HashMap<String,Object>();

            project.put(SwiftlyProjectRpc.PROJECT_NAME, record.projectName);
            project.put(SwiftlyProjectRpc.PROJECT_ID, record.projectId);
            projects.add(project);
        }


        return projects;
    }


    /**
     * Create a project for the user.
     */
    public boolean createProject (String authtoken, String projectName) {
        MemberRecord mrec = null;

        try {
            mrec = MsoyServer.memberRepo.loadMemberForSession(authtoken);
        } catch (PersistenceException e) {
            // TODO: Return an auth denied error
        }

        if (mrec == null) {
            // TODO: Session has expired.
            return false;
        }

        // TODO: Do some stuff
        try {
            MsoyServer.swiftlyMan.createProject(mrec, projectName);
        } catch (PersistenceException e) {
            // TODO: Return a retry error
            e.printStackTrace();
        }

        return false;
    }
}
