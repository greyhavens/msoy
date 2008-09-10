//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;
import mx.events.CloseEvent;

import com.threerings.io.TypedArray;
import com.threerings.util.Command;
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

import com.threerings.msoy.client.BlankPlaceView;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.NoPlaceView;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.data.LobbyCodes;
import com.threerings.msoy.game.data.LobbyMarshaller;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;

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

    /** A command to boot a player from the table. */
    public static const BOOT_PLAYER :String = "BootPlayer";

    /** A command to close the lobby. */
    public static const CLOSE_LOBBY :String = "CloseLobby";

    /** A command to start a single player game immediately. */
    public static const PLAY_SOLO :String = "PlaySolo";

    public function LobbyController (
        gctx :GameContext, mode :int, onClear :Function, playNow :Function, lobbyLoaded :Function)
    {
        _gctx = gctx;
        _mctx = gctx.getMsoyContext();
        _mode = mode;
        _onClear = onClear;
        _playNow = playNow;
        _lobbyLoaded = lobbyLoaded;

        // let the compiler know that these must be compiled into the client
        var c :Class = MsoyGameDefinition;
        c = LobbyMarshaller;

        // create our lobby panel
        _panel = new LobbyPanel(_gctx, this);
        _panel.addEventListener(Event.ADDED_TO_STAGE, handleAddedToStage);
        Command.bind(_panel, CloseEvent.CLOSE, handleCloseLobby);
        setControlledPanel(_panel);

        // we may be being created during the very beginning of client initialization, so don't
        // open our panel immediately but rather queue it up to be opened on the next frame
        _mctx.getTopPanel().callLater(_panel.open);
    }

    /**
     * Subscribes to our lobby object.
     */
    public function enterLobby (lobbyOid :int) :void
    {
        if (_subscriber == null) {
            _subscriber = new SafeSubscriber(lobbyOid, this);
            _subscriber.subscribe(_gctx.getDObjectManager());
        } else {
            Log.getLog(this).warning("Asked to re-enter lobby [sub=" + _subscriber +
                                     ", newOid=" + lobbyOid + "].");
        }
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
     * Returns the table director in use by this lobby.
     */
    public function get tableDir () :TableDirector
    {
        return _tableDir;
    }

    /**
     * Handles PLAY_SOLO.
     */
    public function handlePlaySolo () :void
    {
        _playNow(LobbyCodes.PLAY_NOW_SINGLE);
    }

    /**
     * Handles SUBMIT_TABLE.
     */
    public function handleSubmitTable (
        tcfg :TableConfig, gcfg :GameConfig, friendIds :TypedArray) :void
    {
        _tableDir.createTable(tcfg, gcfg);

        // if requested, send an invitation to our friends, inviting them to this game
        if (friendIds.length > 0) {
            var gsvc :WorldGameService =
                (_mctx.getClient().requireService(WorldGameService) as WorldGameService);
            gsvc.inviteFriends(_mctx.getClient(), gcfg.getGameId(), friendIds);
        }
    }

    /**
     * Handles JOIN_TABLE.
     */
    public function handleJoinTable (tableId :int, position :int) :void
    {
        _tableDir.joinTable(tableId, position);
    }

    /**
     * Handles LEAVE_TABLE.
     */
    public function handleLeaveTable (tableId :int) :void
    {
        _tableDir.leaveTable(tableId);
    }

    /**
     * Handles START_TABLE.
     */
    public function handleStartTable (tableId :int) :void
    {
        _tableDir.startTableNow(tableId);
    }

    /**
     * Handles CLOSE_LOBBY.
     */
    public function handleCloseLobby () :void
    {
        _closedByUser = true;
        forceShutdown();
    }

    /**
     * Handles BOOT_PLAYER.
     */
    public function handleBootPlayer (tableId :int, target :Name) :void
    {
        _tableDir.bootPlayer(tableId, target);
    }

    /**
     * Leaves any occupied table and then shuts down our lobby.
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
        shutdown();
    }

    /**
     * Shuts down the lobby without leaving any occupied table (ie. when we want to enter our
     * game).
     */
    public function shutdown () :void
    {
        // first do our UI cleanup
        _panel.close();

        // then our distributed services cleanup
        if (_subscriber != null) {
            _subscriber.unsubscribe(_gctx.getDObjectManager());
        }
        if (_tableDir != null) {
            _tableDir.clearTableObject();
            _tableDir.removeTableObserver(_panel);
            _tableDir.removeSeatednessObserver(_panel);
        }

        // finally let whoever cares know that we're gone
        _onClear(_closedByUser);
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
            for each (var player :Name in table.players) {
                var member :MemberName = (player as MemberName);
                if (member == null || member.getMemberId() != playerId) {
                    continue;
                }

                if (table.inPlay()) {
                    _mctx.displayFeedback(MsoyCodes.GAME_MSGS, "e.game_in_progress");
                } else {
                    var ii :int = 0;
                    for (; ii < table.players.length; ii++) {
                        if (table.players[ii] == null) {
                            handleJoinTable(table.tableId, ii);
                            break;
                        }
                    }
                    if (ii == table.players.length) {
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
        _panel.open();
    }

    /**
     * Event handler for Event.ADDED_TO_STAGE
     */
    public function handleAddedToStage (evt :Event) :void
    {
        if (_lobj != null) {
            _mctx.getMsoyClient().setWindowTitle(_lobj.game.name);
        }

        // if we're showing the blank view, switch instead to the noview
        if (_mctx.getTopPanel().getPlaceView() is BlankPlaceView) {
            _mctx.getTopPanel().setPlaceView(new NoPlaceView());
        }
    }

    // from Subscriber
    public function objectAvailable (obj :DObject) :void
    {
        _lobj = obj as LobbyObject;
        _panel.init(_lobj, _mode == LobbyCodes.PLAY_NOW_FRIENDS);

        _tableDir = new TableDirector(_gctx, LobbyObject.TABLES);
        _tableDir.setTableObject(obj);
        _tableDir.addTableObserver(_panel);
        _tableDir.addSeatednessObserver(_panel);

        _mctx.getMsoyClient().setWindowTitle(_lobj.game.name);

        // if we have a player table to join, do that now, otherwise
        if (_playerId != 0) {
            joinPlayerTable(_playerId);
            return;
        }

        // otherwise do something appropriate based on our mode
        switch (_mode) {
        case LobbyCodes.PLAY_NOW_FRIENDS:
            joinSomeTable(true);
            break;
        case LobbyCodes.PLAY_NOW_ANYONE:
            joinSomeTable(false);
            break;
        }

        // pass group back to the caller once lobby has loaded
        _lobbyLoaded(_lobj.groupId);
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
                (friendsOnly && LobbyPanel.countFriends(table, _gctx.getPlayerObject()) == 0)) {
                continue;
            }
            for (var ii :int; ii < table.players.length; ii++) {
                if (table.players[ii] == null) {
                    handleJoinTable(table.tableId, ii);
                    return true;
                }
            }
        }
        return false;
    }

    /** The provider of free cheese. */
    protected var _mctx :MsoyContext;

    /** The provider of game related services. */
    protected var _gctx :GameContext;

    /** The mode in which we were opened. */
    protected var _mode :int;

    /** Called when we shut ourselves down. */
    protected var _onClear :Function;

    /** Called when the player wants instant action. */
    protected var _playNow :Function;

    /** Called when the lobby dialog is done loading, with the game's groupId as argument. */
    protected var _lobbyLoaded :Function;

    /** Our distributed LobbyObject */
    protected var _lobj :LobbyObject;

    /** The panel we're controlling. */
    protected var _panel :LobbyPanel;

    /** The table director. */
    protected var _tableDir :TableDirector;

    /** Used to subscribe to our lobby object. */
    protected var _subscriber :SafeSubscriber;

    /** Whether or not the user clicked the close box to close this lobby. */
    protected var _closedByUser :Boolean;

    /** The player whose pending table we'd like to join. */
    protected var _playerId :int = 0;
}
}
