//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.data.WebCreds;

import com.threerings.msoy.swiftly.client.SwiftlyProjectRpc;

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
        HashMap<String,Object> project = new HashMap<String,Object>();
        project.put(SwiftlyProjectRpc.PROJECT_NAME, "Best Game Ever");
        project.put(SwiftlyProjectRpc.PROJECT_ID, "1");
        
        projects.add(project);
        return projects;
    }


    /**
     * Create a project for the user.
     */
    public String createProject (String authtoken, String projectName) {
        // TODO: Do some stuff
        return null;
    }
}
