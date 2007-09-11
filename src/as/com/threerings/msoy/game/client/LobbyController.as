//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;

import com.threerings.parlor.client.TableDirector;

import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.data.Table;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.HeaderBarController;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.LobbyObject;

import com.threerings.util.Controller;
import com.threerings.util.CommandEvent;

public class LobbyController extends Controller implements Subscriber
{
    /** A command to submit a configured table configuration for creation. */
    public static const SUBMIT_TABLE :String = "SubmitTable";

    /** A command to start a table early (when everyone's not yet sitting) */
    public static const START_TABLE :String = "StartTable";

    /** A command to sit in a place in a table. */
    public static const JOIN_TABLE :String = "JoinTable";

    /** A command to leave a table. */
    public static const LEAVE_TABLE :String = "LeaveTable";

    /** A command to leave the lobby. */
    public static const LEAVE_LOBBY :String = "LeaveLobby";

    public function LobbyController (
        mctx :WorldContext, gctx :GameContext, liaison :GameLiaison, oid :int) 
    {
        _mctx = mctx;
        _gctx = gctx;
        _liaison = liaison;

        _subscriber = new SafeSubscriber(oid, this)
        _subscriber.subscribe(_gctx.getDObjectManager());

        _panel = new LobbyPanel(_mctx, _gctx, this);
        _panel.addEventListener(Event.REMOVED_FROM_STAGE, handleRemovedFromStage);
        _panel.addEventListener(Event.ADDED_TO_STAGE, handleAddedToStage);
        setControlledPanel(_panel);
        _panelIsVisible = true;
        _mctx.getTopPanel().setLeftPanel(_panel);
    }

    /**
     * Returns the id of the game managed by this lobby controller. Not valid until we've
     * subscribed to our lobby object.
     */
    public function get gameId () :int
    {
        return _lobj.game.itemId;
    }

    /**
     * This is called if something external wants us to leave any table we're seated at and 
     * shutdown.
     */
    public function forceShutdown () :void
    {
        if (_tableDir != null) {
            var currentTable :Table = _tableDir.getSeatedTable();
            if (currentTable != null) {
                _tableDir.leaveTable(currentTable.tableId);
                _panel.seatednessDidChange(false);
            }
        }

        shutdown(false);
    }

    /**
     * Join the given player - either sit them at the player's lobbying table, join their party
     * game, watch their seated game, or put up an error message.
     */
    public function joinPlayer (memberId :int) :void
    {
        Log.getLog(this).debug("REQUESTED TO JOIN PLAYER [" + memberId + "]");
    }

    /**
     * Handles SUBMIT_TABLE.
     */
    public function handleSubmitTable (tcfg :TableConfig, gcfg :GameConfig) :void
    {
        _tableDir.createTable(tcfg, gcfg);
        _mctx.getGameDirector().setMatchingGame(_lobj.game);
    }

    /**
     * Handles JOIN_TABLE.
     */
    public function handleJoinTable (tableId :int, position :int) :void
    {
        _tableDir.joinTable(tableId, position);
        _mctx.getGameDirector().setMatchingGame(_lobj.game);
    }

    /**
     * Handles LEAVE_TABLE.
     */
    public function handleLeaveTable (tableId :int) :void
    {
        _tableDir.leaveTable(tableId);

        if (!_panelIsVisible) {
            shutdown(false);
            // in case shutdown happens before the leave table event is propagated, we need to make
            // sure the panel hears about it
            _panel.seatednessDidChange(false);
        } 
    }

    /**
     * Handles START_TABLE.
     */
    public function handleStartTable (tableId :int) :void
    {
        _tableDir.startTableNow(tableId);
    }

    /**
     * Handles LEAVE_LOBBY.
     */
    public function handleLeaveLobby () :void
    {
        _mctx.getTopPanel().clearLeftPanel(_panel);
    }

    /**
     * Restores the lobby UI.
     */
    public function restoreLobbyUI () :void
    {
        if (!_panelIsVisible) {
            _panelIsVisible = true;
            setControlledPanel(_panel);
            _mctx.getTopPanel().clearTableDisplay();
            _mctx.getTopPanel().setLeftPanel(_panel);
        }
    }

    /**
     * Event handler for Event.ADDED_TO_STAGE
     */
    public function handleAddedToStage (evt :Event) :void
    {
        if (_lobj != null) {
            _mctx.getWorldClient().setWindowTitle(_lobj.game.name);
        }
    }

    /**
     * Event handler for Event.REMOVED_FROM_STAGE
     */
    public function handleRemovedFromStage (evt :Event) :void
    {
        _panelIsVisible = false;

        var seatedTable :Table = _tableDir != null ? _tableDir.getSeatedTable() : null;
        if ((seatedTable != null) && !seatedTable.inPlay()) {
            var tableDisplay :FloatingTableDisplay = new FloatingTableDisplay(
                _mctx, _gctx, _panel, _tableDir, _panel.getGame().name);
            tableDisplay.open();
            setControlledPanel(tableDisplay.getRenderer());
            _mctx.getTopPanel().setTableDisplay(tableDisplay);

        } else {
            // if we're in a table, then it must have started
            shutdown(seatedTable != null);
        }
    }

    // from Subscriber
    public function objectAvailable (obj :DObject) :void 
    {
        _lobj = obj as LobbyObject;
        _panel.init(_lobj);

        _tableDir = new TableDirector(_gctx, LobbyObject.TABLES);
        _tableDir.setTableObject(obj);
        _tableDir.addTableObserver(_panel);
        _tableDir.addSeatednessObserver(_panel);

        _mctx.getWorldClient().setWindowTitle(_lobj.game.name);
    }

    // from Subscriber
    public function requestFailed (oid :int, cause :ObjectAccessError) :void 
    {
        Log.getLog(this).warning("Request for the LobbyObject failed: " + cause);
    }

    /**
     * Clean up our references, and notify those that care that we're all done here.
     */
    protected function shutdown (inGame :Boolean) :void
    {
        _panel.removeEventListener(Event.REMOVED_FROM_STAGE, handleRemovedFromStage);
        _subscriber.unsubscribe(_mctx.getDObjectManager());
        var currentDisp :FloatingTableDisplay = _mctx.getTopPanel().getTableDisplay();
        if (_lobj != null && currentDisp != null && currentDisp.getGameId() == _lobj.game.itemId) {
            // only clear the display if its a display for this lobby
            _mctx.getTopPanel().clearTableDisplay();
        }
        if (_tableDir != null) {
            _tableDir.clearTableObject();
            _tableDir.removeTableObserver(_panel);
            _tableDir.removeSeatednessObserver(_panel);
        }

        // let the game liaison know that we're gone
        _liaison.lobbyCleared(inGame);
    }

    /** The provider of free cheese. */
    protected var _mctx :WorldContext;

    /** The provider of game related services. */
    protected var _gctx :GameContext;

    /** Handles our connection to the game server. */
    protected var _liaison :GameLiaison;

    /** Our distributed LobbyObject */
    protected var _lobj :LobbyObject;

    /** The panel we're controlling. */
    protected var _panel :LobbyPanel;

    /** The table director. */
    protected var _tableDir :TableDirector;

    /** Used to subscribe to our lobby object. */
    protected var _subscriber :SafeSubscriber;

    protected var _panelIsVisible :Boolean;
}
}
