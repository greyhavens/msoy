//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.samskivert.io.PersistenceException;

import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.server.persist.MemberRecord;
import com.threerings.msoy.web.data.WebCreds;

import java.util.ArrayList;

/**
 * Project Management XML-RPC Methods
 */
public class SwiftlyProjectRpc
{
    /**
     * Provides a list of the user's projects.
     */
    public ArrayList getProjects (String authtoken)
    {
        WebCreds creds;
        ArrayList<String> projects;
        MemberRecord mrec = null;

        projects = new ArrayList<String>();

        try {
            mrec = MsoyServer.memberRepo.loadMemberForSession(authtoken);
        } catch (PersistenceException e) {
            // TODO: Return an auth denied error
        }

        if (mrec == null) {
            // TODO: Session has expired.
            projects.add("Creds == " + authtoken);
            return projects;
        }

        // TODO: Projects for this user
        projects.add("Best Game Ever");
        projects.add("Best Game Ever 2");
        return projects;
    }
}
