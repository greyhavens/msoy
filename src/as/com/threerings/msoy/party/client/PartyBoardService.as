//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ResultListener;

import com.threerings.msoy.money.data.all.Currency;

/**
 * An ActionScript version of the Java PartyBoardService interface.
 */
public interface PartyBoardService extends InvocationService
{
    // from Java interface PartyBoardService
    function createParty (arg1 :Currency, arg2 :int, arg3 :String, arg4 :int, arg5 :Boolean, arg6 :PartyBoardService_JoinListener) :void;

    // from Java interface PartyBoardService
    function getCreateCost (arg1 :InvocationService_ResultListener) :void;

    // from Java interface PartyBoardService
    function getPartyBoard (arg1 :int, arg2 :InvocationService_ResultListener) :void;

    // from Java interface PartyBoardService
    function getPartyDetail (arg1 :int, arg2 :InvocationService_ResultListener) :void;

    // from Java interface PartyBoardService
    function locateParty (arg1 :int, arg2 :PartyBoardService_JoinListener) :void;
}
}
