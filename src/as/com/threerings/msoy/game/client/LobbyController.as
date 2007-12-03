//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;

import com.threerings.io.TypedArray;
import com.threerings.util.CommandEvent;
import com.threerings.util.Controller;
import com.threerings.util.Log;
import com.threerings.util.Name;

import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.client.HeaderBarController;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.client.MsoyGameService;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.MsoyTable;

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

    /** Notifies the lobby controller that we sat at a table. */
    public static const SAT_AT_TABLE :String = "SatAtTable";

    /** A command to close the lobby. */
    public static const CLOSE_LOBBY :String = "CloseLobby";

    public function LobbyController (
        ctx :GameContext, liaison :LobbyGameLiaison, lobbyOid :int, mode :int) 
    {
        _gctx = ctx;
        _mctx = ctx.getWorldContext();
        _liaison = liaison;
        _mode = mode;

        _subscriber = new SafeSubscriber(lobbyOid, this)
        _subscriber.subscribe(_gctx.getDObjectManager());

        _panel = new LobbyPanel(_gctx, this);
        _panel.addEventListener(Event.REMOVED_FROM_STAGE, handleRemovedFromStage);
        _panel.addEventListener(Event.ADDED_TO_STAGE, handleAddedToStage);
        setControlledPanel(_panel);
        _panelIsVisible = true;
        _mctx.getTopPanel().setLeftPanel(_panel);
    }

    /**
     * Handles SUBMIT_TABLE.
     */
    public function handleSubmitTable (
        tcfg :TableConfig, gcfg :GameConfig, friendIds :TypedArray) :void
    {
        _tableDir.createTable(tcfg, gcfg);
        _mctx.getGameDirector().setMatchingGame(_lobj.game);

        // if requested, send an invitation to our friends, inviting them to this game
        if (friendIds.length > 0) {
            var gsvc :MsoyGameService =
                (_mctx.getClient().requireService(MsoyGameService) as MsoyGameService);
            gsvc.inviteFriends(_mctx.getClient(), gcfg.getGameId(), friendIds);
        }
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
     * Handles SAT_AT_TABLE.
     */
    public function handleSatAtTable () :void
    {
        _mctx.getTopPanel().clearLeftPanel(_panel);
    }

    /**
     * Handles CLOSE_LOBBY.
     */
    public function handleCloseLobby () :void
    {
        _closedByUser = true;
        _mctx.getTopPanel().clearLeftPanel(_panel);
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
     * Joins the specified player at their pending game table. 
     */
    public function joinPlayerTable (playerId :int) :void
    {
        if (_lobj == null) {
            // this function will be called again when we have our lobby object
            _playerId = playerId;
            return;
        }

        for each (var table :Table in _lobj.tables.toArray()) {
            for each (var occupant :Name in table.occupants) {
                var member :MemberName = (occupant as MemberName);
                if (member == null || member.getMemberId() != playerId) {
                    continue;
                }

                if (table.inPlay()) {
                    _mctx.displayFeedback(MsoyCodes.GAME_MSGS, "e.game_in_progress");
                } else {
                    var ii :int = 0;
                    for (; ii < table.occupants.length; ii++) {
                        if (table.occupants[ii] == null) {
                            handleJoinTable(table.tableId, ii);
                            break;
                        }
                    }
                    if (ii == table.occupants.length) {
                        _mctx.displayFeedback(MsoyCodes.GAME_MSGS, "e.game_table_full");
                    }
                }
                return;
            }
        }
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
                _gctx, _panel, _tableDir, _panel.getGame().name);
            tableDisplay.open();
            setControlledPanel(tableDisplay.getPanel());
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
        _panel.init(_lobj, _mode == LobbyGameLiaison.PLAY_NOW_FRIENDS);

        _tableDir = new TableDirector(_gctx, LobbyObject.TABLES);
        _tableDir.setTableObject(obj);
        _tableDir.addTableObserver(_panel);
        _tableDir.addSeatednessObserver(_panel);

        _mctx.getWorldClient().setWindowTitle(_lobj.game.name);

        // if we have a player table to join, do that now, otherwise 
        if (_playerId != 0) {
            joinPlayerTable(_playerId);
            return;
        }

        // otherwise do something appropriate based on our mode
        switch (_mode) {
        case LobbyGameLiaison.PLAY_NOW_FRIENDS:
            joinSomeTable(true);
            break;
        case LobbyGameLiaison.PLAY_NOW_ANYONE:
            joinSomeTable(false);
            break;
        }
    }

    // from Subscriber
    public function requestFailed (oid :int, cause :ObjectAccessError) :void 
    {
        Log.getLog(this).warning("Request for the LobbyObject failed: " + cause);
    }

    /**
     * Looks for a table that we can join and joins it.
     */
    protected function joinSomeTable (friendsOnly :Boolean) :Boolean
    {
        for each (var table :Table in _lobj.tables.toArray()) {
            if (table.inPlay() ||
                (friendsOnly && (table as MsoyTable).countFriends(_mctx.getMemberObject()) == 0)) {
                continue;
            }
            for (var ii :int; ii < table.occupants.length; ii++) {
                if (table.occupants[ii] == null) {
                    handleJoinTable(table.tableId, ii);
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Clean up our references, and notify those that care that we're all done here.
     */
    protected function shutdown (inGame :Boolean) :void
    {
        // first do our UI cleanup
        _panel.removeEventListener(Event.REMOVED_FROM_STAGE, handleRemovedFromStage);
        var currentDisp :FloatingTableDisplay = _mctx.getTopPanel().getTableDisplay();
        if (_lobj != null && currentDisp != null && currentDisp.getGameId() == _lobj.game.itemId) {
            // only clear the display if its a display for this lobby
            _mctx.getTopPanel().clearTableDisplay();
        }

        // then our distributed services cleanup
        _subscriber.unsubscribe(_mctx.getDObjectManager());
        if (_tableDir != null) {
            _tableDir.clearTableObject();
            _tableDir.removeTableObserver(_panel);
            _tableDir.removeSeatednessObserver(_panel);
        }

        // finally let the game liaison know that we're gone
        _liaison.lobbyCleared(inGame, _closedByUser);
    }

    /** The provider of free cheese. */
    protected var _mctx :WorldContext;

    /** The provider of game related services. */
    protected var _gctx :GameContext;

    /** Handles our connection to the game server. */
    protected var _liaison :LobbyGameLiaison;

    /** The mode in which we were opened. */
    protected var _mode :int;

    /** Our distributed LobbyObject */
    protected var _lobj :LobbyObject;

    /** The panel we're controlling. */
    protected var _panel :LobbyPanel;

    /** The table director. */
    protected var _tableDir :TableDirector;

    /** Used to subscribe to our lobby object. */
    protected var _subscriber :SafeSubscriber;

    /** Tracks whether or not our lobby panel is visible. */
    protected var _panelIsVisible :Boolean;

    /** Whether or not the user clicked the close box to close this lobby. */
    protected var _closedByUser :Boolean;

    /** The player whose pending table we'd like to join. */
    protected var _playerId :int = 0;
}
}
