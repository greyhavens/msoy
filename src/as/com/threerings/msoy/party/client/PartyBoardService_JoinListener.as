//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java PartyBoardService_JoinListener interface.
 */
public interface PartyBoardService_JoinListener
    extends InvocationService_InvocationListener
{
    // from Java PartyBoardService_JoinListener
    function foundParty (arg1 :String, arg2 :int) :void
}
}
