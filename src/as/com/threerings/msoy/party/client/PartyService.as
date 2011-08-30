//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java PartyService interface.
 */
public interface PartyService extends InvocationService
{
    // from Java interface PartyService
    function assignLeader (arg1 :int, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface PartyService
    function bootMember (arg1 :int, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface PartyService
    function inviteMember (arg1 :int, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface PartyService
    function moveParty (arg1 :int, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface PartyService
    function setGame (arg1 :int, arg2 :int, arg3 :int, arg4 :InvocationService_InvocationListener) :void;

    // from Java interface PartyService
    function updateDisband (arg1 :Boolean, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface PartyService
    function updateRecruitment (arg1 :int, arg2 :InvocationService_InvocationListener) :void;

    // from Java interface PartyService
    function updateStatus (arg1 :String, arg2 :InvocationService_InvocationListener) :void;
}
}
