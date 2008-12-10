//
// $Id$

package com.threerings.msoy.swiftly.server;

import com.google.inject.Inject;
import com.google.inject.Singleton;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.peer.server.MsoyPeerManager;

import com.threerings.msoy.swiftly.data.all.SwiftlyProject;

/**
 * Contains various Swiftly node actions.
 */
@Singleton
public class SwiftlyNodeActions
{
    /**
     * Reports to the room hosting this project that the supplied project object has been updated.
     */
    public void projectUpdated (SwiftlyProject project)
    {
    }

    /**
     * Reports to the room hosting this project that the supplied member is now a collaborator.
     */
    public void collaboratorAdded (int projectId, MemberName name)
    {
    }

    /**
     * Reports to the room hosting this project that the supplied member is not a collaborator.
     */
    public void collaboratorRemoved (int projectId, MemberName name)
    {
    }

    @Inject protected final MsoyPeerManager _peerMan;
}
