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
     * Get the occupants of this woot-woot party place, which may include some
     * non-PartyOccupantInfo occupants.
     */
    DSet<OccupantInfo> getOccupants ();

    /**
     * Get the party summary set.
     */
    DSet<PartySummary> getParties ();

    /**
     * Get the leader set.
     */
    DSet<PartyLeader> getPartyLeaders ();

    /**
     * Add a party summary.
     */
    void addToParties (PartySummary summary);

    /**
     * Remove a party summary.
     */
    void removeFromParties (Comparable<?> key);

    /**
     * Add a leader.
     */
    void addToPartyLeaders (PartyLeader leader);

    /**
     * Update a leader.
     */
    void updatePartyLeaders (PartyLeader leader);

    /**
     * Remove a leader.
     */
    void removeFromPartyLeaders (Comparable<?> key);
}
