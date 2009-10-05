//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.msoy.world.client.WorldService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Byte;
import com.threerings.util.Integer;

/**
 * Provides the implementation of the <code>WorldService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class WorldMarshaller extends InvocationMarshaller
    implements WorldService
{
    /** The method id used to dispatch <code>getHomeId</code> requests. */
    public static const GET_HOME_ID :int = 1;

    // from interface WorldService
    public function getHomeId (arg1 :int, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(GET_HOME_ID, [
            Byte.valueOf(arg1), Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>getHomePageGridItems</code> requests. */
    public static const GET_HOME_PAGE_GRID_ITEMS :int = 2;

    // from interface WorldService
    public function getHomePageGridItems (arg1 :InvocationService_ResultListener) :void
    {
        var listener1 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener1.listener = arg1;
        sendRequest(GET_HOME_PAGE_GRID_ITEMS, [
            listener1
        ]);
    }

    /** The method id used to dispatch <code>setHomeSceneId</code> requests. */
    public static const SET_HOME_SCENE_ID :int = 3;

    // from interface WorldService
    public function setHomeSceneId (arg1 :int, arg2 :int, arg3 :int, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(SET_HOME_SCENE_ID, [
            Integer.valueOf(arg1), Integer.valueOf(arg2), Integer.valueOf(arg3), listener4
        ]);
    }
}
}
