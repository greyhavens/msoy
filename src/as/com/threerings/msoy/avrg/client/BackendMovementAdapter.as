// 
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.dobj.EntryUpdatedEvent;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.avrg.data.AVRGameObject;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.room.data.RoomObject;


/**
 * Tracks player movement within a room and dispatches notifications to user code functions.
 */
public class BackendMovementAdapter
{
    /**
     * Creates a new adapter for a game and room.
     */
    public function BackendMovementAdapter (
        gameObj : AVRGameObject, roomObj :RoomObject, funcs :Object, playerMoved :String)
    {
        _gameObj = gameObj;
        _roomObj = roomObj;
        _playerMoved = funcs[playerMoved];

        if (_playerMoved == null) {
            throw new Error("User code " + playerMoved + " not found");
        }

        _roomObj.addListener(_listener);
    }

    /**
     * Pass a target id as the first argument. This allows the user code to determine which
     * room sub-control to notify of movements.
     */
    public function setTargetId (targetId :int) :void
    {
        _targetId = targetId;
    }

    /**
     * Stop listening for movements.
     */
    public function release () :void
    {
        _roomObj.removeListener(_listener);
    }

    protected function entryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == SpotSceneObject.OCCUPANT_LOCS) {
            var oid :int = event.getEntry().getKey() as int;
            // find the occupant info for this body
            var occInfo :OccupantInfo = _roomObj.occupantInfo.get(oid) as OccupantInfo;
            if (occInfo) {
                // and its name
                var name :MemberName = occInfo.username as MemberName;
                // and make sure it's a player
                if (name != null && _gameObj.getOccupantInfo(name)) {
                    if (_targetId != 0) {
                        _playerMoved(_targetId, name.getMemberId());
                    } else {
                        _playerMoved(name.getMemberId());
                    }
                }
            }
        }
    }

    protected var _gameObj :AVRGameObject;
    protected var _roomObj :RoomObject;
    protected var _playerMoved :Function;
    protected var _targetId :int;
    protected var _listener :SetAdapter = new SetAdapter(null, entryUpdated);
}
}
