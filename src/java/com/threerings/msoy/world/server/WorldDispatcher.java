//
// $Id$

package com.threerings.msoy.world.server;

import com.threerings.msoy.world.data.WorldMarshaller;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationDispatcher;
import com.threerings.presents.server.InvocationException;

/**
 * Dispatches requests to the {@link WorldProvider}.
 */
public class WorldDispatcher extends InvocationDispatcher<WorldMarshaller>
{
    /**
     * Creates a dispatcher that may be registered to dispatch invocation
     * service requests for the specified provider.
     */
    public WorldDispatcher (WorldProvider provider)
    {
        this.provider = provider;
    }

    @Override // documentation inherited
    public WorldMarshaller createMarshaller ()
    {
        return new WorldMarshaller();
    }

    @Override // documentation inherited
    public void dispatchRequest (
        ClientObject source, int methodId, Object[] args)
        throws InvocationException
    {
        switch (methodId) {
        case WorldMarshaller.GET_GROUP_HOME_SCENE_ID:
            ((WorldProvider)provider).getGroupHomeSceneId(
                source, ((Integer)args[0]).intValue(), (InvocationService.ResultListener)args[1]
            );
            return;

        case WorldMarshaller.GET_HOME_PAGE_GRID_ITEMS:
            ((WorldProvider)provider).getHomePageGridItems(
                source, (InvocationService.ResultListener)args[0]
            );
            return;

        default:
            super.dispatchRequest(source, methodId, args);
            return;
        }
    }
}
