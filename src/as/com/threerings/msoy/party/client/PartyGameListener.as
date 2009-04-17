//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.presents.dobj.DSet_Entry;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.party.data.PartyOccupantInfo;
import com.threerings.msoy.party.data.PartySummary;

/**
 * Listens on a GameObject and calls the appropriate party-related methods
 * on the supplied backend.
 */
public class PartyGameListener
    implements SetListener
{
    public function PartyGameListener (gameObj :PlaceObject, backend :Object)
    {
        _gameObj = gameObj;
        _backend = backend;

        _gameObj.addListener(this);
    }

    public function shutdown () :void
    {
        _gameObj.removeListener(this);
        _gameObj = null;
        _backend = null;
    }

    // from SetListener
    public function entryAdded (event :EntryAddedEvent) :void
    {
        var name :String = event.getName();
        if (name == PlaceObject.OCCUPANT_INFO) {
            playerChanged(null, event.getEntry());

        } else if (name == PARTIES) {
            var summary :PartySummary = event.getEntry() as PartySummary;
            callUserCode("game_partyEntered_v1", summary.id);
        }
    }

    // from SetListener
    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var name :String = event.getName();
        if (name == PlaceObject.OCCUPANT_INFO) {
            playerChanged(event.getOldEntry(), event.getEntry());
        }
    }

    // from SetListener
    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        var name :String = event.getName();
        if (name == PlaceObject.OCCUPANT_INFO) {
            playerChanged(event.getOldEntry(), null);

        } else if (name == PARTIES) {
            callUserCode("game_partyLeft_v1", event.getKey());
        }
    }

    /**
     * Helper.
     */
    protected function playerChanged (oldE :DSet_Entry, newE :DSet_Entry) :void
    {
        var oldId :int = (oldE is PartyOccupantInfo) ? PartyOccupantInfo(oldE).getPartyId() : 0;
        var newId :int = (newE is PartyOccupantInfo) ? PartyOccupantInfo(newE).getPartyId() : 0;
        if (oldId != newId) {
            if (oldId != 0) {
                callUserCode("party_playerLeft_v1", oldId, getMemberId(oldE));
            }
            if (newId != 0) {
                callUserCode("party_playerEntered_v1", newId, getMemberId(newE));
            }
        }
    }

    /**
     * Helper.
     */
    protected function getMemberId (occInfo :DSet_Entry) :int
    {
        return MemberName(OccupantInfo(occInfo).username).getMemberId();
    }

    protected function callUserCode (... args) :*
    {
        // TODO: strongly type the backend? (interface needed)
        return _backend.callUserCode.apply(null, args);
    }

    protected var _gameObj :PlaceObject;

    protected var _backend :Object;

    /** Hopefully the PartySummary set is called this. */
    protected static const PARTIES :String = "parties";
}
}
