//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java PartyBoardService interface.
 */
public interface PartyBoardService extends InvocationService
{
    // from Java interface PartyBoardService
    function createParty (arg1 :Client, arg2 :String, arg3 :int, arg4 :Boolean, arg5 :InvocationService_ResultListener) :void;

    // from Java interface PartyBoardService
    function getPartyBoard (arg1 :Client, arg2 :String, arg3 :InvocationService_ResultListener) :void;

    // from Java interface PartyBoardService
    function getPartyDetail (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface PartyBoardService
    function joinParty (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;
}
}
