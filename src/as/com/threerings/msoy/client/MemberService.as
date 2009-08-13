//
// $Id$

package com.threerings.msoy.client {

import com.threerings.io.TypedArray;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java MemberService interface.
 */
public interface MemberService extends InvocationService
{
    // from Java interface MemberService
    function acknowledgeWarning () :void;

    // from Java interface MemberService
    function bootFromPlace (arg1 :int, arg2 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function complainMember (arg1 :int, arg2 :String) :void;

    // from Java interface MemberService
    function ditchFollower (arg1 :int, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface MemberService
    function followMember (arg1 :int, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface MemberService
    function getCurrentMemberLocation (arg1 :int, arg2 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function getDisplayName (arg1 :int, arg2 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function getHomeId (arg1 :int, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function inviteAllToBeFriends (arg1 :TypedArray /* of int */, arg2 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function inviteToBeFriend (arg1 :int, arg2 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function inviteToFollow (arg1 :int, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface MemberService
    function setAvatar (arg1 :int, arg2 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function setAway (arg1 :String, arg2 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function setDisplayName (arg1 :String, arg2 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function setHomeSceneId (arg1 :int, arg2 :int, arg3 :int, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function setMuted (arg1 :int, arg2 :Boolean, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function updateStatus (arg1 :String, arg2 :InvocationService_InvocationListener) :void;
}
}
