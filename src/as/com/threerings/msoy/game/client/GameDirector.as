//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;

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

import com.threerings.msoy.game.data.GameCodes;
import com.threerings.msoy.game.data.LobbyMarshaller;
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
    }

    /**
     * Requests that the lobby for the specified game be joined and displayed.
     */
    public function displayLobby (gameId :int) :void
    {
        if (_liaison != null) {
            if (_liaison.gameId == gameId) {
                LobbyGameLiaison(_liaison).showLobbyUI();
            } else {
                // TODO: close current game and open new one?
                log.info("Zoiks, asked to switch to new lobby [in=" + _liaison.gameId +
                         ", want=" + gameId + "].");
            }
            return;
        }

        // create our new liaison, which will resolve the lobby and do all the business
        _liaison = new LobbyGameLiaison(_mctx, gameId);
    }

    /**
     * Requests that we join the given player in the given game.
     */
    public function joinPlayer (gameId :int, memberId :int) :void
    {
        if (_liaison != null && _liaison.gameId != gameId) {
            _liaison.shutdown();
            _liaison = null;
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
        if (_liaison != null && _liaison.gameId != gameId) {
            _liaison.shutdown();
            _liaison = null;
        }
        displayLobby(gameId);
        LobbyGameLiaison(_liaison).joinPlayerTable(memberId);
    }

    public function activateAVRGame (gameId :int) :void
    {
        if (_liaison != null) {
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
        }
    }

    // from BasicDirector
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        // shutdown and game connection we might have going
        if (_liaison != null) {
            _liaison.shutdown();
        }
    }

    /**
     * A convenience method to display feedback using the game bundle.
     */
    protected function displayFeedback (msg :String) :void
    {
        _mctx.displayFeedback(GameCodes.GAME_BUNDLE, msg);
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

}
}
