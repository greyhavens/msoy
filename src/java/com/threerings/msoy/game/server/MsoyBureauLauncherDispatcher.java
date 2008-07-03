//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.game.data.MsoyBureauLauncherMarshaller;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link MsoyBureauLauncherProvider}.
 */
public class MsoyBureauLauncherDispatcher extends InvocationDispatcher<MsoyBureauLauncherMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public MsoyBureauLauncherDispatcher (MsoyBureauLauncherProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public MsoyBureauLauncherMarshaller createMarshaller ()
    {
        return new MsoyBureauLauncherMarshaller();
    }

    @SuppressWarnings("unchecked")
    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case MsoyBureauLauncherMarshaller.LAUNCHER_INITIALIZED:
            ((MsoyBureauLauncherProvider)provider).launcherInitialized(
                source                
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
