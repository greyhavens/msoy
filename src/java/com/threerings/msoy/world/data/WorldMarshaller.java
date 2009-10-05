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
    /** The method id used to dispatch {@link #getHomeId} requests. */
    public static final int GET_HOME_ID = 1;

    // from interface WorldService
    public void getHomeId (Client arg1, byte arg2, int arg3, InvocationService.ResultListener arg4)
    {
        InvocationMarshaller.ResultMarshaller listener4 = new InvocationMarshaller.ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, GET_HOME_ID, new Object[] {
            Byte.valueOf(arg2), Integer.valueOf(arg3), listener4
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

    /** The method id used to dispatch {@link #setHomeSceneId} requests. */
    public static final int SET_HOME_SCENE_ID = 3;

    // from interface WorldService
    public void setHomeSceneId (Client arg1, int arg2, int arg3, int arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_HOME_SCENE_ID, new Object[] {
            Integer.valueOf(arg2), Integer.valueOf(arg3), Integer.valueOf(arg4), listener5
        });
    }
}
