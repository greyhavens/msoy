//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;

import com.threerings.parlor.game.client.GameController;
import com.threerings.parlor.game.data.GameObject;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.AVRGameObject;
import com.threerings.msoy.game.data.GameContentOwnership;
import com.threerings.msoy.game.data.LobbyMarshaller;
import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.MsoyGameMarshaller;
import com.threerings.msoy.game.data.PlayerObject;

/**
 * A director that manages game related bits.
 */
public class GameDirector extends BasicDirector
{
    public static const log :Log = Log.getLog(GameDirector);

    public function GameDirector (ctx :WorldContext)
    {
        super(ctx);
        _mctx = ctx;

        // let the compiler know that these must be compiled into the client
        var c :Class = MsoyGameDefinition;
        c = MsoyGameMarshaller;
        c = LobbyMarshaller;
        c = LobbyController;
        c = PlayerObject;
        c = GameContentOwnership;
    }

    /**
     * Requests that the lobby for the specified game be joined and displayed.
     */
    public function displayLobby (gameId :int) :void
    {
        if (_liaison != null) {
            if (_liaison is LobbyGameLiaison && _liaison.gameId == gameId) {
                LobbyGameLiaison(_liaison).showLobbyUI();
            } else {
                _liaison.shutdown();
                _liaison = null;
            }
        }
        if (_liaison == null) {
            // create our new liaison, which will resolve the lobby and do all the business
            _liaison = new LobbyGameLiaison(_mctx, gameId, LobbyGameLiaison.SHOW_LOBBY);
        }
    }

    /**
     * Requests that we immediately start playing the specified game id.
     *
     * @param singlePlayer if true we'll start a single player game, if false we'll create or join
     * a multiplayer game.
     */
    public function playNow (gameId :int, singlePlayer: Boolean) :void
    {
        if (_liaison != null) {
            if (_liaison is LobbyGameLiaison && _liaison.gameId == gameId) {
                LobbyGameLiaison(_liaison).playNow(singlePlayer);
            } else {
                _liaison.shutdown();
                _liaison = null;
            }
        }
        if (_liaison == null) {
            // create our new liaison, which will head on into the game once we're logged on
            _liaison = new LobbyGameLiaison(_mctx, gameId, singlePlayer ?
                LobbyGameLiaison.PLAY_NOW_SINGLE : LobbyGameLiaison.PLAY_NOW_MULTI);
        }
    }

    /**
     * Requests that we join the given player in the given game.
     */
    public function joinPlayer (gameId :int, memberId :int) :void
    {
        if (_liaison != null) {
            if (_liaison is AVRGameLiaison || _liaison.gameId != gameId) {
                _liaison.shutdown();
                _liaison = null;
            }
        }

        if (_liaison == null) {
            _liaison = new LobbyGameLiaison(_mctx, gameId, memberId);
        } else {
            LobbyGameLiaison(_liaison).joinPlayer(memberId);
        }
    }

    /**
     * Requests that we join the given player at his pending game table.
     */
    public function joinPlayerTable (gameId :int, memberId :int) :void
    {
        if (_liaison != null) {
            if (_liaison is AVRGameLiaison || _liaison.gameId != gameId) {
                _liaison.shutdown();
                _liaison = null;
            }
        }
        displayLobby(gameId);
        LobbyGameLiaison(_liaison).joinPlayerTable(memberId);
    }

    /**
     * Called when we first login and then every time we leave a game or a lobby;
     * checks to see if we have a persistent AVRG we should (re-)activate.
     */
    public function checkMemberAVRGame () :void
    {
        if (_liaison == null) {
            var memberObj :MemberObject = _mctx.getMemberObject();
            if (memberObj.avrGameId > 0) {
                _liaison = new AVRGameLiaison(_mctx, memberObj.avrGameId);
            }
        }
    }

    public function activateAVRGame (gameId :int) :void
    {
        if (_liaison != null) {
            if (_liaison is LobbyGameLiaison) {
                log.warning("Eek, asked to join an AVRG while in a lobbied game.");
                return;
            }
            if (_liaison.gameId == gameId) {
                log.warning("Requested to activate the AVRG that's already active [gameId=" +
                            gameId + "]");
                return;
            }
            // TODO: implement proper leaving, this should only be the fallback
            _liaison.shutdown();
        }

        _liaison = new AVRGameLiaison(_mctx, gameId);
    }

    public function leaveAVRGame () :void
    {
        if (_liaison != null && _liaison is AVRGameLiaison) {
            AVRGameLiaison(_liaison).leaveAVRGame();
        }
    }

    /**
     * Requests that we move to the specified game location.
     */
    public function enterGame (gameOid :int) :void
    {
        if (_liaison == null) {
            log.warning("Requested to enter game but have no liaison?! [oid=" + gameOid + "].");
        } else if (!(_liaison is LobbyGameLiaison)) {
            log.warning("Requested to enter game but have AVRG liaison?! [oid=" + gameOid + "].");
        } else {
            LobbyGameLiaison(_liaison).enterGame(gameOid);
        }
    }

    /**
     * Returns the configuration of the (non-world) game we currently occupy if we're in a game.
     * Returns null otherwise.
     */
    public function getGameConfig () :MsoyGameConfig
    {
        if (_liaison != null && _liaison is LobbyGameLiaison) {
            return LobbyGameLiaison(_liaison).getGameConfig();
        }
        return null;
    }

    /**
     * Returns the gameId of the game we're currently connected to, or zero if we're not.
     */
    public function getGameId () :int
    {
        return (_liaison != null) ? _liaison.gameId : 0;
    }

    /**
     * Returns the game object if we're currently in an AVRG, null otherwise.
     */
    public function getAVRGameObject () :AVRGameObject
    {
        if (_liaison != null && _liaison is AVRGameLiaison) {
            return AVRGameLiaison(_liaison).getAVRGameObject();
        }
        return null;
    }

    /**
     * Let the caller know if the tutorial is our currently active AVRG.
     */
    public function isPlayingTutorial () :Boolean
    {
        return _liaison != null && _liaison is AVRGameLiaison &&
            _liaison.gameId == Game.TUTORIAL_GAME_ID;
    }

    /**
     * If we're in the tutorial, forward tutorial-specific events to the relevant game object.
     * This should allow us to write the tutorial as an otherwise entirely external AVRG.
     */
    public function tutorialEvent (eventName :String) :void
    {
        if (isPlayingTutorial()) {
            var ctrl :AVRGameController = AVRGameLiaison(_liaison).getAVRGameController();
            if (ctrl != null) {
                ctrl.tutorialEvent(eventName);
            }
        }
    }

    /**
     * Called by the LobbyController when we join or create a game table. We need to keep this
     * around so that we can enter the game properly if/when we end up in an actual game.
     */
    public function setMatchingGame (game :Game) :void
    {
        _matchingGame = game;
    }

    /**
     * Called by the GameLiaison when it has shutdown and gone away.
     */
    public function liaisonCleared (liaison :GameLiaison) : void
    {
        // we could get the "wrong" liaison here, if we were asked to load up a new lobby while
        // another one was active.
        if (_liaison == liaison) {
            _liaison = null;
            // if this was a lobbied game, see about restarting the AVRG
            if (liaison is LobbyGameLiaison) {
                checkMemberAVRGame();
            }
        }
    }

    // from BasicDirector
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        // shutdown any game connection we might have going
        if (_liaison != null) {
            _liaison.shutdown();
        }
    }

    /**
     * Remember the most recent lobbied game we play. Currently this only used to transmit
     * a tutorial event whenever the AVRG reconnects, but we may use it in a more general
     * AVRG setting later.
     */
    public function setMostRecentLobbyGame (game :int) :void
    {
        _mostRecentLobbyGame = game;
    }

    /**
     * Fetch (and destroy) the ID of the most recently played lobby game, or 0 for none.
     */
    public function popMostRecentLobbyGame () :int
    {
        var game :int = _mostRecentLobbyGame;
        _mostRecentLobbyGame = 0;
        return game;
    }

    /**
     * A convenience method to display feedback using the game bundle.
     */
    protected function displayFeedback (msg :String) :void
    {
        _mctx.displayFeedback(MsoyGameCodes.GAME_BUNDLE, msg);
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        client.addServiceGroup(MsoyCodes.GAME_GROUP);
    }

    /** A casted ref to the msoy context. */
    protected var _mctx :WorldContext;

    /** Handles our connection to the game server. */
    protected var _liaison :GameLiaison;

    /** Tracks the game id of the last game of which we joined a table. We need to remember this
     * because by the time we get around to entering that game, we no longer have this info. */
    protected var _matchingGame :Game;

    /** Remember the ID of the most recent lobbied game we played, for the tutorial. */
    protected var _mostRecentLobbyGame :int;
}
}
