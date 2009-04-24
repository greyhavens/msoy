//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;

import mx.events.CloseEvent;

import com.threerings.io.TypedArray;
import com.threerings.util.ArrayUtil;
import com.threerings.util.Command;
import com.threerings.util.Controller;
import com.threerings.util.Log;
import com.threerings.util.Name;
import com.threerings.util.Util;

import com.threerings.parlor.client.SeatednessObserver;
import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.msoy.client.BlankPlaceView;
import com.threerings.msoy.client.NoPlaceView;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.LobbyMarshaller;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.MsoyTableConfig;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.party.client.PartyDirector;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.presents.client.ClientEvent;

public class LobbyController extends Controller
    implements SeatednessObserver
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

    // modes for our user interface
    public static const MODE_MATCH :int = 1;
    public static const MODE_CREATE :int = 2;
    public static const MODE_SEATED :int = 3;

    // statically reference classes we require
    MsoyGameDefinition;
    LobbyMarshaller;

    public function LobbyController (
        gctx :GameContext, lobbyOid :int, onClear :Function, displaySplash :Boolean)
    {
        _gctx = gctx;
        _mctx = gctx.getWorldContext();
        _onClear = onClear;
        _displaySplash = displaySplash;

        _waitForWorldLogon = new GatedExecutor(_gctx.getWorldContext().getClient().isLoggedOn);

        // create our lobby panel
        _panel = new LobbyPanel(_gctx, this);
        _panel.addEventListener(Event.ADDED_TO_STAGE, handleAddedToStage);
        Command.bind(_panel, CloseEvent.CLOSE, handleCloseLobby);
        setControlledPanel(_panel);

        // subscribe to the lobby object
        _subscriber = new SafeSubscriber(lobbyOid, objectAvailable, requestFailed);
        _subscriber.subscribe(_gctx.getDObjectManager());
    }

    /**
     * Returns the item id of the game managed by this lobby controller. Not valid until we've
     * subscribed to our lobby object.
     */
    public function get gameItemId () :int
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
     * Returns the main lobby display container.
     */
    public function get panel () :LobbyPanel
    {
        return _panel;
    }

    /**
     * Returns true if we're seated at ANY table, even in another lobby.
     */
    public function isSeated () :Boolean
    {
        // if we know we're seated, just return that
        if (_isSeated) {
            return true;
        }

        // otherwise look at the data
        var ourName :MemberName = _gctx.getMyName();
        return _lobj.tables.toArray().some(function (table :Table, ... rest) :Boolean {
            return ArrayUtil.contains(table.players, ourName);
        });
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

        // find the table with the specified playerId
        var table :Table = ArrayUtil.findIf(_lobj.tables.toArray(), function (t :Table) :Boolean {
            return !t.inPlay() && (t.players != null) &&
                t.players.some(function (name :*, ... rest) :Boolean {
                    return (name is MemberName) && (MemberName(name).getMemberId() == playerId);
                });
        });
        if (table != null) {
            if (-1 == table.players.indexOf(null)) {
                _mctx.displayFeedback(MsoyCodes.GAME_MSGS, "e.game_table_full");
            } else {
                // it might be full anyway once our request hits the server..
                handleJoinTable(table.tableId, Table.ANY_POSITION);
            }
        }
    }

    /**
     * Returns the count of friends of the specified member that are seated at this table.
     */
    public function countFriends (table :Table) :int
    {
        var friendIds :Array = _gctx.getOnlineFriends().map(
            function (f :FriendEntry) :int { return f.name.getMemberId() });
        var plobj :PlayerObject = _gctx.getPlayerObject();
        var friends :int = 0, ourId :int = plobj.memberName.getMemberId();
        for (var ii :int; ii < table.players.length; ii++) {
            var name :MemberName = (table.players[ii] as MemberName);
            if (name == null) {
                continue;
            }
            var friendId :int = name.getMemberId();
            if (friendIds.indexOf(friendId) >= 0 || friendId == ourId) {
                friends++;
            }
        }
        return friends;
    }

    /**
     * Returns true if there are tables that we might join, or watch or otherwies interact with.
     */
    public function haveActionableTables () :Boolean
    {
        return _lobj.tables.toArray().some(Util.adapt(isActionableTable));
    }

    /**
     * Returns true if this table is "actionable" (we can do something with it like watch the game,
     * play the game or join the table).
     */
    public function isActionableTable (table :Table) :Boolean
    {
        // if we're a maintainer, show all tables; it's useful for debugging
        if (_gctx.getPlayerObject().tokens.isMaintainer()) {
            return true;
        }

        // if it's a running unwatchable seated game, no action is possible
        var inPlayUnwatchable :Boolean = (table.config.getMatchType() != GameConfig.PARTY) &&
            table.inPlay() && (_lobj.gameDef.match as MsoyMatchConfig).unwatchable;

        // if it's a private table, it's not actionable (TODO: what does private table really mean,
        // how does anyone ever join a private table?)
        return !(inPlayUnwatchable || table.tconfig.privateTable);
    }

    /**
     * Handles SUBMIT_TABLE.
     */
    public function handleSubmitTable (
        tcfg :TableConfig, gcfg :GameConfig, friendIds :Array) :void
    {
        var mtcfg :MsoyTableConfig = MsoyTableConfig(tcfg);
        const partyDir :PartyDirector = _gctx.getWorldContext().getPartyDirector();
        const isPartyLeader :Boolean = partyDir.isPartyLeader();
        if (isPartyLeader) {
            mtcfg.partyId = partyDir.getPartyId();
        }

        _tableDir.createTable(tcfg, gcfg);

        // if requested, send an invitation to our friends, inviting them to this game
        if (friendIds.length > 0) {
            var gsvc :WorldGameService =
                (_mctx.getClient().requireService(WorldGameService) as WorldGameService);
            // TODO: invite friends to party if partyLeader, rather than inviting to table???
            gsvc.inviteFriends(_mctx.getClient(), gcfg.getGameId(),
                TypedArray.create(int, friendIds));
        }
    }

    /**
     * Handles JOIN_TABLE.
     */
    public function handleJoinTable (tableId :int, position :int) :void
    {
        // if they're trying to join a party table, force them to join the party instead
        var table :Table = _lobj.tables.get(tableId) as Table;
        var tablePartyId :int = (table == null) ? 0 : MsoyTableConfig(table.tconfig).partyId;
        if ((tablePartyId != 0) && (tablePartyId != _mctx.getPartyDirector().getPartyId())) {
            _mctx.getPartyDirector().joinParty(tablePartyId);

        } else {
            // else, just join the table
            _tableDir.joinTable(tableId, position);
        }
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
                seatednessDidChange(false);
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
        _mctx.getUIState().setInLobby(false);

        // then our distributed services cleanup
        if (_subscriber != null) {
            _subscriber.unsubscribe(_gctx.getDObjectManager());
        }
        if (_tableDir != null) {
            _tableDir.clearTableObject();
            _tableDir.removeSeatednessObserver(this);
        }

        // finally let whoever cares know that we're gone
        _onClear(_closedByUser);
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
        if (_mctx.getPlaceView() is BlankPlaceView) {
            _mctx.setPlaceView(new NoPlaceView());
        }
    }

    /**
     * Returns the default starting lobby panel mode for our game type and current circumstances.
     *
     * @param noCreate true if the caller requires that we not go straight to table creation.
     */
    public function getStartMode (noCreate :Boolean = false) :int
    {
        // either go to the matchmaking panel or right into create if there is nothing to show
        // on the matchmaking panel
        return (haveActionableTables() || noCreate) ? MODE_MATCH : MODE_CREATE;
    }

    public function objectAvailable (obj :DObject) :void
    {
        _lobj = obj as LobbyObject;

        _mctx.getMsoyClient().setWindowTitle(_lobj.game.name);

        // create our table director
        _tableDir = new TableDirector(_gctx, LobbyObject.TABLES);
        _tableDir.setFailureHandler(function (cause :String) :void {
            _mctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
        });
        _tableDir.setTableObject(obj);
        _tableDir.addSeatednessObserver(this);

        // replace the current view with the game's splash screen
        setGameView(_lobj.game);

        _waitForWorldLogon.execute(function () :void {
            // set up our starting panel mode
            _panel.init(_lobj);
            _panel.open();
            _panel.setMode(getStartMode());
            _mctx.getUIState().setInLobby(true);

            // if we have a player table to join, do that now, otherwise
            if (_playerId != 0) {
                joinPlayerTable(_playerId);
                _playerId = 0;
            }
        });
    }

    public function requestFailed (oid :int, cause :ObjectAccessError) :void
    {
        Log.getLog(this).warning("Request for the LobbyObject failed: " + cause);
    }

    // from SeatednessObserver
    public function seatednessDidChange (nowSeated :Boolean) :void
    {
        _isSeated = nowSeated;
        if (nowSeated && _tableDir.getSeatedTable().shouldBeStarted()) {
            return;
        }
        _panel.setMode(nowSeated ? MODE_SEATED : MODE_MATCH);
        _mctx.getGameDirector().dispatchGamingStateChanged();
    }

    /** Called by the ParlorGameLiaison upon login to the world server. */
    public function worldClientDidLogon (event :ClientEvent) :void
    {
        // run anything that's been waiting on the member object
        _waitForWorldLogon.update();
    }

    /**
     * Once the lobby object has been located, this function pulls out the splash media,
     * and creates a new place view to display it.
     */
    protected function setGameView (game :Game) :void
    {
        if (!_displaySplash) {
            return;
        }
        _mctx.setPlaceView(new LobbyPlaceView(game));
    }

    /** The provider of free cheese. */
    protected var _mctx :WorldContext;

    /** The provider of game related services. */
    protected var _gctx :GameContext;

    /** Called when we shut ourselves down. */
    protected var _onClear :Function;

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

    /** Should the lobby be displayed on top of a custom splash view? */
    protected var _displaySplash :Boolean;

    /** The player whose pending table we'd like to join. */
    protected var _playerId :int = 0;

    /** Are we seated? */
    protected var _isSeated :Boolean;

    /** Executes jobs only when the lobby object and the member object have been resolved. */
    protected var _waitForWorldLogon :GatedExecutor;
}
}
