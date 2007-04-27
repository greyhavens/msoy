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

    /** A command to rejoin the lobby - executed by FloatingTableDisplay. */
    public static const JOIN_LOBBY :String = "JoinLobby";

    public function LobbyController (mctx :WorldContext, oid :int) 
    {
        _mctx = mctx;

        _subscriber = new SafeSubscriber(oid, this)
        _subscriber.subscribe(_mctx.getDObjectManager());

        _panel = new LobbyPanel(_mctx, this);
        _panel.addEventListener(Event.REMOVED_FROM_STAGE, handleRemovedFromStage);
        setControlledPanel(_panel);
        _panelIsVisible = true;
        _mctx.getTopPanel().setLeftPanel(_panel);
    }

    // from Subscriber
    public function objectAvailable (obj :DObject) :void 
    {
        _lobj = obj as LobbyObject;
        _panel.init(_lobj);

        _tableDir = new TableDirector(_mctx, LobbyObject.TABLES);
        _tableDir.setTableObject(obj);
        _tableDir.addTableObserver(_panel);
        _tableDir.addSeatednessObserver(_panel);
    }

    /**
     * Event handler for Event.REMOVED_FROM_STAGE
     */
    public function handleRemovedFromStage (evt :Event) :void
    {
        _panelIsVisible = false;
        var seatedTable :Table = _tableDir.getSeatedTable();
        if ((seatedTable != null) && !seatedTable.inPlay()) {
            var tableDisplay :FloatingTableDisplay = new FloatingTableDisplay(_mctx, _panel, 
                _tableDir, _panel.getGame().name);
            tableDisplay.open();
            setControlledPanel(tableDisplay.getRenderer());
            _mctx.getTopPanel().setTableDisplay(tableDisplay);
            _mctx.getMsoyController().gameLobbyCleared(_lobj.game.itemId, _playerInitiatedClose);

        } else {
            shutdown();
        }
    }

    // from Subscriber
    public function requestFailed (oid :int, cause :ObjectAccessError) :void 
    {
        Log.getLog(this).warning("request for the LobbyObject failed: ", cause);
    }

    /**
     * Handles SUBMIT_TABLE.
     */
    public function handleSubmitTable (args :Array) :void
    {
        _tableDir.createTable(args[0] as TableConfig, args[1] as GameConfig);
    }

    /**
     * Handles JOIN_TABLE.
     */
    public function handleJoinTable (args :Array) :void
    {
        _tableDir.joinTable(args[0] as int /*tableId*/, args[1] as int /*pos*/);
    }

    /**
     * Handles LEAVE_TABLE.
     */
    public function handleLeaveTable (tableId :int) :void
    {
        _tableDir.leaveTable(tableId);
        if (!_panelIsVisible) {
            shutdown();
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
        _playerInitiatedClose = true;
        _mctx.getTopPanel().clearLeftPanel(_panel);
    }

    /**
     * Handles JOIN_LOBBY.
     */
    public function handleJoinLobby () :void
    {
        _panelIsVisible = true;
        setControlledPanel(_panel);
        _mctx.getTopPanel().clearTableDisplay();
        _mctx.getTopPanel().setLeftPanel(_panel);
        _mctx.getMsoyController().gameLobbyShown(_lobj.game.itemId);
    }

    /**
     * TEMP: Forwards on HeaderBarController.SHOW_EMBED_HTML
     */
    public function handleShowEmbedHtml () :void
    {
        CommandEvent.dispatch(_mctx.getTopPanel().getHeaderBar(), 
            HeaderBarController.SHOW_EMBED_HTML);
    }

    /**
     * Clean up our references, and notify those that care that we're all done here.
     */
    protected function shutdown () :void
    {
        _panel.removeEventListener(Event.REMOVED_FROM_STAGE, handleRemovedFromStage);
        _subscriber.unsubscribe(_mctx.getDObjectManager());
        var currentDisp :FloatingTableDisplay = _mctx.getTopPanel().getTableDisplay();
        if (_lobj != null && currentDisp != null && currentDisp.getGameId() == _lobj.game.itemId) {
            // only clear the display if its a display for this lobby
            _mctx.getTopPanel().clearTableDisplay();
        }
        _mctx.getMsoyController().gameLobbyCleared(_lobj.game.itemId, _playerInitiatedClose);
        _tableDir.clearTableObject();
        _tableDir.removeTableObserver(_panel);
        _tableDir.removeSeatednessObserver(_panel);
    }

    /** The provider of free cheese. */
    protected var _mctx :WorldContext;

    /** The panel we're controlling. */
    protected var _panel :LobbyPanel;

    /** The table director. */
    protected var _tableDir :TableDirector;

    /** Our distributed LobbyObject */
    protected var _lobj :LobbyObject;

    protected var _subscriber :SafeSubscriber;

    protected var _panelIsVisible :Boolean;
    protected var _playerInitiatedClose :Boolean = false;
}
}
