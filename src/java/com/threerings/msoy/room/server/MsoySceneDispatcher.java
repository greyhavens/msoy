//
// $Id$

package com.threerings.msoy.room.server;

import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoySceneMarshaller;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;
import com.threerings.whirled.client.SceneService;

/**
 * Dispatches requests to the {@link MsoySceneProvider}.
 */
public class MsoySceneDispatcher extends InvocationDispatcher<MsoySceneMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public MsoySceneDispatcher (MsoySceneProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public MsoySceneMarshaller createMarshaller ()
    {
        return new MsoySceneMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case MsoySceneMarshaller.MOVE_TO:
            ((MsoySceneProvider)provider).moveTo(
                source, ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), ((Integer)args[2]).intValue(), (MsoyLocation)args[3], (SceneService.SceneMoveListener)args[4]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
