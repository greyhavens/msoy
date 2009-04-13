//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java PartyBoardService interface.
 */
public interface PartyBoardService extends InvocationService
{
    // from Java interface PartyBoardService
    function createParty (arg1 :Client, arg2 :Currency, arg3 :int, arg4 :String, arg5 :int, arg6 :Boolean, arg7 :PartyBoardService_JoinListener) :void;

    // from Java interface PartyBoardService
    function getCreateCost (arg1 :Client, arg2 :InvocationService_ResultListener) :void;

    // from Java interface PartyBoardService
    function getPartyBoard (arg1 :Client, arg2 :String, arg3 :InvocationService_ResultListener) :void;

    // from Java interface PartyBoardService
    function getPartyDetail (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;

    // from Java interface PartyBoardService
    function locateParty (arg1 :Client, arg2 :int, arg3 :PartyBoardService_JoinListener) :void;
}
}
