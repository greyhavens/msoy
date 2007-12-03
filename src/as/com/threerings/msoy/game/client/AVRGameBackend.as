//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.LocationObserver;
import com.threerings.crowd.client.OccupantAdapter;
import com.threerings.crowd.client.OccupantObserver;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;

import com.threerings.presents.dobj.*;

import com.threerings.util.Iterator;
import com.threerings.util.Log;
import com.threerings.util.Name;

import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.ControlBackend;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.data.GameState;
import com.threerings.msoy.game.data.QuestState;
import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.world.client.AbstractRoomView;
import com.threerings.msoy.world.client.RoomMetrics;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.data.RoomObject;

public class AVRGameBackend extends ControlBackend
{
    public static const log :Log = Log.getLog(AVRGameBackend);

    public function AVRGameBackend (
        gctx :GameContext, ctrl :AVRGameController, gameObj :AVRGameObject)
    {
        _gctx = gctx;
        _mctx = gctx.getWorldContext();
        _gameObj = gameObj;
        _ctrl = ctrl;

        _playerObj = _gctx.getPlayerObject();

        _gameObj.addListener(_gameListener);

        _mctx.getLocationDirector().addLocationObserver(_locationObserver);
        _mctx.getOccupantDirector().addOccupantObserver(_occupantObserver);

        // will be null if not a room
        _roomObj = (_mctx.getLocationDirector().getPlaceObject() as RoomObject);
        if (_roomObj != null) {
            _roomObj.addListener(_movementListener);
        }

        _stateBackend = new StateControlBackend(gctx, this, gameObj);
        _questBackend = new QuestControlBackend(gctx, this, gameObj);
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

         _mctx.getLocationDirector().removeLocationObserver(_locationObserver);
         _mctx.getOccupantDirector().removeOccupantObserver(_occupantObserver);
        
         _gameObj.removeListener(_gameListener);

         super.shutdown();
    }

    public function get room () :RoomObject
    {
        return _roomObj;
    }

    public function panelResized () :void
    {
        callUserCode("panelResized_v1");
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
        return _mctx.getGameDirector().getGameId() == _ctrl.getGameId();
    }

    // from ControlBackend
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        // AVRGameControl
        o["getStageBounds_v1"] = getStageBounds_v1;
        o["getRoomBounds_v1"] = getRoomBounds_v1;
        o["getViewBounds_v1"] = getViewBounds_v1;
        o["deactivateGame_v1"] = deactivateGame_v1;
        o["isPlayerHere_v1"] = isPlayerHere_v1;
        o["getPlayerIds_v1"] = getPlayerIds_v1;
        o["spawnMob_v1"] = spawnMob_v1;

        _stateBackend.populateSubProperties(o);
        _questBackend.populateSubProperties(o);
    }

    protected function getStageBounds_v1 () :Rectangle
    {
        return _mctx.getTopPanel().getPlaceViewBounds();
    }

    protected function getRoomBounds_v1 () :Rectangle
    {
        var view :AbstractRoomView = _mctx.getTopPanel().getPlaceView() as AbstractRoomView;
        if (view != null) {
            var metrics :RoomMetrics = view.layout.metrics;
            return new Rectangle(0, 0, metrics.sceneWidth, metrics.sceneHeight);
        }
        return null;
    }

    protected function getViewBounds_v1 () :Rectangle
    {
        var view :AbstractRoomView = _mctx.getTopPanel().getPlaceView() as AbstractRoomView;
        if (view != null) {
            var rect :Rectangle = view.getScrollBounds();
            var offset :int = view.getScrollOffset();
            rect.left += offset;
            rect.right += offset;
            return rect;
        }
        return null;
    }

    protected function deactivateGame_v1 () :Boolean
    {
        if (!isPlaying()) {
            return false;
        }
        _mctx.getGameDirector().leaveAVRGame();
        return true;
    }

    protected function getPlayerId_v1 () :int
    {
        return _mctx.getMemberObject().getMemberId();
    }

    protected function isPlayerHere_v1 (id :int) :Boolean
    {
        if (isPlaying() && _roomObj != null) {
            var fakeName :MemberName = new MemberName(null, id);
            return (_gameObj.getOccupantInfo(fakeName) != null &&
                    _roomObj.getOccupantInfo(fakeName) != null);
        }
        return false;
    }

    protected function getPlayerIds_v1 () :Array
    {
        if (!isPlaying() || _roomObj == null) {
            return null;
        }

        var result :Array = new Array();
        var iterator :Iterator = _gameObj.players.iterator();
        while (iterator.hasNext()) {
            var name :MemberName = OccupantInfo(iterator.next()).username as MemberName;
            if (name != null) {
                result.push(name.getMemberId());
            }
        }
        return result;
    }

    protected function spawnMob_v1 (mobId :String) :Boolean
    {
        if (isPlaying()) {
            var view :RoomView = _mctx.getTopPanel().getPlaceView() as RoomView;
            if (view != null) {
                view.getRoomObject().roomService.spawnMob(
                    _mctx.getClient(), _ctrl.getGameId(), mobId,
                    loggingInvocationListener("spawnMob"));
                return true;
            }
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

    protected var _mctx :WorldContext;
    protected var _gctx :GameContext;
    protected var _ctrl :AVRGameController;

    protected var _stateBackend :StateControlBackend;
    protected var _questBackend :QuestControlBackend;

    protected var _gameObj :AVRGameObject;
    protected var _playerObj :PlayerObject;
    protected var _roomObj :RoomObject;

    protected var _gameListener :SetAdapter = new SetAdapter(
        function (event :EntryAddedEvent) :void {
            if (event.getName() == AVRGameObject.PLAYERS && _roomObj != null) {
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
            if (event.getName() == AVRGameObject.PLAYERS && _roomObj != null) {
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
                callUserCode("leftRoom_v1", _mctx.getSceneDirector().getScene().getId());
            }
            _roomObj = (place as RoomObject);
            if (_roomObj != null) {
                _roomObj.addListener(_movementListener);
                callUserCode("enteredRoom_v1", _mctx.getSceneDirector().getScene().getId());
            }
    }, null);

    protected var _movementListener :SetAdapter = new SetAdapter(null,
        function (event :EntryUpdatedEvent) :void {
            if (event.getName() == SpotSceneObject.OCCUPANT_LOCS) {
                var name :MemberName = OccupantInfo(event.getEntry()).username as MemberName;
                if (_roomObj != null && name != null && _gameObj.getOccupantInfo(name)) {
                    callUserCode("playerMoved_v1", name.getMemberId());
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
