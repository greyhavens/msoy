//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.msoy.world.client.MsoySceneService;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoySceneMarshaller;
import com.threerings.presents.client.Client;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.whirled.client.SceneService;
import com.threerings.whirled.data.SceneMarshaller;

/**
 * Dispatches requests to the {@link MsoySceneProvider}.
 */
public class MsoySceneDispatcher extends InvocationDispatcher
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public MsoySceneDispatcher (MsoySceneProvider provider)
    {
        this.provider = provider;
    }

    // from InvocationDispatcher
    public InvocationMarshaller createMarshaller ()
    {
        return new MsoySceneMarshaller();
    }

    @SuppressWarnings("unchecked") // from InvocationDispatcher
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case MsoySceneMarshaller.MOVE_TO:
            ((MsoySceneProvider)provider).moveTo(
                source,
                ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), ((Integer)args[2]).intValue(), (MsoyLocation)args[3], (SceneService.SceneMoveListener)args[4]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
