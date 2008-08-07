//
// $Id$

package com.threerings.msoy.avrg.client {

import flash.display.DisplayObject;
import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.util.Iterator;
import com.threerings.util.Log;
import com.threerings.util.MethodQueue;
import com.threerings.util.Name;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.SetAdapter;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.LocationObserver;
import com.threerings.crowd.client.OccupantAdapter;
import com.threerings.crowd.client.OccupantObserver;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.client.ControlBackend;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.room.client.MemberSprite;
import com.threerings.msoy.room.client.MobSprite;
import com.threerings.msoy.room.client.OccupantSprite;
import com.threerings.msoy.room.client.RoomMetrics;
import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.client.WorldContext;
import com.threerings.msoy.room.client.layout.RoomLayout;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.RoomObject;

import com.threerings.msoy.avrg.data.AVRGameObject;

public class AVRGameBackend extends ControlBackend
{
    public static const log :Log = Log.getLog(AVRGameBackend);

    public function AVRGameBackend (
        wctx :WorldContext, gctx :GameContext, ctrl :AVRGameController, gameObj :AVRGameObject)
    {
        _wctx = wctx;
        _gctx = gctx;
        _ctrl = ctrl;
        _playerObj = _gctx.getPlayerObject();

        _gameObj = gameObj;
        _gameObj.addListener(_gameListener);
        _gctx.getClient().getClientObject().addListener(_playerListener);

        _wctx.getLocationDirector().addLocationObserver(_locationObserver);
        _wctx.getOccupantDirector().addOccupantObserver(_occupantObserver);

        // will be null if not a room
        _roomObj = (_wctx.getLocationDirector().getPlaceObject() as RoomObject);
        if (_roomObj != null) {
            _roomObj.addListener(_movementListener);
        }

        _stateBackend = new StateControlBackend(wctx, gctx, this, gameObj);
        _questBackend = new QuestControlBackend(wctx, gctx, this, gameObj);
    }

    // from ControlBackend
    override public function shutdown () :void
    {
         _stateBackend.shutdown();
         _questBackend.shutdown();

         if (_roomObj != null) {
             _roomObj.removeListener(_movementListener);
             _roomObj = null;
         }

         _wctx.getLocationDirector().removeLocationObserver(_locationObserver);
         _wctx.getOccupantDirector().removeOccupantObserver(_occupantObserver);

         _gctx.getClient().getClientObject().removeListener(_playerListener);
         _gameObj.removeListener(_gameListener);

         super.shutdown();
    }

    public function get room () :RoomObject
    {
        return _roomObj;
    }

    public function panelResized () :void
    {
        MethodQueue.callLater(callUserCode, [ "panelResized_v1" ]);
    }

    public function tutorialEvent (eventName :String) :void
    {
        callUserCode("messageReceived_v1", "tutorialEvent", eventName);
    }

    public function requestMobSprite (id :String) :DisplayObject
    {
        return callUserCode("requestMobSprite_v1", id) as DisplayObject;
    }

    public function mobRemoved (id :String) :void
    {
        callUserCode("mobRemoved_v1", id);
    }

    public function mobAppearanceChanged (
        id :String, locArray :Array, orient :Number, moving :Boolean, idle :Boolean) :void
    {
        callUserCode("mobAppearanceChanged_v1", id, locArray, orient, moving, idle);
    }

    public function hitTestPoint (x :Number, y :Number, shapeFlag :Boolean = false) :Boolean
    {
        return callUserCode("hitTestPoint_v1", x, y, shapeFlag) as Boolean;
    }

    public function isPlaying () :Boolean
    {
        return _wctx.getGameDirector().getGameId() == _ctrl.getGameId();
    }

    // from ControlBackend
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        // AVRGameControl
        o["hasControl_v1"] = hasControl_v1;
        o["getStageSize_v1"] = getStageSize_v1;
        o["getRoomBounds_v1"] = getRoomBounds_v1;
        o["stageToRoom_v1"] = stageToRoom_v1;
        o["roomToStage_v1"] = roomToStage_v1;
        o["locationToRoom_v1"] = locationToRoom_v1;
        o["deactivateGame_v1"] = deactivateGame_v1;
        o["getRoomId_v1"] = getRoomId_v1;
        o["getPlayerId_v1"] = getPlayerId_v1;
        o["isPlayerHere_v1"] = isPlayerHere_v1;
        o["getPlayerIds_v1"] = getPlayerIds_v1;
        o["spawnMob_v1"] = spawnMob_v1;
        o["despawnMob_v1"] = despawnMob_v1;
        o["setTicker_v1"] = setTicker_v1;

        // MobControl helpers
        o["setMobDecoration_v1"] = setMobDecoration_v1;
        o["setMobHotSpot_v1"] = setMobHotSpot_v1;

        // AVRG Avatar functions
        o["getAvatarInfo_v1"] = getAvatarInfo_v1;

        o["playAvatarAction_v1"] = playAvatarAction_v1;
        o["setAvatarState_v1"] = setAvatarState_v1;
        o["setAvatarMoveSpeed_v1"] = setAvatarMoveSpeed_v1;
        o["setAvatarLocation_v1"] = setAvatarLocation_v1;
        o["setAvatarOrientation_v1"] = setAvatarOrientation_v1;

        _stateBackend.populateSubProperties(o);
        _questBackend.populateSubProperties(o);
    }

    protected function hasControl_v1 () :Boolean
    {
        var view :RoomObjectView = _wctx.getTopPanel().getPlaceView() as RoomObjectView;
        return view && view.getRoomObjectController().hasAVRGameControl();
    }

    protected function getStageSize_v1 (full :Boolean) :Rectangle
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            if (full) {
                return _wctx.getTopPanel().getPlaceViewBounds();
            }
            return view.getScrollSize();
        }
        return null;
    }

    protected function getRoomBounds_v1 () :Rectangle
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            var metrics :RoomMetrics = view.layout.metrics;
            return new Rectangle(0, 0, metrics.sceneWidth, metrics.sceneHeight);
        }
        return null;
    }

    protected function stageToRoom_v1 (p :Point) :Point
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            p = view.globalToLocal(p);
            p.x -= view.getScrollOffset();
            return p;
        }
        return null;
    }

    protected function roomToStage_v1 (p :Point) :Point
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            return view.localToGlobal(new Point(p.x + view.getScrollOffset(), p.y));
        }
        return null;
    }

    protected function locationToRoom_v1 (x :Number, y :Number, z :Number)  :Point
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            return view.layout.locationToPoint(new MsoyLocation(x, y, z));
        }

        return null;
    }

    protected function deactivateGame_v1 () :Boolean
    {
        if (!isPlaying()) {
            return false;
        }
        _wctx.getGameDirector().leaveAVRGame();
        return true;
    }

    protected function getRoomId_v1 () :int
    {
        var scene :Scene = _wctx.getSceneDirector().getScene();
        return scene != null ? scene.getId() : -1;
    }

    protected function getPlayerId_v1 () :int
    {
        return _wctx.getMemberObject().getMemberId();
    }

    protected function isPlayerHere_v1 (id :int) :Boolean
    {
        if (isPlaying() && _roomObj != null) {
            var fakeName :MemberName = new MemberName("", id);
            return (_gameObj.getOccupantInfo(fakeName) != null &&
                    _roomObj.getOccupantInfo(fakeName) != null);
        }
        return false;
    }

    protected function getPlayerName_v1 (playerId :int) :String
    {
        var occInfo :OccupantInfo = _roomObj.getOccupantInfo(new MemberName("", playerId));
        if (occInfo != null && occInfo is MemberInfo && occInfo.username != null) {
            return occInfo.username.toString();
        }
        return null;
    }

    protected function getPlayerIds_v1 () :Array
    {
        if (!isPlaying() || _roomObj == null) {
            return null;
        }

        var result :Array = new Array();
        var iterator :Iterator = _gameObj.occupantInfo.iterator();
        while (iterator.hasNext()) {
            var name :MemberName = OccupantInfo(iterator.next()).username as MemberName;
            if (name != null) {
                result.push(name.getMemberId());
            }
        }
        return result;
    }

    protected function spawnMob_v1 (mobId :String, mobName :String) :Boolean
    {
        if (mobId && mobName && isPlaying()) {
            var sprite :MobSprite = getMobSprite(mobId);
            if (sprite == null) {
                _roomObj.roomService.spawnMob(
                    _wctx.getClient(), _ctrl.getGameId(), mobId, mobName,
                    loggingInvocationListener("spawnMob"));
                return true;
            }
        }
        return false;
    }

    protected function despawnMob_v1 (mobId :String) :Boolean
    {
        if (mobId && isPlaying()) {
            var sprite :MobSprite = getMobSprite(mobId);
            if (sprite != null) {
                _roomObj.roomService.despawnMob(
                    _wctx.getClient(), _ctrl.getGameId(), mobId,
                    loggingInvocationListener("despawnMob"));
                return true;
            }
        }
        return false;
    }

    protected function setMobDecoration_v1 (
        mobId :String, decoration :DisplayObject, add :Boolean) :Boolean
    {
        if (isPlaying()) {
            var sprite :MobSprite = getMobSprite(mobId);
            if (sprite != null) {
                if (add) {
                    sprite.addDecoration(decoration);
                } else {
                    sprite.removeDecoration(decoration);
                }
                return true;
            }
        }
        return false;
    }

    protected function setMobHotSpot_v1 (
        mobId :String, x :Number, y :Number, height :Number = NaN) :void
    {
        if (isPlaying()) {
            var sprite :MobSprite = getMobSprite(mobId);
            if (sprite != null) {
                sprite.setHotSpot(x, y, height);
            }
        }
    }

    protected function setTicker_v1 (tickerName :String, msOfDelay :int) :void
    {
        if (isPlaying() && tickerName != null) {
            _gameObj.avrgService.setTicker(
                _gctx.getClient(), tickerName, msOfDelay, loggingConfirmListener("setTicker"));
        }
    }

    protected function getAvatarInfo_v1 (playerId :int) :Object
    {
        var sprite :MemberSprite = getAvatarSprite(playerId);
        if (sprite != null) {
            return [
                sprite.getActorInfo().username.toString(),
                sprite.getState(),
                sprite.getLocation().x,
                sprite.getLocation().y,
                sprite.getLocation().z,
                sprite.getLocation().orient,
                sprite.getMoveSpeed(),
                sprite.isMoving(),
                sprite.isIdle(),
                sprite.getBounds(sprite.stage)
            ];
        }
        return null;
    }

    protected function playAvatarAction_v1 (action :String) :Boolean
    {
        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.sendMessage(action, null, true);
            return true;
        }
        return false;
    }

    protected function setAvatarState_v1 (state :String) :Boolean
    {
        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setState(state);
            return true;
        }
        return false;
    }

    /**
     * Called by user code to configure the default move speed for this actor.
     */
    protected function setAvatarMoveSpeed_v1 (pixelsPerSecond :Number) :Boolean
    {
        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setMoveSpeedFromUser(pixelsPerSecond);
            return true;
        }
        return false;
    }

    /**
     * Called by user code when it wants to change the actor's scene location.
     */
    protected function setAvatarLocation_v1 (
        x :Number, y :Number, z: Number, orient :Number) :Boolean
    {
        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setLocationFromUser(x, y, z, orient);
            return true;
        }
        return false;
    }

    /**
     * Called by user code when it wants to change the actor's scene orientation.
     */
    protected function setAvatarOrientation_v1 (orient :Number) :Boolean
    {
        var sprite :MemberSprite = getMySprite();
        if (sprite != null) {
            sprite.setOrientationFromUser(orient);
            return true;
        }
        return false;
    }

    protected function playerEntered (name :Name) :void
    {
        if (name is MemberName) {
            callUserCode("playerEntered_v1", MemberName(name).getMemberId());
        }
    }

    protected function playerLeft (name :Name) :void
    {
        if (name is MemberName) {
            callUserCode("playerLeft_v1", MemberName(name).getMemberId());
        }
    }

    protected function getMySprite () :MemberSprite
    {
        if (isPlaying()) {
            var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
            if (view != null) {
                return view.getMyAvatar();
            }
        }
        return null;
    }

    protected function getMobSprite (mobId :String) :MobSprite
    {
        var view :RoomObjectView = _wctx.getTopPanel().getPlaceView() as RoomObjectView;
        return (view == null) ? null : view.getMob(_ctrl.getGameId(), mobId);
    }

    protected function getAvatarSprite (playerId :int) :MemberSprite
    {
        if (!isPlaying()) {
            log.debug("getAvatarSprite(" + playerId + ") while !isPlaying()");
            return null;
        }
        var view :RoomObjectView = _wctx.getTopPanel().getPlaceView() as RoomObjectView;
        if (view == null) {
            log.debug("getAvatarSprite(" + playerId + ") without RoomView");
            return null;
        }
        var occInfo :OccupantInfo = _gameObj.getOccupantInfo(new MemberName("", playerId));
        if (occInfo != null) {
            var sprite :OccupantSprite = view.getOccupant(occInfo.bodyOid);
            if (sprite != null) {
                return sprite as MemberSprite;
            }
        }
        log.debug("getAvatarSprite(" + playerId + ") return null");
        return null;
    }

    protected function loggingConfirmListener (svc :String, processed :Function = null)
        :InvocationService_ConfirmListener
    {
        return new ConfirmAdapter(function (cause :String) :void {
            log.warning("Service failure [service=" + svc + ", cause=" + cause + "].");
        }, processed);
    }

    protected function loggingInvocationListener (svc :String) :InvocationService_InvocationListener
    {
        return new InvocationAdapter(function (cause :String) :void {
            log.warning("Service failure [service=" + svc + ", cause=" + cause + "].");
        });
    }

    protected var _wctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;

    protected var _stateBackend :StateControlBackend;
    protected var _questBackend :QuestControlBackend;

    protected var _gameObj :AVRGameObject;
    protected var _playerObj :PlayerObject;
    protected var _roomObj :RoomObject;

    protected var _playerListener :MessageAdapter = new MessageAdapter(
        function (event :MessageEvent) :void {
            var name :String = event.getName();
            if (name == AVRGameObject.COINS_AWARDED_MESSAGE) {
                callUserCode("coinsAwarded_v1", int(event.getArgs()[0]));
            }
    });

    protected var _gameListener :SetAdapter = new SetAdapter(
        function (event :EntryAddedEvent) :void {
            if (event.getName() == PlaceObject.OCCUPANT_INFO && _roomObj != null) {
                var gameInfo :OccupantInfo = event.getEntry();

                // we look this user up by display name in the room
                if (_roomObj.getOccupantInfo(gameInfo.username) != null) {
                    // an occupant of our current room began playing this AVRG
                    playerEntered(gameInfo.username);
                }
            }
        },
        null,
        function (event :EntryRemovedEvent) :void {
            if (event.getName() == PlaceObject.OCCUPANT_INFO && _roomObj != null) {
                var gameInfo :OccupantInfo = event.getOldEntry();

                // we look this user up by display name in the room
                if (_roomObj.getOccupantInfo(gameInfo.username)) {
                    // an occupant of our current room stopped playing this AVRG
                    playerLeft(gameInfo.username);
                }
            }
        });

    protected var _locationObserver :LocationObserver = new LocationAdapter(
        null, function (place :PlaceObject) :void {
            if (_roomObj != null) {
                _roomObj.removeListener(_movementListener);
                callUserCode("leftRoom_v1");
            }
            _roomObj = (place as RoomObject);
            if (_roomObj != null) {
                _roomObj.addListener(_movementListener);
                callUserCode("enteredRoom_v1", _wctx.getSceneDirector().getScene().getId());
            }
    }, null);

    protected var _movementListener :SetAdapter = new SetAdapter(null,
        function (event :EntryUpdatedEvent) :void {
            if (event.getName() == SpotSceneObject.OCCUPANT_LOCS) {
                var oid :int = event.getEntry().getKey();
                if (_roomObj != null) {
                    // find the occupant info for this body
                    var occInfo :OccupantInfo = _roomObj.occupantInfo.get(oid);
                    if (occInfo) {
                        // and its name
                        var name :MemberName = occInfo.username as MemberName;
                        // and make sure it's a player
                        if (name != null && _gameObj.getOccupantInfo(name)) {
                            callUserCode("playerMoved_v1", name.getMemberId());
                        }
                    }
                }
            }
        });

    protected var _occupantObserver :OccupantObserver = new OccupantAdapter(
        function (info :OccupantInfo) :void {
            if (_roomObj != null && _gameObj.getOccupantInfo(info.username) != null) {
                playerEntered(info.username);
            }
        },
        function (info :OccupantInfo) :void {
            if (_roomObj != null && _gameObj.getOccupantInfo(info.username) != null) {
                playerLeft(info.username);
            }
        });
}
}
