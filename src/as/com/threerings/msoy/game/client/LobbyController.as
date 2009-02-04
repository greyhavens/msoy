//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;

import mx.events.CloseEvent;

import com.threerings.io.TypedArray;
import com.threerings.util.Command;
import com.threerings.util.Controller;
import com.threerings.util.Log;
import com.threerings.util.Name;

import com.threerings.parlor.client.SeatednessObserver;
import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.data.TableConfig;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.Subscriber;
import com.threerings.presents.util.SafeSubscriber;

import com.threerings.msoy.client.BlankPlaceView;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyPlaceView;
import com.threerings.msoy.client.NoPlaceView;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.LobbyCodes;
import com.threerings.msoy.game.data.LobbyMarshaller;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.ui.ScalingMediaContainer;

import com.whirled.game.data.MatchConfig;
import com.threerings.presents.client.ClientEvent;
import com.threerings.msoy.world.client.WorldContext;

public class LobbyController extends Controller
    implements Subscriber, SeatednessObserver
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

    // modes for our user interface
    public static const MODE_SPLASH :int = 0;
    public static const MODE_MATCH :int = 1;
    public static const MODE_CREATE :int = 2;
    public static const MODE_SEATED :int = 3;

    // statically reference classes we require
    MsoyGameDefinition;
    LobbyMarshaller;


    public function LobbyController (gctx :GameContext, mode :LobbyDef, onClear :Function,
        playNow :Function, lobbyLoaded :Function, displaySplash :Boolean)
    {
        _gctx = gctx;
        _mctx = gctx.getMsoyContext();
        _mode = mode;
        _onClear = onClear;
        _playNow = playNow;
        _lobbyLoaded = lobbyLoaded;
        _displaySplash = displaySplash;

        _waitForWorldLogon = new GatedExecutor(function () :Boolean {
            return _gctx.getMsoyContext().getClient().isLoggedOn();
        });

        _lobbyTimer = new LobbyResolutionTimer(_mctx);
        _lobbyTimer.start();

        // create our lobby panel
        _panel = new LobbyPanel(_gctx, this);
        _panel.addEventListener(Event.ADDED_TO_STAGE, handleAddedToStage);
        Command.bind(_panel, CloseEvent.CLOSE, handleCloseLobby);
        setControlledPanel(_panel);
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
        var ourName :MemberName = _gctx.getPlayerObject().memberName;
        for each (var table :Table in _lobj.tables.toArray()) {
            if (table.players != null && -1 != table.players.indexOf(ourName)) {
                return true;
            }
        }
        return false;
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
        for each (var table :Table in _lobj.tables.toArray()) {
            if (isActionableTable(table)) {
                return true;
            }
        }
        return false;
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
        _lobbyTimer.stop();

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
        const match :MatchConfig = _lobj.gameDef.match;
        const partyGame :Boolean = (match.getMatchType() == GameConfig.PARTY);
        const multiplayerSupported :Boolean = (match.getMaximumPlayers() > 1);
        const multiplayerRequired :Boolean = (match.getMinimumPlayers() > 1);

        const lobbyMultiRequested :Boolean = (multiplayerSupported && _mode.multiplayerLobby);

        // if we are a party game or multiplayer only...
        if (partyGame || multiplayerRequired || lobbyMultiRequested) {
            // either go to the matchmaking panel or right into create if there is nothing to show
            // on the matchmaking panel
            return (haveActionableTables() || noCreate) ? MODE_MATCH : MODE_CREATE;
        } else {
            // othrewise show the splash page so they can select single or multiplayer
            return MODE_SPLASH;
        }
    }

    // from Subscriber
    public function objectAvailable (obj :DObject) :void
    {
        _lobj = obj as LobbyObject;
        _lobbyTimer.stop();

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

            // pass group back to the caller now that the lobby has loaded
            _lobbyLoaded(_lobj.groupId);

            // this is only used for testing game loading issues per WRLD-531, and will be removed
            // after the test is over. -- robert
            _mctx.getMsoyClient().trackClientAction("WRLD-531-2 game started", "stage 4");

            // if we have a player table to join, do that now, otherwise
            if (_playerId != 0) {
                joinPlayerTable(_playerId);

            } else {
                // otherwise do something appropriate based on our mode
                switch (_mode) {
                case LobbyCodes.PLAY_NOW_ANYONE:
                    joinSomeTable();
                    break;
                }
            }
        });
    }

    // from Subscriber
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
    }

    /** Called by the LobbyGameLiaison upon login to the world server. */
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
        if (! _displaySplash) {
            return;
        }

        _mctx.setPlaceView(new LobbyPlaceView(game));
    }

    /**
     * Looks for a table that we can join and joins it.
     */
    protected function joinSomeTable () :Boolean
    {
        for each (var table :Table in _lobj.tables.toArray()) {
            if (table.inPlay()) {
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

    /** Lobby definition. */
    protected var _mode :LobbyDef;

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

    /** Should the lobby be displayed on top of a custom splash view? */
    protected var _displaySplash :Boolean;

    /** The player whose pending table we'd like to join. */
    protected var _playerId :int = 0;

    /** Are we seated? */
    protected var _isSeated :Boolean;

    /** Monitors whether we've successfully subscribed to a lobby object. */
    protected var _lobbyTimer :LobbyResolutionTimer;

    /** Executes jobs only when the lobby object and the member object have been resolved. */
    protected var _waitForWorldLogon :GatedExecutor;
}
}

import flash.utils.Timer;
import flash.events.TimerEvent;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCodes;

/** Displays occasional feedback messages while we wait for a lobby. */
class LobbyResolutionTimer
{
    /** Show a message every 5 seconds... */
    public static const DELAY :int = 5000;
    /** ... and show it only three times. */
    public static const COUNT :int = 3;

    public function LobbyResolutionTimer (mctx :MsoyContext)
    {
        _mctx = mctx;
        _timer = new Timer(DELAY, COUNT);
    }

    public function start () :void
    {
        _timer.addEventListener(TimerEvent.TIMER, displayUpdate);
        _timer.addEventListener(TimerEvent.TIMER_COMPLETE, displayFinalUpdate);
        _timer.start();
    }

    public function stop () :void
    {
        _timer.stop();
        _timer.removeEventListener(TimerEvent.TIMER, displayUpdate);
        _timer.removeEventListener(TimerEvent.TIMER_COMPLETE, displayFinalUpdate);
    }

    public function displayUpdate (e :TimerEvent) :void
    {
        _mctx.displayInfo(MsoyCodes.GAME_MSGS, "e.waiting_for_lobby");
    }

    public function displayFinalUpdate (e :TimerEvent) :void
    {
        _mctx.displayInfo(MsoyCodes.GAME_MSGS, "e.waiting_for_lobby_failed");
    }

    protected var _mctx :MsoyContext;
    protected var _timer :Timer;
}
