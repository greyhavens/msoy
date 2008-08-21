//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.SetAdapter;
import com.threerings.presents.util.SafeObjectManager;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.MsoyClient;

import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.RoomPropertiesEntry;
import com.threerings.msoy.room.data.RoomPropertiesObject;
import com.threerings.msoy.game.client.GameContext;

import com.threerings.msoy.avrg.data.AVRGameConfig;
import com.threerings.msoy.avrg.data.AVRGameObject;

public class AVRGameController extends PlaceController
{
    public static const log :Log = Log.getLog(AVRGameController);

    // from PlaceController
    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);

        _gctx = (ctx as GameContext);
    }

    public function initializeWorldContext (wctx :WorldContext) :void
    {
        _wctx = wctx;

        _wctx.getClient().addEventListener(
            MsoyClient.MINI_WILL_CHANGE, function (ev :ValueEvent) :void {
                miniWillChange(ev.value);
        });

        // willEnterPlace() may have already run
        maybeStartup();
    }

    // from PlaceController
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new AVRGamePanel(this);
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

        _wctx.getClient().removeEventListener(MsoyClient.MINI_WILL_CHANGE, miniWillChange);

        gameAvailable(0);

        _backend.shutdown();

        _gameObj = null;

        _wctx.getLocationDirector().removeLocationObserver(_roomObserver);

        _worldObjectMgr.unsubscribeAll();
        _worldObjectMgr = null;

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
        // if we played a lobbied game recently, tell the tutorial code
        if (_wctx.getGameDirector().popMostRecentLobbyGame() != 0) {
            tutorialEvent("gamePlayed");
        }

        gameAvailable(getGameId());
    }

    public function tutorialEvent (eventName :String) :void
    {
        // TODO: are we ready to drop all this tutorial scaffolding?
//        if (_panel) {
//            _panel.tutorialEvent(eventName);
//        }
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

    /**
     * Retrieve the room properties for our current rooom. Returns null if they are not yet 
     * available.
     */
    public function getRoomProperties () :RoomPropertiesObject
    {
        if (_roomPropsOid == 0) {
            return null;
        }
        return _worldObjectMgr.getObj(_roomPropsOid) as RoomPropertiesObject;
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

        var panel :AVRGamePanel = (getPlaceView() as AVRGamePanel);
        panel.backendIsReady();

        _worldObjectMgr = new SafeObjectManager(
            _wctx.getClient().getDObjectManager(), log, roomPropsAvailable);

        // sign up for room objects
        _wctx.getLocationDirector().addLocationObserver(_roomObserver);

        updateRoom(_wctx.getLocationDirector().getPlaceObject() as RoomObject);
    }

    override protected function setPlaceView () :void
    {
        _wctx.getWorldController().setAVRGamePanel(getPlaceView() as AVRGamePanel);
    }

    protected function miniWillChange (mini :Boolean) :void
    {
        if (_wctx.getGameDirector().isPlayingTutorial()) {
            tutorialEvent(mini ? "willMinimize" : "willUnminimize");
        }
    }

    protected function gameAvailable (gameId :int) :void
    {
        var view :RoomObjectView = _wctx.getTopPanel().getPlaceView() as RoomObjectView;
        if (view != null) {
            view.avrGameAvailable(gameId);
        }
    }

    /**
     * Called when the user's room changes.
     */
    protected function updateRoom (roomObject :RoomObject) :void
    {
        // establish a subscription to the properties of the room
        var gameId :int = getGameId();

        var entry :RoomPropertiesEntry = null;
        if (roomObject != null) {
            entry = roomObject.propertySpaces.get(gameId) as RoomPropertiesEntry;
        }

        if (entry != null) {
            setRoomPropsOid(entry.propsOid);

        } else if (roomObject != null) {
            setRoomPropsOid(0);
            var setListener :SetAdapter;
            setListener = new SetAdapter(function (event :EntryAddedEvent) :void {
                entry = event.getEntry() as RoomPropertiesEntry;
                if (entry.ownerId == gameId) {
                    setRoomPropsOid(entry.propsOid);
                    roomObject.removeListener(setListener);
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
            _backend.setRoomProperties(null);
            _worldObjectMgr.unsubscribe(_roomPropsOid);
        }

        _roomPropsOid = propsOid;

        if (_roomPropsOid != 0) {
            _worldObjectMgr.subscribe(_roomPropsOid);
        }
    }

    protected function roomPropsAvailable (roomProps :RoomPropertiesObject) :void
    {
        if (roomProps.getOid() == _roomPropsOid) {
            _backend.setRoomProperties(roomProps);
        }
    }

    protected var _wctx :WorldContext;
    protected var _gctx :GameContext;

    protected var _gameObj :AVRGameObject;
    protected var _backend :AVRGameBackend;

    protected var _worldObjectMgr :SafeObjectManager;
    protected var _roomPropsOid :int;
    protected var _roomObserver :LocationAdapter = new LocationAdapter(null, updateRoom);
}
}
