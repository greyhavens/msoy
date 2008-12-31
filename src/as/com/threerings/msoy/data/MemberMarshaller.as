//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.TypedArray;
import com.threerings.msoy.client.MemberService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Byte;
import com.threerings.util.Float;
import com.threerings.util.Integer;
import com.threerings.util.langBoolean;

/**
 * Provides the implementation of the <code>MemberService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class MemberMarshaller extends InvocationMarshaller
    implements MemberService
{
    /** The method id used to dispatch <code>acknowledgeWarning</code> requests. */
    public static const ACKNOWLEDGE_WARNING :int = 1;

    // from interface MemberService
    public function acknowledgeWarning (arg1 :Client) :void
    {
        sendRequest(arg1, ACKNOWLEDGE_WARNING, [

        ]);
    }

    /** The method id used to dispatch <code>bootFromPlace</code> requests. */
    public static const BOOT_FROM_PLACE :int = 2;

    // from interface MemberService
    public function bootFromPlace (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void
    {
        var listener3 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, BOOT_FROM_PLACE, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>complainMember</code> requests. */
    public static const COMPLAIN_MEMBER :int = 3;

    // from interface MemberService
    public function complainMember (arg1 :Client, arg2 :int, arg3 :String) :void
    {
        sendRequest(arg1, COMPLAIN_MEMBER, [
            Integer.valueOf(arg2), arg3
        ]);
    }

    /** The method id used to dispatch <code>dispatchDeferredNotifications</code> requests. */
    public static const DISPATCH_DEFERRED_NOTIFICATIONS :int = 4;

    // from interface MemberService
    public function dispatchDeferredNotifications (arg1 :Client) :void
    {
        sendRequest(arg1, DISPATCH_DEFERRED_NOTIFICATIONS, [

        ]);
    }

    /** The method id used to dispatch <code>ditchFollower</code> requests. */
    public static const DITCH_FOLLOWER :int = 5;

    // from interface MemberService
    public function ditchFollower (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, DITCH_FOLLOWER, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>emailShare</code> requests. */
    public static const EMAIL_SHARE :int = 6;

    // from interface MemberService
    public function emailShare (arg1 :Client, arg2 :Boolean, arg3 :String, arg4 :int, arg5 :TypedArray /* of class java.lang.String */, arg6 :String, arg7 :InvocationService_ConfirmListener) :void
    {
        var listener7 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener7.listener = arg7;
        sendRequest(arg1, EMAIL_SHARE, [
            langBoolean.valueOf(arg2), arg3, Integer.valueOf(arg4), arg5, arg6, listener7
        ]);
    }

    /** The method id used to dispatch <code>followMember</code> requests. */
    public static const FOLLOW_MEMBER :int = 7;

    // from interface MemberService
    public function followMember (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, FOLLOW_MEMBER, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>getABTestGroup</code> requests. */
    public static const GET_ABTEST_GROUP :int = 8;

    // from interface MemberService
    public function getABTestGroup (arg1 :Client, arg2 :String, arg3 :Boolean, arg4 :InvocationService_ResultListener) :void
    {
        var listener4 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, GET_ABTEST_GROUP, [
            arg2, langBoolean.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch <code>getCurrentMemberLocation</code> requests. */
    public static const GET_CURRENT_MEMBER_LOCATION :int = 9;

    // from interface MemberService
    public function getCurrentMemberLocation (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_CURRENT_MEMBER_LOCATION, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>getDisplayName</code> requests. */
    public static const GET_DISPLAY_NAME :int = 10;

    // from interface MemberService
    public function getDisplayName (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_DISPLAY_NAME, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>getGroupHomeSceneId</code> requests. */
    public static const GET_GROUP_HOME_SCENE_ID :int = 11;

    // from interface MemberService
    public function getGroupHomeSceneId (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_GROUP_HOME_SCENE_ID, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>getGroupName</code> requests. */
    public static const GET_GROUP_NAME :int = 12;

    // from interface MemberService
    public function getGroupName (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_GROUP_NAME, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>getHomeId</code> requests. */
    public static const GET_HOME_ID :int = 13;

    // from interface MemberService
    public function getHomeId (arg1 :Client, arg2 :int, arg3 :int, arg4 :InvocationService_ResultListener) :void
    {
        var listener4 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, GET_HOME_ID, [
            Byte.valueOf(arg2), Integer.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch <code>getHomePageGridItems</code> requests. */
    public static const GET_HOME_PAGE_GRID_ITEMS :int = 14;

    // from interface MemberService
    public function getHomePageGridItems (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_HOME_PAGE_GRID_ITEMS, [
            listener2
        ]);
    }

    /** The method id used to dispatch <code>inviteToBeFriend</code> requests. */
    public static const INVITE_TO_BE_FRIEND :int = 15;

    // from interface MemberService
    public function inviteToBeFriend (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_TO_BE_FRIEND, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>inviteToFollow</code> requests. */
    public static const INVITE_TO_FOLLOW :int = 16;

    // from interface MemberService
    public function inviteToFollow (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, INVITE_TO_FOLLOW, [
            Integer.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>loadAllBadges</code> requests. */
    public static const LOAD_ALL_BADGES :int = 17;

    // from interface MemberService
    public function loadAllBadges (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, LOAD_ALL_BADGES, [
            listener2
        ]);
    }

    /** The method id used to dispatch <code>setAvatar</code> requests. */
    public static const SET_AVATAR :int = 18;

    // from interface MemberService
    public function setAvatar (arg1 :Client, arg2 :int, arg3 :Number, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, SET_AVATAR, [
            Integer.valueOf(arg2), Float.valueOf(arg3), listener4
        ]);
    }

    /** The method id used to dispatch <code>setAway</code> requests. */
    public static const SET_AWAY :int = 19;

    // from interface MemberService
    public function setAway (arg1 :Client, arg2 :Boolean, arg3 :String) :void
    {
        sendRequest(arg1, SET_AWAY, [
            langBoolean.valueOf(arg2), arg3
        ]);
    }

    /** The method id used to dispatch <code>setDisplayName</code> requests. */
    public static const SET_DISPLAY_NAME :int = 20;

    // from interface MemberService
    public function setDisplayName (arg1 :Client, arg2 :String, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, SET_DISPLAY_NAME, [
            arg2, listener3
        ]);
    }

    /** The method id used to dispatch <code>setHomeSceneId</code> requests. */
    public static const SET_HOME_SCENE_ID :int = 21;

    // from interface MemberService
    public function setHomeSceneId (arg1 :Client, arg2 :int, arg3 :int, arg4 :int, arg5 :InvocationService_ConfirmListener) :void
    {
        var listener5 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, SET_HOME_SCENE_ID, [
            Integer.valueOf(arg2), Integer.valueOf(arg3), Integer.valueOf(arg4), listener5
        ]);
    }

    /** The method id used to dispatch <code>trackClientAction</code> requests. */
    public static const TRACK_CLIENT_ACTION :int = 22;

    // from interface MemberService
    public function trackClientAction (arg1 :Client, arg2 :String, arg3 :String) :void
    {
        sendRequest(arg1, TRACK_CLIENT_ACTION, [
            arg2, arg3
        ]);
    }

    /** The method id used to dispatch <code>trackTestAction</code> requests. */
    public static const TRACK_TEST_ACTION :int = 23;

    // from interface MemberService
    public function trackTestAction (arg1 :Client, arg2 :String, arg3 :String) :void
    {
        sendRequest(arg1, TRACK_TEST_ACTION, [
            arg2, arg3
        ]);
    }

    /** The method id used to dispatch <code>trackVectorAssociation</code> requests. */
    public static const TRACK_VECTOR_ASSOCIATION :int = 24;

    // from interface MemberService
    public function trackVectorAssociation (arg1 :Client, arg2 :String) :void
    {
        sendRequest(arg1, TRACK_VECTOR_ASSOCIATION, [
            arg2
        ]);
    }

    /** The method id used to dispatch <code>updateAvailability</code> requests. */
    public static const UPDATE_AVAILABILITY :int = 25;

    // from interface MemberService
    public function updateAvailability (arg1 :Client, arg2 :int) :void
    {
        sendRequest(arg1, UPDATE_AVAILABILITY, [
            Integer.valueOf(arg2)
        ]);
    }

    /** The method id used to dispatch <code>updateStatus</code> requests. */
    public static const UPDATE_STATUS :int = 26;

    // from interface MemberService
    public function updateStatus (arg1 :Client, arg2 :String, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_STATUS, [
            arg2, listener3
        ]);
    }
}
}
