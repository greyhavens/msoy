//
// $Id$

package com.threerings.msoy.party.server;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.data.MsoyUserObject;

import com.threerings.msoy.party.data.PartyOccupantInfo;
import com.threerings.msoy.party.data.PartyPlaceObject;
import com.threerings.msoy.party.data.PartySummary;

/**
 * Server-side utilities for managing a PartyPlaceObject.
 */
public class PartyPlaceUtil
{
    /**
     * Possibly add the specified party to the place.
     */
    public static void addParty (MsoyUserObject userObj, PartyPlaceObject placeObj)
    {
        PartySummary summary = userObj.getParty();
        if ((summary != null) && !placeObj.getParties().containsKey(summary.id)) {
            placeObj.addToParties(summary);
        }
    }

    /**
     * Maybe remove a party summary from the place- should be called AFTER we any current
     * party user's occupant info.
     */
    public static void removeParty (MsoyUserObject userObj, PartyPlaceObject placeObj)
    {
        removeParty(userObj.getParty(), placeObj);
    }

    /**
     * Used by the PartyRegistry when the user is entering/leaving the party. The new
     * PartySummary will already be set on the user, so this has us provide the old one.
     */
    public static void removeParty (PartySummary summary, PartyPlaceObject placeObj)
    {
        if ((summary == null) || !placeObj.getParties().containsKey(summary.id)) {
            return;
        }
        for (OccupantInfo info : placeObj.getOccupants()) {
            if ((info instanceof PartyOccupantInfo) &&
                    (((PartyOccupantInfo) info).getPartyId() == summary.id)) {
                return; // there's still a partier here!
            }
        }
        placeObj.removeFromParties(summary.id);
    }
}
