//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.util.PresentsContext;

import com.threerings.msoy.party.data.PartierObject;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Provides access to distributed object services used by the party system.
 */
public interface PartyContext extends PresentsContext
{
    /**
     * Returns the context we use to obtain basic client services.
     */
    function getWorldContext () :WorldContext;

    /**
     * Returns our client object casted as a PartierObject.
     */
    function getPartierObject () :PartierObject;
}
}
