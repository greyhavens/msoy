//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java WorldService interface.
 */
public interface WorldService extends InvocationService
{
    // from Java interface WorldService
    function acceptAndProceed (arg1 :int, arg2 :InvocationService_ConfirmListener) :void;

    // from Java interface WorldService
    function completeDjTutorial (arg1 :InvocationService_InvocationListener) :void;

    // from Java interface WorldService
    function ditchFollower (arg1 :int, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface WorldService
    function followMember (arg1 :int, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface WorldService
    function getHomeId (arg1 :int, arg2 :int, arg3 :WorldService_HomeResultListener) :void;

    // from Java interface WorldService
    function getHomePageGridItems (arg1 :InvocationService_ResultListener) :void;

    // from Java interface WorldService
    function inviteToFollow (arg1 :int, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface WorldService
    function setAvatar (arg1 :int, arg2 :InvocationService_ConfirmListener) :void;

    // from Java interface WorldService
    function setHomeSceneId (arg1 :int, arg2 :int, arg3 :int, arg4 :InvocationService_ConfirmListener) :void;
}
}
