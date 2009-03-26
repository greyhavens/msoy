//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.world.client.WorldService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link WorldService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class WorldMarshaller extends InvocationMarshaller
    implements WorldService
{
    /** The method id used to dispatch {@link #getGroupHomeSceneId} requests. */
    public static final int GET_GROUP_HOME_SCENE_ID = 1;

    // from interface WorldService
    public void getGroupHomeSceneId (Client arg1, int arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_GROUP_HOME_SCENE_ID, new Object[] {
            Integer.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #getHomePageGridItems} requests. */
    public static final int GET_HOME_PAGE_GRID_ITEMS = 2;

    // from interface WorldService
    public void getHomePageGridItems (Client arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_HOME_PAGE_GRID_ITEMS, new Object[] {
            listener2
        });
    }
}
