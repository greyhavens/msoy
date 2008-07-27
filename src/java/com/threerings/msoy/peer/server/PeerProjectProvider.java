//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.client.PeerProjectService;
import com.threerings.msoy.swiftly.data.all.SwiftlyProject;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link PeerProjectService}.
 */
public interface PeerProjectProvider extends InvocationProvider
{
    /**
     * Handles a {@link PeerProjectService#collaboratorAdded} request.
     */
    void collaboratorAdded (ClientObject caller, int arg1, MemberName arg2);

    /**
     * Handles a {@link PeerProjectService#collaboratorRemoved} request.
     */
    void collaboratorRemoved (ClientObject caller, int arg1, MemberName arg2);

    /**
     * Handles a {@link PeerProjectService#projectUpdated} request.
     */
    void projectUpdated (ClientObject caller, SwiftlyProject arg1);
}
