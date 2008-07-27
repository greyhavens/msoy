//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.client.PeerProjectService;
import com.threerings.msoy.swiftly.data.all.SwiftlyProject;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link PeerProjectService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PeerProjectMarshaller extends InvocationMarshaller
    implements PeerProjectService
{
    /** The method id used to dispatch {@link #collaboratorAdded} requests. */
    public static final int COLLABORATOR_ADDED = 1;

    // from interface PeerProjectService
    public void collaboratorAdded (Client arg1, int arg2, MemberName arg3)
    {
        sendRequest(arg1, COLLABORATOR_ADDED, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #collaboratorRemoved} requests. */
    public static final int COLLABORATOR_REMOVED = 2;

    // from interface PeerProjectService
    public void collaboratorRemoved (Client arg1, int arg2, MemberName arg3)
    {
        sendRequest(arg1, COLLABORATOR_REMOVED, new Object[] {
            Integer.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #projectUpdated} requests. */
    public static final int PROJECT_UPDATED = 3;

    // from interface PeerProjectService
    public void projectUpdated (Client arg1, SwiftlyProject arg2)
    {
        sendRequest(arg1, PROJECT_UPDATED, new Object[] {
            arg2
        });
    }
}
