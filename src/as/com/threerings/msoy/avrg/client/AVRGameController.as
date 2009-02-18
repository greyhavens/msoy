//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.MessageBundle;
import com.threerings.util.Log;
import com.threerings.util.Name;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.util.SafeObjectManager;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.OccupantAdapter;
import com.threerings.crowd.client.OccupantObserver;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.whirled.game.data.WhirledGameCodes;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.room.data.RoomCodes;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesEntry;
import com.threerings.msoy.room.data.RoomPropertiesObject;

import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.PlayerObject;

import com.threerings.msoy.avrg.data.AVRGameConfig;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.PlayerLocation;

import flash.utils.ByteArray;

/**
 * Coordinates the client side of AVRG business.
 */
public class AVRGameController extends PlaceController
{
    public const log :Log = Log.getLog(this);

    // from PlaceController
    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);

        _gctx = (ctx as GameContext);
    }

    public function initializeWorldContext (wctx :WorldContext) :void
    {
        _wctx = wctx;

        // willEnterPlace() may have already run
        maybeStartup();
    }

    // from PlaceController
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new AVRGamePanel(ctx as GameContext, this);
    }

    // from PlaceController
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _gameObj = (plobj as AVRGameObject);

        // initializeWorldContext() may have already run
        maybeStartup();
    }

    // from PlaceController
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _wctx.getWorldController().setAVRGamePanel(null);

        gameAvailable(0);

        maybeDispatchLeftRoom("shutdown");

        _wctx.getOccupantDirector().removeOccupantObserver(_occupantObserver);

        _gctx.getClient().getClientObject().removeListener(_playerListener);
        _gameObj.removeListener(_gameListener);

        _backend.shutdown();

        _gameObj = null;

        _wctx.getLocationDirector().removeLocationObserver(_roomObserver);

        _roomPropsSubs.unsubscribeAll();
        _roomPropsSubs = null;

        super.didLeavePlace(plobj);
    }

    // from interface Subscriber
    public function requestFailed (oid :int, cause :ObjectAccessError) :void
    {
        log.warning("Failed to subscribe to world game object [oid=" + oid +
                    ", cause=" + cause + "].");
        _gameObj = null;
    }

    // called by the Panel when it's finished loading the AVRG media
    public function gameIsReady () :void
    {
        gameAvailable(getGameId());
    }

    public function get backend () :AVRGameBackend
    {
        return _backend;
    }

    public function getAVRGameConfig () :AVRGameConfig
    {
        return (_config as AVRGameConfig);
    }

    public function getAVRGameObject () :AVRGameObject
    {
        return (_plobj as AVRGameObject);
    }

    public function getGameId () :int
    {
        return getAVRGameConfig().getGameId();
    }

    public function getRoom () :RoomObject
    {
        return _roomObj;
    }

    /**
     * Retrieve the room properties for our current rooom. Returns null if they are not yet
     * available.
     */
    public function getRoomProperties () :RoomPropertiesObject
    {
        if (_roomPropsOid == 0) {
            return null;
        }
        var placeObj :PlaceObject = _wctx.getLocationDirector().getPlaceObject();
        if (placeObj == null || placeObj.getOid() != _roomOid) {
            return null;
        }
        return _roomPropsSubs.getObj(_roomPropsOid) as RoomPropertiesObject;
    }

    public function backendConnected () :void
    {
        maybeDispatchEnteredRoom("backend connected");
    }

    public function setIdle (nowIdle :Boolean) :void
    {
        if (_gameObj != null) {
            _gameObj.avrgService.setIdle(_gctx.getClient(), nowIdle,
                BackendUtils.loggingConfirmListener("setIdle"));
        }
    }

    // both initializeWorldContext() and willEnterPlace() contribute data that is vital to
    // the creation of the backend, and we cannot know which order they will execute in, so
    // we call this method in both; it will be executed when both have done their job.
    protected function maybeStartup () :void
    {
        // if we're not fully configured yet, wait for next call
        if (_gameObj == null || _wctx == null) {
            return;
        }

        // else create the backend!
        _backend = new AVRGameBackend(_wctx, _gctx, this, _gameObj);

        _playerObj = _gctx.getPlayerObject();
        _gameObj.addListener(_gameListener);

        _gctx.getClient().getClientObject().addListener(_playerListener);

        _wctx.getOccupantDirector().addOccupantObserver(_occupantObserver);

        // will be null if not a room
        _roomObj = (_wctx.getLocationDirector().getPlaceObject() as RoomObject);

        // make sure we're listening for events
        enteredRoom(_roomObj);

        var panel :AVRGamePanel = (getPlaceView() as AVRGamePanel);
        panel.backendIsReady();

        // sign up for room objects
        _wctx.getLocationDirector().addLocationObserver(_roomObserver);

        updateRoom(_wctx.getLocationDirector().getPlaceObject() as RoomObject);
    }

    override protected function setPlaceView () :void
    {
        _wctx.getWorldController().setAVRGamePanel(getPlaceView() as AVRGamePanel);
    }

    protected function gameAvailable (gameId :int) :void
    {
        var view :RoomObjectView = _wctx.getPlaceView() as RoomObjectView;
        if (view != null) {
            view.avrGameAvailable(gameId);
        }
    }

    /**
     * Called when the user's room changes.
     */
    protected function updateRoom (roomObject :RoomObject) :void
    {
        if (roomObject == _roomObj) {
            log.warning("Room changing to same room?");
        }

        maybeDispatchLeftRoom("room change");

        leftRoom(_roomObj);

        _roomObj = roomObject;

        // establish a subscription to the properties of the room
        var gameId :int = getGameId();

        var entry :RoomPropertiesEntry = null;
        if (roomObject != null) {
            entry = roomObject.propertySpaces.get(gameId) as RoomPropertiesEntry;
            _roomOid = roomObject.getOid();
        } else {
            _roomOid = 0;
        }

        if (entry != null) {
            setRoomPropsOid(entry.propsOid);

        } else if (roomObject != null) {
            setRoomPropsOid(0);

            // listen for the property space becoming available
            var setListener :SetAdapter;
            setListener = new SetAdapter(function (event :EntryAddedEvent) :void {
                if (event.getName() == RoomObject.PROPERTY_SPACES) {
                    entry = event.getEntry() as RoomPropertiesEntry;
                    if (entry.ownerId == gameId) {
                        setRoomPropsOid(entry.propsOid);
                        // unregister this listener once we have the property space oid
                        roomObject.removeListener(setListener);
                    }
                }
            });
            roomObject.addListener(setListener);

        } else {
            setRoomPropsOid(0);
        }
    }

    /**
     * Subscribe to properties and relinquish the old subscription if any.
     */
    protected function setRoomPropsOid (propsOid :int) :void
    {
        if (_roomPropsOid != 0) {
            _roomPropsSubs.unsubscribe(_roomPropsOid);
            _roomPropsSubs = null;
        }

        _roomPropsOid = propsOid;

        if (_roomPropsOid != 0) {
            _roomPropsSubs = new SafeObjectManager(
                _wctx.getClient().getDObjectManager(), log, roomPropsAvailable, roomPropsFailure);
            _roomPropsSubs.subscribe(_roomPropsOid);
        }
    }

    protected function roomPropsAvailable (roomProps :RoomPropertiesObject) :void
    {
        maybeDispatchEnteredRoom("props arrived");
    }

    protected function roomPropsFailure (oid :int, cause :Error) :*
    {
        log.warning("Failed to subscribe to room properties", "oid", oid, cause);
    }

    // internal callback
    protected function roomOccupantAdded (name :Name, why :String) :void
    {
        if (name is MemberName) {
            var memberId :int = MemberName(name).getMemberId();
            if (memberId != _playerObj.getMemberId()) {
                log.debug(playerIdStr(), "Player entered [name=" + name + ", why=" + why + "]");
                _backend.roomOccupantAdded(memberId);
            }
        }
    }

    // internal callback
    protected function roomOccupantRemoved (name :Name, why :String) :void
    {
        if (name is MemberName) {
            var memberId :int = MemberName(name).getMemberId();
            if (memberId != _playerObj.getMemberId()) {
                log.debug(playerIdStr(), "Player left [name=" + name + ", why=" + why + "]");
                _backend.roomOccupantRemoved(memberId);
            }
        }
    }

    protected function maybeDispatchLeftRoom (why :String) :void
    {
        if (_backend == null) {
            return;
        }

        // Dispatch to user when first condition is met
        if (_lastDispatchedSceneId != 0) {
            log.debug(playerIdStr(), "Left room [sceneId=" + _lastDispatchedSceneId +
                ", why=" + why + "]");
            _backend.playerLeftRoom(_lastDispatchedSceneId);
            _lastDispatchedSceneId = 0;
        }
    }

    protected function maybeDispatchEnteredRoom (why :String) :void
    {
        if (_wctx.getSceneDirector() == null || _wctx.getLocationDirector() == null ||
            _gameObj == null || _playerObj == null || _wctx.getSceneDirector().getScene() == null ||
            _backend == null) {
            log.info(
                "Bad null when entering room", "why", why,
                "sceneDir", _wctx.getSceneDirector() != null,
                "locDir", _wctx.getLocationDirector() != null,
                "gameObj", _gameObj != null, "playerObj", _playerObj != null,
                "scene", _wctx.getSceneDirector() != null &&
                _wctx.getSceneDirector().getScene() != null, "backend", _backend != null);
            maybeDispatchLeftRoom("entry error");
            return;
        }

        var sceneId :int = _wctx.getSceneDirector().getScene().getId();
        if (sceneId == _lastDispatchedSceneId) {
            log.debug(
                playerIdStr(), "Already entered room [sceneId=" + sceneId + ", why=" + why + "]");
            return;
        }

        // Dispatch to user when all conditions are met
        var ploc :PlayerLocation;
        ploc = _gameObj.playerLocs.get(_playerObj.getMemberId()) as PlayerLocation;
        var roomProps :RoomPropertiesObject = getRoomProperties();
        var placeObj :PlaceObject = _wctx.getLocationDirector().getPlaceObject();
        var placeId :int = placeObj == null ? 0 : placeObj.getOid();
        var roomOid :int = _roomObj == null ? 0 : _roomObj.getOid();
        var connected :Boolean = _backend.isConnected();

        if (roomProps != null && ploc != null && sceneId == ploc.sceneId && placeId != 0 &&
            placeId == roomOid && connected) {
            maybeDispatchLeftRoom("entered"); // no-op if left already
            log.debug(playerIdStr(), "Entered room  [sceneId=" + sceneId + ", why=" + why + "]");
            _backend.playerEnteredRoom(sceneId, roomProps);
            enteredRoom(_roomObj);
            _lastDispatchedSceneId = sceneId;

        } else {
            log.debug(playerIdStr(),
                "Would dispatch but... [roomProps=" + roomProps + ", ploc=" + ploc +
                ", sceneId=" + sceneId + ", placeId=" + placeId + ", roomOid=" + roomOid +
                ", connected=" + connected + ", why=" + why + "]");
        }
    }

    // called when we're in a room and fully AVRG-ready for it
    protected function enteredRoom (roomObj :RoomObject) :void
    {
        // but do persistently listen for messages on the room object
        roomObj.addListener(_roomMessageListener);
    }

    // called when we've left a room and should clean up
    protected function leftRoom (roomObj :RoomObject) :void
    {
        if (roomObj != null) {
            roomObj.removeListener(_roomMessageListener);
        }
    }

    protected function playerIdStr () :String
    {
        return "[" + (_playerObj == null ? "null" : _playerObj.getMemberId()) + "]";
    }

    protected function reportCoinsAwarded (amount :int, forReal :Boolean) :void
    {
        var msg :String = forReal ?
            MessageBundle.tcompose("m.coins_awarded", amount) : "m.no_coins_dev";
        _wctx.displayInfo(WhirledGameCodes.WHIRLEDGAME_MESSAGE_BUNDLE, msg);
    }

    protected function gameEntryAdded (event :EntryAddedEvent) :void
    {
        if (event.getName() == PlaceObject.OCCUPANT_INFO && _roomObj != null) {
            var gameInfo :OccupantInfo = event.getEntry() as OccupantInfo;

            // we look this user up by display name in the room
            if (_roomObj.getOccupantInfo(gameInfo.username) != null) {
                // an occupant of our current room began playing this AVRG
                roomOccupantAdded(gameInfo.username, "game occupancy change");
            }
        } else if (event.getName() == AVRGameObject.PLAYER_LOCS) {
            var ploc :PlayerLocation = event.getEntry() as PlayerLocation;
            if (ploc.playerId == _playerObj.getMemberId()) {
                maybeDispatchEnteredRoom("location update");
            }
        }
    }

    protected function gameEntryUpdated (event :EntryUpdatedEvent) :void
    {
        if (event.getName() == AVRGameObject.PLAYER_LOCS) {
            var ploc :PlayerLocation = event.getEntry() as PlayerLocation;
            if (ploc.playerId == _playerObj.getMemberId()) {
                maybeDispatchLeftRoom("location update");
                maybeDispatchEnteredRoom("location update");
            }
        }
    }

    protected function gameEntryRemoved (event :EntryRemovedEvent) :void
    {
        if (event.getName() == PlaceObject.OCCUPANT_INFO && _roomObj != null) {
            var gameInfo :OccupantInfo = event.getOldEntry() as OccupantInfo;

            // we look this user up by display name in the room
            if (_roomObj.getOccupantInfo(gameInfo.username)) {
                // an occupant of our current room stopped playing this AVRG
                roomOccupantRemoved(gameInfo.username, "game occupancy change");
            }
        } else if (event.getName() == AVRGameObject.PLAYER_LOCS) {
            var ploc :PlayerLocation = event.getOldEntry() as PlayerLocation;
            if (ploc.playerId == _playerObj.getMemberId()) {
                maybeDispatchLeftRoom("location update");
            }
        }
    }

    protected function occupantEntered (info :OccupantInfo) :void
    {
        if (_roomObj != null && _gameObj.getOccupantInfo(info.username) != null) {
            roomOccupantAdded(info.username, "room occupancy changed");
        }
    }

    protected function occupantLeft (info :OccupantInfo) :void
    {
        if (_roomObj != null && _gameObj.getOccupantInfo(info.username) != null) {
            roomOccupantRemoved(info.username, "room occupancy changed");
        }
    }

    protected function roomMessage (event :MessageEvent) :void {
        log.info("roomMessage", "event", event);
        if (event.getName() == RoomCodes.SPRITE_SIGNAL) {
            var args :Array = event.getArgs();
            _backend.signalReceived(args[0] as String, args[1] as ByteArray);
        }
    }

    protected function playerMessage (event :MessageEvent) :void
    {
        var name :String = event.getName();
        if (name == AVRGameObject.TASK_COMPLETED_MESSAGE) {
            var args :Array = event.getArgs();
            var task :String = String(args[0]);
            var amount :int = int(args[1]);
            if (!_backend.taskCompleted(task, amount)) {
                const forReal :Boolean = Boolean(args[2]);
                reportCoinsAwarded(amount, forReal);
            }
        }
    }

    protected var _wctx :WorldContext;
    protected var _gctx :GameContext;

    protected var _gameObj :AVRGameObject;
    protected var _playerObj :PlayerObject;
    protected var _backend :AVRGameBackend;

    protected var _roomObj :RoomObject;
    protected var _roomPropsSubs :SafeObjectManager;
    protected var _roomPropsOid :int;
    protected var _roomOid :int;
    protected var _roomObserver :LocationAdapter = new LocationAdapter(null, updateRoom);

    protected var _lastDispatchedSceneId :int;

    protected var _roomMessageListener :MessageAdapter =
        new MessageAdapter(roomMessage);

    protected var _playerListener :MessageAdapter =
        new MessageAdapter(playerMessage);

    protected var _gameListener :SetAdapter =
        new SetAdapter(gameEntryAdded, gameEntryUpdated, gameEntryRemoved);

    protected var _occupantObserver :OccupantObserver =
        new OccupantAdapter(occupantEntered, occupantLeft);
}
}
