//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.client.PeerProjectService;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link PeerProjectService}.
 */
public interface PeerProjectProvider extends InvocationProvider
{
    /**
     * Handles a {@link PeerProjectService#collaboratorAdded} request.
     */
    public void collaboratorAdded (ClientObject caller, int arg1, MemberName arg2);

    /**
     * Handles a {@link PeerProjectService#collaboratorRemoved} request.
     */
    public void collaboratorRemoved (ClientObject caller, int arg1, MemberName arg2);

    /**
     * Handles a {@link PeerProjectService#projectUpdated} request.
     */
    public void projectUpdated (ClientObject caller, SwiftlyProject arg1);
}
