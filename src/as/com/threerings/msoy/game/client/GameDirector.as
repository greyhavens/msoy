//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;

import mx.styles.StyleManager;

import com.threerings.util.Log;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;

import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.client.PlaceController;

import com.threerings.flex.CommandMenu;

import com.threerings.parlor.game.data.UserIdentifier;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.skins.CommentButton;
import com.threerings.msoy.utils.Args;
import com.threerings.msoy.utils.Base64Encoder;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.avrg.data.AVRGameConfig;
import com.threerings.msoy.avrg.client.AVRGameBackend;
import com.threerings.msoy.avrg.client.AVRGameController;
import com.threerings.msoy.avrg.client.AVRGameLiaison;

import com.threerings.msoy.game.data.GameGameMarshaller;
import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyUserIdentifier;
import com.threerings.msoy.game.data.ParlorGameConfig;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.WorldGameMarshaller;
import com.threerings.msoy.game.util.GameUtil;

/**
 * An even dispatched when the user has entered or left any game, or entered or left any table
 * in a forming game, or started up their GameLiaison or shut it down. Fuck, anything to do with
 * games.
 *
 * @eventType com.threerings.msoy.game.client.GameDirector.GAMING_STATE_CHANGED
 */
[Event(name="GamingStateChanged", type="flash.events.Event")]

/**
 * A director that manages game related bits.
 */
public class GameDirector extends BasicDirector
{
    /** An event type dispatched when the user has entered or left any game.
     *
     * @eventType GamingStateChanged
     */
    public static const GAMING_STATE_CHANGED :String = "GamingStateChanged";

    /** An event type dispatched when the user has entered or left any parlor table.
     *
     * @eventType TableStateChanged
     */
    public static const TABLE_STATE_CHANGED :String = "TableStateChanged";

    public const log :Log = Log.getLog(this);

    // statically reference classes we require
    MsoyGameDefinition;
    WorldGameMarshaller;
    GameGameMarshaller;

    public function GameDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;

        // set up our user identifier
        UserIdentifier.setIder(MsoyUserIdentifier.getUserId);
    }

    public function dispatchGamingStateChanged () :void
    {
        dispatchEvent(new Event(GAMING_STATE_CHANGED));
    }

    /**
     * Returns true if we're in a lobby or in a game.
     */
    public function isGaming () :Boolean
    {
        return (_liaison != null);
    }

    /**
     * Returns the gameId of the game we're currently connected to, or zero if we're not.
     */
    public function getGameId () :int
    {
        return (_liaison != null) ? _liaison.gameId : 0;
    }

    /**
     */
    public function isInParlorTable () :Boolean
    {
        return (_liaison is ParlorGameLiaison) && ParlorGameLiaison(_liaison).isInTable();
    }

    public function getParlorGameOid () :int
    {
        return (_liaison is ParlorGameLiaison) ? ParlorGameLiaison(_liaison).getGameOid() : 0;
    }

    /**
     */
    public function isInParlorGame () :Boolean
    {
        return (_liaison is ParlorGameLiaison) && ParlorGameLiaison(_liaison).isInGame();
    }

    /**
     * Returns the name of the game we're currently connected to, or null if we're not.
     */
    public function getGameName () :String
    {
        return (_liaison != null) ? _liaison.gameName : null;
    }

    /**
     * Returns the description of the game we're currently connected to, or null if we're not.
     */
    public function getGameDescription () :String
    {
        return (_liaison != null) ? _liaison.gameDescription : null;
    }

    /**
     * Is our game an avr game?
     */
    public function isAVRGame () :Boolean
    {
        return (_liaison is AVRGameLiaison);
    }

    /**
     * Returns the currently active GameContext or null if no game is active.
     */
    public function getGameContext () :GameContext
    {
        return (_liaison != null) ? _liaison.getGameContext() : null;
    }

    /**
     * Clear any game we're in.
     */
    public function clearAnyGame () :void
    {
        if (_liaison != null) {
            _liaison.shutdown();
        }
    }

    /**
     * Returns the configuration of the (non-world) game we currently occupy if we're in a game.
     * Returns null otherwise.
     */
    public function getGameConfig () :ParlorGameConfig
    {
        if (_liaison != null) {
            return _liaison.gameConfig as ParlorGameConfig;
        }
        return null;
    }

    /**
     * Returns the currently active GameController or null if no game is active.
     */
    public function getGameController () :PlaceController
    {
        var gctx :GameContext = getGameContext();
        return (gctx != null) ? gctx.getLocationDirector().getPlaceController() : null;
    }

    /**
     * Populates the supplied array with data to create the game control bar menu.
     *
     * @return true if the menu was populated, false if we're not in a game.
     */
    public function populateGameMenu (menuData :Array) :Boolean
    {
        if (_liaison == null) {
            return false;
        }

        var config :MsoyGameConfig = _liaison.gameConfig;
        var icon :MediaDesc = (config == null) ? null : config.getThumbnail();
        CommandMenu.addTitle(menuData, _liaison.gameName, (icon == null) ? null :
            MediaWrapper.createView(icon, MediaDesc.QUARTER_THUMBNAIL_SIZE));
        menuData.push({label: Msgs.GAME.get("b.gameInstructions"), command: viewGameInstructions,
            icon: StyleManager.getStyleDeclaration(".controlBarGameButton").getStyle("image")});
        if (_liaison.gameGroupId != GameUtil.NO_GROUP) {
            menuData.push({label: Msgs.GAME.get("b.gameGroup"),
                command: MsoyController.VIEW_GROUP, arg: _liaison.gameGroupId });
            menuData.push({label :Msgs.GAME.get("b.game_whirled"),
                command: MsoyController.GO_GROUP_HOME, arg: _liaison.gameGroupId});
        }
        menuData.push({label: Msgs.GAME.get("b.gameShop"), command: viewGameShop });
        if ((config is ParlorGameConfig) &&
                (ParlorGameConfig(config).getGameDefinition().match.getMaximumPlayers() > 1)) {
            menuData.push({label: Msgs.GAME.get("b.gameLobby"), command: displayCurrentLobby});
        }
        menuData.push({label: Msgs.GAME.get("b.gameComment"), command: viewGameComments,
            icon: CommentButton });
        menuData.push({label: Msgs.GAME.get("b.gameInvite"), command: viewDefaultInvitePage});
        menuData.push({label: Msgs.GAME.get("b.gameTrophies"), command: viewGameTrophies});
        if (GameUtil.isDevelopmentVersion(_liaison.gameId) && !(_liaison is AVRGameLiaison)) {
            menuData.push({label: Msgs.GAME.get("b.gameRemoveTrophies"), command: removeTrophies});
        }
        if (_liaison is AVRGameLiaison) {
            menuData.push({label: Msgs.GAME.get("b.gameExit"), command: leaveAVRGame});
            if (DeploymentConfig.devDeployment) {
                menuData.push({label :"Debug Coordinates", command: showCoordinateDebugPanel});
            }
        }

        return true;
    }

    /**
     * Requests that the lobby for the specified game be joined and displayed.
     */
    public function enterLobby (gameId :int, ghost :String = null, gport :int = 0) :void
    {
        resolveLobbyLiaison(gameId, ghost, gport);
        ParlorGameLiaison(_liaison).showLobby();
    }

    /**
     * Requests that the lobby for the current game be displayed. Returns true when successful, or
     * false if there is no existing game or existing lobby (eg. the game is an AVRG) or other
     * problems were encountered.
     */
    public function displayCurrentLobby () :Boolean
    {
        if (_liaison is ParlorGameLiaison) {
            ParlorGameLiaison(_liaison).showLobby();
            return true;
        }
        return false;
    }

    /**
     * Displays the instructions page for the currently active game.
     */
    public function viewGameInstructions () :void
    {
        _wctx.getWorldController().displayPage("games", "d_" + getGameId() + "_i");
    }

    /**
     * Displays the comments page for the currently active game.
     */
    public function viewGameComments () :void
    {
        _wctx.getWorldController().displayPage("games", "d_" + getGameId() + "_c");
    }

    /**
     * Displays the trophies page for the currently active game.
     */
    public function viewGameTrophies () :void
    {
        TrophyPanel.show(getGameContext(), getGameId(), getGameName(), getGameDescription());
    }

    /**
     * Removes the trophies that this player has earned in an in-development game copy.
     */
    public function removeTrophies () :void
    {
        if (!GameUtil.isDevelopmentVersion(_liaison.gameId)) {
            log.warning("Asked to remove copies from a non-development game", "gameId",
                _liaison.gameId);
            return;
        }

        var svc :GameGameService =
            getGameContext().getClient().requireService(GameGameService) as GameGameService;
        svc.removeDevelopmentTrophies(getGameContext().getClient(), _liaison.gameId,
            getGameContext().getWorldContext().confirmListener(
            "m.trophies_removed", MsoyCodes.GAME_MSGS));
    }

    /**
     * Displays the shop page for the currently active game.
     */
    public function viewGameShop (itemType :int = 0, catalogId :int = 0) :void
    {
        var args :String;
        if (catalogId != 0) {
            args = "l_" + itemType + "_" + catalogId;
        } else {
            args = "g_" + getGameId();
            if (itemType != 0) {
                args += "_" + itemType;
            }
        }
        _wctx.getWorldController().displayPage("shop", args);
    }

    /**
     * Brings up the game invite page in our shell, as requested by the game the user is playing.
     */
    public function viewInvitePage (defmsg :String, token :String = "", roomId :int = 0) :void
    {
        var encodedToken :String = encodeBase64(token);
        log.debug("Viewing invite", "token", token, "encoded", encodedToken);
        // we encode the strings so they are valid as part of the URL and the user cannot trivially
        // see them
        _wctx.getWorldController().displayPage("people",
            Args.join("invites", "game", getGameId(), defmsg, encodedToken,
                _liaison is AVRGameLiaison ? 1 : 0, roomId));
    }

    /**
     * Brings up a default game invite page.
     */
    public function viewDefaultInvitePage () :void
    {
        _wctx.getWorldController().displayPage("people",
            Args.join("invites", "game", getGameId(), "", "", _liaison is AVRGameLiaison ? 1 : 0,
                _wctx.getWorldController().getCurrentSceneId()));
    }

    /**
     * Requests that we immediately start playing the specified game id. If a playerId is
     * specified, we will attempt to join that player's game or lobby table.
     */
    public function playNow (gameId :int, playerId :int, ghost :String = null, gport :int = 0,
        inviteToken :String = null, inviterId :int = 0) :void
    {
        log.debug("Resolving liaison", "host", ghost, "port", gport);
        resolveLobbyLiaison(gameId, ghost, gport);
//         log.debug("Setting invite data", "token", inviteToken, "invId", inviterId);
        _liaison.setInviteData(inviteToken, inviterId);
        log.debug("Playing now", "gameId", gameId, "playerId", playerId);
        ParlorGameLiaison(_liaison).playNow(playerId);
    }

    /** Forwards idleness status updates to any AVRG we may be playing. */
    public function setIdle (nowIdle :Boolean) :void
    {
        if (_liaison != null && _liaison is AVRGameLiaison) {
            var ctrl :AVRGameController = AVRGameLiaison(_liaison).getAVRGameController();
            if (ctrl != null) {
                ctrl.setIdle(nowIdle);
            }
        }
    }

    /**
     * Retrieve the backend of the AVRG currently in progress, or null.
     */
    public function getAVRGameBackend () :AVRGameBackend
    {
        if (_liaison != null && _liaison is AVRGameLiaison) {
            return AVRGameLiaison(_liaison).getAVRGameBackend();
        }
        return null;
    }

    /**
     * Activates the specified AVR game, connecting to the appropriate game server and clearing any
     * existing game server connection.
     */
    public function activateAVRGame (
        gameId :int, inviteToken :String = "", inviterMemberId :int = 0) :void
    {
        log.debug("Activating AVR game", "token", inviteToken, "inviter", inviterMemberId);

        if (_liaison != null) {
            if (_liaison is ParlorGameLiaison) {
                log.warning("Eek, asked to join an AVRG while in a lobbied game.");
                return;
            }
            if (_liaison.gameId == gameId) {
                if (_liaison.gameConfig == null) {
                    displayFeedback("m.locating_game");
                }
                return;
            }
            // TODO: implement proper leaving, this should only be the fallback
            _liaison.shutdown();
        }

        displayFeedback("m.locating_game");

        _liaison = new AVRGameLiaison(_wctx, gameId);
        _liaison.setInviteData(inviteToken, inviterMemberId);
        _liaison.start();
        dispatchGamingStateChanged();
    }

    public function leaveAVRGame () :void
    {
        if (_liaison != null && _liaison is AVRGameLiaison) {
            AVRGameLiaison(_liaison).leaveAVRGame();
        }
    }

    /**
     * Requests that we enter the specified in-progress game.
     */
    public function enterGame (gameId :int, gameOid :int) :void
    {
        log.debug("Entering game", "gameId", gameId, "gameOid", gameOid);

        if (_liaison == null) {
            log.warning("Requested to enter game but have no liaison?! [oid=" + gameOid + "].");
            // probably we're following a URL that is super-double-plus out of date; fall back
            // Note we lose the share data here. It might not make sense to send this data
            // back into a new game
            _wctx.getWorldController().handlePlayGame(gameId);

        } else if (!(_liaison is ParlorGameLiaison)) {
            log.warning("Requested to enter game but have AVRG liaison?! [oid=" + gameOid + "].");

        } else {
            ParlorGameLiaison(_liaison).enterGame(gameOid);
        }
    }

    /**
     * Leaves our current parlor game and either returns to the Whirled or displays the lobby.
     */
    public function backToWhirled (showLobby :Boolean) :void
    {
        if (showLobby) {
            ParlorGameLiaison(_liaison).clearGame();
            _wctx.getWorldController().handlePlayGame(getGameId());
        } else {
            // go back to our previous location
            _wctx.getMsoyController().handleMoveBack(true);
        }
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
            dispatchGamingStateChanged();
        }
    }

    // from BasicDirector
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        // if we're actually logging off, rather than just switching servers, then shutdown any
        // active game connection
        if (!event.isSwitchingServers()) {
            clearAnyGame();
        }
    }

    public function getInviteToken () :String
    {
        if (_liaison != null) {
            return _liaison.inviteToken;
        }
        return "";
    }

    public function getInviterMemberId () :int
    {
        if (_liaison != null) {
            return _liaison.inviterMemberId;
        }
    	return 0;
    }

    /**
     * A convenience method to display feedback using the game bundle.
     */
    protected function displayFeedback (msg :String) :void
    {
        _wctx.displayFeedback(MsoyGameCodes.GAME_BUNDLE, msg);
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        client.addServiceGroup(MsoyCodes.GAME_GROUP);
    }

    /**
     * Ensures that we have a game liaison for the specified game.
     */
    protected function resolveLobbyLiaison (gameId :int, ghost :String = null, gport :int = 0) :void
    {
        if (_liaison is ParlorGameLiaison && _liaison.gameId == gameId) {
            return;
        }
        if (_liaison != null) {
            _liaison.shutdown();
        }
        _liaison = new ParlorGameLiaison(_wctx, gameId);
        _liaison.start(ghost, gport);
    }

    protected function showCoordinateDebugPanel () :void
    {
        var ctrl :AVRGameController = AVRGameLiaison(_liaison).getAVRGameController();
        ctrl.showCoordinateDebugPanel();
    }

    protected static function encodeBase64 (str :String) :String
    {
        if (str == null || str == "") {
            return "";
        }
        var encoder :Base64Encoder = new Base64Encoder();
        encoder.encodeUTFBytes(str);
        return encoder.toString();
    }

    /** A casted ref to the msoy context. */
    protected var _wctx :WorldContext;

    /** Handles our connection to the game server. */
    protected var _liaison :GameLiaison;
}
}
