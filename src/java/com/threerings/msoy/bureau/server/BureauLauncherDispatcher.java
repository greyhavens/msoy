//
// $Id$

package com.threerings.msoy.bureau.server;

import javax.annotation.Generated;

import com.threerings.msoy.admin.gwt.BureauLauncherInfo;
import com.threerings.msoy.bureau.data.BureauLauncherMarshaller;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link BureauLauncherProvider}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from BureauLauncherService.java.")
public class BureauLauncherDispatcher extends InvocationDispatcher<BureauLauncherMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public BureauLauncherDispatcher (BureauLauncherProvider provider)
    {
        this.provider = provider;
    }

    @Override
    public BureauLauncherMarshaller createMarshaller ()
    {
        return new BureauLauncherMarshaller();
    }

    @Override
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case BureauLauncherMarshaller.LAUNCHER_INITIALIZED:
            ((BureauLauncherProvider)provider).launcherInitialized(
                source
            );
            return;

        case BureauLauncherMarshaller.SET_BUREAU_LAUNCHER_INFO:
            ((BureauLauncherProvider)provider).setBureauLauncherInfo(
                source, (BureauLauncherInfo)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
