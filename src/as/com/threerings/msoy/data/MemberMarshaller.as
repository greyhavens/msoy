//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.TypedArray;

import com.threerings.util.Integer;
import com.threerings.util.langBoolean;

import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

import com.threerings.msoy.client.MemberService;

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
    public function acknowledgeWarning () :void
    {
        sendRequest(ACKNOWLEDGE_WARNING, [
        ]);
    }

    /** The method id used to dispatch <code>bootFromPlace</code> requests. */
    public static const BOOT_FROM_PLACE :int = 2;

    // from interface MemberService
    public function bootFromPlace (arg1 :int, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(BOOT_FROM_PLACE, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>complainMember</code> requests. */
    public static const COMPLAIN_MEMBER :int = 3;

    // from interface MemberService
    public function complainMember (arg1 :int, arg2 :String) :void
    {
        sendRequest(COMPLAIN_MEMBER, [
            Integer.valueOf(arg1), arg2
        ]);
    }

    /** The method id used to dispatch <code>getCurrentMemberLocation</code> requests. */
    public static const GET_CURRENT_MEMBER_LOCATION :int = 4;

    // from interface MemberService
    public function getCurrentMemberLocation (arg1 :int, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_CURRENT_MEMBER_LOCATION, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>getDisplayName</code> requests. */
    public static const GET_DISPLAY_NAME :int = 5;

    // from interface MemberService
    public function getDisplayName (arg1 :int, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_DISPLAY_NAME, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>inviteAllToBeFriends</code> requests. */
    public static const INVITE_ALL_TO_BE_FRIENDS :int = 6;

    // from interface MemberService
    public function inviteAllToBeFriends (arg1 :TypedArray /* of int */, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(INVITE_ALL_TO_BE_FRIENDS, [
            arg1, listener2
        ]);
    }

    /** The method id used to dispatch <code>inviteToBeFriend</code> requests. */
    public static const INVITE_TO_BE_FRIEND :int = 7;

    // from interface MemberService
    public function inviteToBeFriend (arg1 :int, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(INVITE_TO_BE_FRIEND, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>setAway</code> requests. */
    public static const SET_AWAY :int = 8;

    // from interface MemberService
    public function setAway (arg1 :String, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(SET_AWAY, [
            arg1, listener2
        ]);
    }

    /** The method id used to dispatch <code>setDisplayName</code> requests. */
    public static const SET_DISPLAY_NAME :int = 9;

    // from interface MemberService
    public function setDisplayName (arg1 :String, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(SET_DISPLAY_NAME, [
            arg1, listener2
        ]);
    }

    /** The method id used to dispatch <code>setMuted</code> requests. */
    public static const SET_MUTED :int = 10;

    // from interface MemberService
    public function setMuted (arg1 :int, arg2 :Boolean, arg3 :InvocationService_ConfirmListener) :void
    {
        var listener3 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(SET_MUTED, [
            Integer.valueOf(arg1), langBoolean.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>updateStatus</code> requests. */
    public static const UPDATE_STATUS :int = 11;

    // from interface MemberService
    public function updateStatus (arg1 :String, arg2 :InvocationService_InvocationListener) :void
    {
        var listener2 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(UPDATE_STATUS, [
            arg1, listener2
        ]);
    }
}
}
