//
// $Id$

package com.threerings.msoy.client {

import flash.utils.ByteArray;
import com.threerings.io.TypedArray;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * An ActionScript version of the Java MemberService interface.
 */
public interface MemberService extends InvocationService
{
    // from Java interface MemberService
    function acknowledgeNotifications (arg1 :Client, arg2 :TypedArray /* of int */, arg3 :InvocationService_InvocationListener) :void;

    // from Java interface MemberService
    function followMember (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function getCurrentMemberLocation (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function getDisplayName (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function getGroupName (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function getHomeId (arg1 :Client, arg2 :int, arg3 :int, arg4 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function inviteToBeFriend (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function inviteToFollow (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function issueInvitation (arg1 :Client, arg2 :MemberName, arg3 :InvocationService_ResultListener) :void;

    // from Java interface MemberService
    function setAvatar (arg1 :Client, arg2 :int, arg3 :Number, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function setAway (arg1 :Client, arg2 :Boolean, arg3 :String) :void;

    // from Java interface MemberService
    function setDisplayName (arg1 :Client, arg2 :String, arg3 :InvocationService_InvocationListener) :void;

    // from Java interface MemberService
    function setHomeSceneId (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface MemberService
    function updateAvailability (arg1 :Client, arg2 :int) :void;
}
}
