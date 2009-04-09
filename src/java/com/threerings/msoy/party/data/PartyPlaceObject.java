//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.OccupantInfo;

/**
 * An interface implemented by places that host partiers.
 */
public interface PartyPlaceObject
{
    /**
     * Get the party summary set.
     */
    DSet<PartySummary> getParties ();

    /**
     * Add a party summary.
     */
    void addToParties (PartySummary summary);

    /**
     * Remove a party summary.
     */
    void removeFromParties (Comparable<?> key);

    /**
     * Get the occupants of this woot-woot party place, which may include some
     * non-PartyOccupantInfo occupants.
     */
    DSet<OccupantInfo> getOccupantInfo ();
}
