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

import com.threerings.msoy.party.data.PartyLeader;
import com.threerings.msoy.party.data.PartyOccupantInfo;
import com.threerings.msoy.party.data.PartyPlaceObject;
import com.threerings.msoy.party.data.PartySummary;

/**
 * Listens on a GameObject and calls the appropriate party-related methods
 * on the supplied backend.
 */
public class PartyGameHelper
    implements SetListener
{
    public function init (gameObj :PartyPlaceObject, callUserCode :Function) :void
    {
        _gameObj = gameObj;
        PlaceObject(_gameObj).addListener(this);
        _callUserCode = callUserCode;
    }

    public function shutdown () :void
    {
        PlaceObject(_gameObj).removeListener(this);
        _gameObj = null;
        _callUserCode = null;
    }

    public function populateProperties (o :Object) :void
    {
        o["game_getPartyIds_v1"] = game_getPartyIds_v1;
        o["player_getPartyId_v1"] = player_getPartyId_v1;

        o["party_getName_v1"] = getName_v1;
        o["party_getGroupId_v1"] = getGroupId_v1;
        o["party_getGroupName_v1"] = getGroupName_v1;
        o["party_getLeaderId_v1"] = getLeaderId_v1;
        o["party_getPlayerIds_v1"] = getPlayerIds_v1;
//        o["party_moveToRoom_v1"] = moveToRoom_v1;
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

        } else if (name == PARTY_LEADERS) {
            var leader :PartyLeader = event.getEntry() as PartyLeader;
            callUserCode("party_leaderChanged_v1", leader.partyId, leader.leaderId);
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

    protected function game_getPartyIds_v1 () :Array
    {
        return _gameObj.getParties().toArray().map(function (party :PartySummary, ... rest) :int {
            return party.id;
        });
    }

    protected function player_getPartyId_v1 (playerId :int) :int
    {
        // Note: we could make this not O(n) by watching party changes
        for each (var o :Object in _gameObj.getOccupants().toArray()) {
            if (playerId == getMemberId(o)) {
                var poi :PartyOccupantInfo = o as PartyOccupantInfo;
                return (poi == null) ? 0 : poi.getPartyId();
            }
        }
        return 0;
    }

    protected function getName_v1 (partyId :int) :String
    {
        var party :PartySummary = getSummary(partyId);
        return (party == null) ? null : party.name;
    }

    protected function getGroupId_v1 (partyId :int) :int
    {
        var party :PartySummary = getSummary(partyId);
        return (party == null) ? 0 : party.group.getGroupId();
    }

    protected function getGroupName_v1 (partyId :int) :String
    {
        var party :PartySummary = getSummary(partyId);
        return (party == null) ? null : party.group.toString();
    }

    protected function getLeaderId_v1 (partyId :int) :int
    {
        var leader :PartyLeader = _gameObj.getPartyLeaders().get(partyId) as PartyLeader;
        return (leader == null) ? 0 : leader.leaderId;
    }

    protected function getPlayerIds_v1 (partyId :int) :Array
    {
        // NOTE: we could make this not O(n) by watching comings and goings and
        // maintaining a list for each party.
        var ids :Array = [];
        if (partyId != 0) { // don't let people scam-up a list of all unpartied folks
            for each (var o :Object in _gameObj.getOccupants().toArray()) {
                if ((o is PartyOccupantInfo) && (partyId == PartyOccupantInfo(o).getPartyId())) {
                    ids.push(getMemberId(o));
                }
            }
        }
        return ids;
    }

//    protected function moveToRoom_v1 (partyId :int, roomId :int, ... future)  :void
//    {
//    }

    /**
     * Helper for reporting players entering and leaving parties.
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
     * Helper for getting a memberId from something we know to be an OccupantInfo.
     */
    protected function getMemberId (occInfo :Object) :int
    {
        // Do we need to watch out for non-MemberNames?
        return MemberName(OccupantInfo(occInfo).username).getId();
    }

    /**
     * Helper for extracting a party summary.
     */
    protected function getSummary (partyId :int) :PartySummary
    {
        return _gameObj.getParties().get(partyId) as PartySummary;
    }

    protected function callUserCode (... args) :*
    {
        return _callUserCode.apply(null, args);
    }

    protected var _callUserCode :Function;

    protected var _gameObj :PartyPlaceObject;

    /** Hopefully the PartySummary set is called this. */
    protected static const PARTIES :String = "parties";

    /** Hopefully the PartyLeader set is called this. */
    protected static const PARTY_LEADERS :String = "partyLeaders";
}
}
