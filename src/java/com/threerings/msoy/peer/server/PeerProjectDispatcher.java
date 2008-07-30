//
// $Id$

package com.threerings.msoy.peer.server;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.peer.data.PeerProjectMarshaller;
import com.threerings.msoy.swiftly.data.all.SwiftlyProject;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link PeerProjectProvider}.
 */
public class PeerProjectDispatcher extends InvocationDispatcher<PeerProjectMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public PeerProjectDispatcher (PeerProjectProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public PeerProjectMarshaller createMarshaller ()
    {
        return new PeerProjectMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case PeerProjectMarshaller.COLLABORATOR_ADDED:
            ((PeerProjectProvider)provider).collaboratorAdded(
                source, ((Integer)args[0]).intValue(), (MemberName)args[1]
            );
            return;

        case PeerProjectMarshaller.COLLABORATOR_REMOVED:
            ((PeerProjectProvider)provider).collaboratorRemoved(
                source, ((Integer)args[0]).intValue(), (MemberName)args[1]
            );
            return;

        case PeerProjectMarshaller.PROJECT_UPDATED:
            ((PeerProjectProvider)provider).projectUpdated(
                source, (SwiftlyProject)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
