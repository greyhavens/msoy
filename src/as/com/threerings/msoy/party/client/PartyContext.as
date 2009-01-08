//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.util.PresentsContext;

import com.threerings.msoy.client.MsoyContext;

/**
 * Provides access to distributed object services used by the party system.
 */
public class PartyContext extends PresentsContext
{
    /**
     * Returns the context we use to obtain basic client services.
     */
    function getMsoyContext () :MsoyContext;

    /**
     * Returns our client object casted as a PartierObject.
     */
    function getPartierObject () :PartierObject;
}
}
