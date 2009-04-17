//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.presents.dobj.DSet;

public interface PartyPlaceObject
{
    /**
     * Get the parties in this place.
     */
    function getParties () :DSet;

    /**
     * Get the occupants in this place, which may include some non-PartyOccupantInfo occupants.
     */
    function getOccupants () :DSet;

    /**
     * Get the leaders.
     */
    function getPartyLeaders () :DSet;
}
}
