//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.msoy.world.client.WorldService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
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
    /** The method id used to dispatch <code>getGroupHomeSceneId</code> requests. */
    public static const GET_GROUP_HOME_SCENE_ID :int = 1;

    // from interface WorldService
    public function getGroupHomeSceneId (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_GROUP_HOME_SCENE_ID, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>getHomePageGridItems</code> requests. */
    public static const GET_HOME_PAGE_GRID_ITEMS :int = 2;

    // from interface WorldService
    public function getHomePageGridItems (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_HOME_PAGE_GRID_ITEMS, [
            listener2
        ]);
    }
}
}
