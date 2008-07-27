//
// $Id$

package com.threerings.msoy.peer.client;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.swiftly.data.all.SwiftlyProject;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Provides game-related peer services.
 */
public interface PeerProjectService extends InvocationService
{
    /**
     * Reports to the room hosting this project that the supplied project object has been updated.
     */
    void projectUpdated (Client client, SwiftlyProject project);

    /**
     * Reports to the room hosting this project that the supplied member is now a collaborator.
     */
    void collaboratorAdded (Client client, int projectId, MemberName name);

    /**
     * Reports to the room hosting this project that the supplied member is not a collaborator.
     */
    void collaboratorRemoved (Client client, int projectId, MemberName name);
}
