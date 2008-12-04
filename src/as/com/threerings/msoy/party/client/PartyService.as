//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;

/**
 * An ActionScript version of the Java PartyService interface.
 */
public interface PartyService extends InvocationService
{
    // from Java interface PartyService
    function assignLeader (arg1 :Client, arg2 :int, arg3 :InvocationService_ConfirmListener) :void;

    // from Java interface PartyService
    function bootMember (arg1 :Client, arg2 :InvocationService_ConfirmListener) :void;

    // from Java interface PartyService
    function leaveParty (arg1 :Client, arg2 :InvocationService_ConfirmListener) :void;
}
}
