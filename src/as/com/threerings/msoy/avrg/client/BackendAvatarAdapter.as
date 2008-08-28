// 
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.MethodQueue;
import com.threerings.util.Util;

import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.dobj.EntryUpdatedEvent;

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.avrg.data.AVRGameObject;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.RoomObject;


/**
 * Tracks player movement within a room and dispatches notifications to user code functions.
 */
public class BackendAvatarAdapter
{
    /**
     * Creates a new adapter for a game and room.
     */
    public function BackendAvatarAdapter (
        gameObj : AVRGameObject, roomObj :RoomObject, funcs :Object, playerMoved :String, 
        actorStateChanged :String, actorAppearanceChanged :String)
    {
        _gameObj = gameObj;
        _roomObj = roomObj;

        function checkIt (name :String) :Function {
            if (funcs[name] == null) {
                throw new Error("User code " + name + " not found");
            }
            return funcs[name] as Function;
        }

        _playerMoved = checkIt(playerMoved);
        _actorStateChanged = checkIt(actorStateChanged);
        _actorAppearanceChanged = checkIt(actorAppearanceChanged);

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

    protected function resolveMemberId (occOid :int) :int
    {
        // find the occupant info for this body
        var occInfo :OccupantInfo = _roomObj.occupantInfo.get(occOid) as OccupantInfo;
        if (occInfo) {
            // and its name
            var name :MemberName = occInfo.username as MemberName;
            // and make sure it's a player
            if (name != null && _gameObj.getOccupantInfo(name)) {
                return name.getMemberId();
            }
        }
        return 0;
    }

    protected function callUserCode (fn :Function, ...args) :void
    {
        try {
            fn.apply(null, args);

        } catch (err :Error) {
            BackendUtils.log.warning("Error in user-code: " + err);
            BackendUtils.log.logStackTrace(err);
        }
    }

    protected function entryUpdated (event :EntryUpdatedEvent) :void
    {
        var memberId :int;
        if (event.getName() == SpotSceneObject.OCCUPANT_LOCS) {
            memberId = resolveMemberId(event.getEntry().getKey() as int);
            if (memberId != 0) {
                if (_targetId != 0) {
                    callUserCode(_playerMoved, _targetId, memberId);
                } else {
                    callUserCode(_playerMoved, memberId);
                }
            }
        } else if (event.getName() == PlaceObject.OCCUPANT_INFO) {
            memberId = resolveMemberId(event.getEntry().getKey() as int);
            if (memberId != 0) {
                var info :MemberInfo = event.getEntry() as MemberInfo;
                var oldInfo :MemberInfo = event.getOldEntry() as MemberInfo;
                if (info.status != oldInfo.status || 
                    !Util.equals(info.getMedia(), oldInfo.getMedia())) {
                    // Call later so that sprites are sure to have updated
                    MethodQueue.callLater(function () :void {
                        if (_targetId != 0) {
                            callUserCode(_actorAppearanceChanged, _targetId, memberId);
                        } else {
                            callUserCode(_actorAppearanceChanged, memberId);
                        }
                    });
                }

                var state :String = info.getState();
                if (state != oldInfo.getState()) {
                    // Call later so that sprites are sure to have updated
                    MethodQueue.callLater(function () :void {
                        if (_targetId != 0) {
                            callUserCode(_actorStateChanged, _targetId, memberId, state);
                        } else {
                            callUserCode(_actorStateChanged, memberId, state);
                        }
                    });
                }
            }
        }
    }

    protected var _gameObj :AVRGameObject;
    protected var _roomObj :RoomObject;
    protected var _playerMoved :Function;
    protected var _actorStateChanged :Function;
    protected var _actorAppearanceChanged :Function;
    protected var _targetId :int;
    protected var _listener :SetAdapter = new SetAdapter(null, entryUpdated);
}
}
