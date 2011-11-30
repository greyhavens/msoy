//
// $Id$

package com.threerings.msoy.world.data {

import com.threerings.util.Byte;
import com.threerings.util.Integer;

import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

import com.threerings.msoy.world.client.WorldService;
import com.threerings.msoy.world.client.WorldService_HomeResultListener;

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
    /** The method id used to dispatch <code>acceptAndProceed</code> requests. */
    public static const ACCEPT_AND_PROCEED :int = 1;

    // from interface WorldService
    public function acceptAndProceed (arg1 :int, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(ACCEPT_AND_PROCEED, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>completeDjTutorial</code> requests. */
    public static const COMPLETE_DJ_TUTORIAL :int = 2;

    // from interface WorldService
    public function completeDjTutorial (arg1 :InvocationService_InvocationListener) :void
    {
        var listener1 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener1.listener = arg1;
        sendRequest(COMPLETE_DJ_TUTORIAL, [
            listener1
        ]);
    }

    /** The method id used to dispatch <code>ditchFollower</code> requests. */
    public static const DITCH_FOLLOWER :int = 3;

    // from interface WorldService
    public function ditchFollower (arg1 :int, arg2 :InvocationService_InvocationListener) :void
    {
        var listener2 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(DITCH_FOLLOWER, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>followMember</code> requests. */
    public static const FOLLOW_MEMBER :int = 4;

    // from interface WorldService
    public function followMember (arg1 :int, arg2 :InvocationService_InvocationListener) :void
    {
        var listener2 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(FOLLOW_MEMBER, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>getHomeId</code> requests. */
    public static const GET_HOME_ID :int = 5;

    // from interface WorldService
    public function getHomeId (arg1 :int, arg2 :int, arg3 :WorldService_HomeResultListener) :void
    {
        var listener3 :WorldMarshaller_HomeResultMarshaller = new WorldMarshaller_HomeResultMarshaller();
        listener3.listener = arg3;
        sendRequest(GET_HOME_ID, [
            Byte.valueOf(arg1), Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>getHomePageGridItems</code> requests. */
    public static const GET_HOME_PAGE_GRID_ITEMS :int = 6;

    // from interface WorldService
    public function getHomePageGridItems (arg1 :InvocationService_ResultListener) :void
    {
        var listener1 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener1.listener = arg1;
        sendRequest(GET_HOME_PAGE_GRID_ITEMS, [
            listener1
        ]);
    }

    /** The method id used to dispatch <code>inviteToFollow</code> requests. */
    public static const INVITE_TO_FOLLOW :int = 7;

    // from interface WorldService
    public function inviteToFollow (arg1 :int, arg2 :InvocationService_InvocationListener) :void
    {
        var listener2 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(INVITE_TO_FOLLOW, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>setAvatar</code> requests. */
    public static const SET_AVATAR :int = 8;

    // from interface WorldService
    public function setAvatar (arg1 :int, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(SET_AVATAR, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>setHomeSceneId</code> requests. */
    public static const SET_HOME_SCENE_ID :int = 9;

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
