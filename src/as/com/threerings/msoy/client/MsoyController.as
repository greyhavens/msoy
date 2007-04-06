//
// $Id$

package com.threerings.msoy.client {

import flash.events.IEventDispatcher;
import flash.events.KeyboardEvent;
import flash.events.TextEvent;

import flash.system.Capabilities;

import flash.text.TextField;

import flash.ui.Keyboard;

import mx.controls.Button;

import com.threerings.util.Controller;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.NetUtil;
import com.threerings.util.StringUtil;
import com.threerings.util.CommandEvent;

import com.threerings.flex.CommandMenu;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.crowd.chat.client.ChatCantStealFocus;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneObject;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.SceneBookmarkEntry;

import com.threerings.msoy.chat.client.ChatControl;

import com.threerings.msoy.web.data.FriendEntry;
import com.threerings.msoy.web.data.MemberName;

import com.threerings.msoy.chat.client.ChatControl;
import com.threerings.msoy.chat.client.ReportingListener;

import com.threerings.msoy.item.web.ItemIdent;

import com.threerings.msoy.game.client.LobbyController;
import com.threerings.msoy.game.client.LobbyService;
import com.threerings.msoy.game.client.WorldGameService;
import com.threerings.msoy.game.client.FloatingTableDisplay;

import com.threerings.msoy.world.client.RoomView;

public class MsoyController extends Controller
    implements ClientObserver
{
    public static const log :Log = Log.getLog(MsoyController);

    /** Command to show the 'about' dialog. */
    public static const ABOUT :String = "About";

    /** Command to log us on. */
    public static const LOGON :String = "Logon";

    /** Command to display the friends list. */
    public static const SHOW_FRIENDS :String = "ShowFriends";

    /** Command to display the recent scenes list. */
    public static const POP_ROOMS_MENU :String = "PopRoomsMenu";

    /** Command to display the friends menu. */
    public static const POP_FRIENDS_MENU :String = "PopFriendsMenu";

    /** Command to display the pets popup. */
    public static const SHOW_PETS :String = "ShowPets";

    /** Command to go to a particular place (by Oid). */
    public static const GO_LOCATION :String = "GoLocation";

    /** Command to go to a particular scene. */
    public static const GO_SCENE :String = "GoScene";

    /** Command to go to a member's home scene. */
    public static const GO_MEMBER_HOME :String = "GoMemberHome";

    /** Command to go to a group's home scene. */
    public static const GO_GROUP_HOME :String = "GoGroupHome";

    /** Command to join a game lobby. */
    public static const JOIN_GAME_LOBBY :String = "JoinGameLobby";

    /** Command to join an in-world game. */
    public static const JOIN_WORLD_GAME :String = "JoinWorldGame";

    /** Command to leave the in-world game. */
    public static const LEAVE_WORLD_GAME :String = "LeaveWorldGame";

    /** Command to add/remove friends. */
    public static const ALTER_FRIEND :String = "AlterFriend";

    /** Command to initiate private conversation with the specified user. */
    public static const TELL :String = "Tell";

    /** Command to edit preferences. */
    public static const CHAT_PREFS :String = "ChatPrefs";

    /** Command to select a different avatar. */
    public static const PICK_AVATAR :String = "PickAvatar";
    
    /** Command to view an item, arg is [ itemTypeId, itemId ] */
    public static const VIEW_ITEM :String = "ViewItem";

    /** Command to view a member's profile, arg is [ memberId ] */
    public static const VIEW_MEMBER :String = "ViewMember";

    /** Command to view the app in full-screen mode. */
    public static const TOGGLE_FULLSCREEN :String = "ToggleFullscreen";

    /**
     * Create the msoy controller.
     */
    public function MsoyController (ctx :WorldContext, topPanel :TopPanel)
    {
        _ctx = ctx;
        _ctx.getClient().addClientObserver(this);
        _topPanel = topPanel;
        setControlledPanel(ctx.getStage());
    }

    /**
     * Handle the SHOW_FRIENDS command.
     */
    public function handleShowFriends (show :Boolean) :void
    {
        _topPanel.showFriends(show);
    }

    /**
     * Handle the TELL command.
     */
    public function handleTell (user :MemberName) :void
    {
        ChatControl.initiateTell(user);
    }

    /**
     * Handle the POP_FRIENDS_MENU command.
     */
    public function handlePopFriendsMenu (trigger :Button) :void
    {
        var friends :Array = _ctx.getMemberObject().getSortedEstablishedFriends();
        friends = friends.map(
            function (fe :FriendEntry, index :int, array :Array) :Object {
                return {
                    label: fe.name.toString(),
                    command: TELL,
                    arg: fe.name
                };
            });

        if (friends.length == 0) {
            friends.push({ label: Msgs.GENERAL.get("m.no_friends") });
        }

        var chatters :Array = _ctx.getChatDirector().getChatters();
        chatters = chatters.map(
            function (name :MemberName, index :int, array :Array) :Object {
                return {
                    label: name.toString(),
                    command: TELL,
                    arg: name
                };
            });

        if (chatters.length == 0) {
            chatters.push({ label: Msgs.GENERAL.get("m.no_chatters") });
        }

        var menuData :Array = [
            { label: Msgs.GENERAL.get("l.recent_chatters"),
              children: chatters },
            { label: Msgs.GENERAL.get("l.friends"),
              children: friends }
        ];

        CommandMenu.createMenu(menuData).popUp(trigger, true);
    }

    /**
     * Handle the POP_ROOMS_MENU command.
     */
    public function handlePopRoomsMenu (trigger :Button) :void
    {
        var scene :Scene = _ctx.getSceneDirector().getScene();
        var currentSceneId :int = (scene == null) ? -1 : scene.getId();
        if (!(_ctx.getLocationDirector().getPlaceObject() is SceneObject)) {
            currentSceneId = -1;
        }

        var memberObj :MemberObject = _ctx.getMemberObject();

        var friends :Array = memberObj.getSortedEstablishedFriends();
        friends = friends.map(
            function (fe :FriendEntry, index :int, array :Array) :Object {
                return {
                    label: fe.name.toString(),
                    command: GO_MEMBER_HOME,
                    arg: fe.getMemberId()
                };
            });

        var recent :Array = memberObj.recentScenes.toArray();
        recent.sort(
            function (sb1 :SceneBookmarkEntry, sb2 :SceneBookmarkEntry) :int {
                return int(sb1.lastVisit - sb2.lastVisit);
            });

        var owned :Array = memberObj.ownedScenes.toArray();
        // TODO: sort owned?

        var bookmarkMapper :Function = function (
            sb :SceneBookmarkEntry, index :int, array :Array) :Object {
                return {
                    label: sb.toString(),
                    enabled: (sb.sceneId != currentSceneId),
                    command: GO_SCENE,
                    arg: sb.sceneId
                };
            };

        recent = recent.map(bookmarkMapper);
        owned = owned.map(bookmarkMapper);

        var menuData :Array = [];

        // add the friends if present
        if (friends.length > 0) {
            menuData.push({ label: Msgs.GENERAL.get("l.visit_friends"),
                children: friends });
        }
        // add owned scenes, if any
        if (owned.length > 0) {
            menuData.push({ label: Msgs.GENERAL.get("l.owned_scenes"),
                children: owned});
        }
        // always add recent scenes
        menuData.push({ label: Msgs.GENERAL.get("l.recent_scenes"),
            children: recent });

        if (!memberObj.isGuest()) {
            menuData.push(
                { type: "separator" },
                { label: Msgs.GENERAL.get("l.go_home"),
                  enabled: (memberObj.homeSceneId != currentSceneId),
                  command :GO_SCENE,
                  arg: memberObj.homeSceneId
                });
        }

        CommandMenu.createMenu(menuData).popUp(trigger, true);
    }

    /**
     * Handle the ABOUT command.
     */
    public function handleAbout () :void
    {
        new AboutDialog(_ctx);
    }

    /**
     * Handle the SHOW_PETS command.
     */
    public function handleShowPets (show :Boolean) :void
    {
        if (_topPanel.getPlaceView() is RoomView) {
            new PetsDialog(_ctx);
        }
    }

    /**
     * @return true if this player appears to support full-screen mode.
     */
    public function supportsFullScreen () :Boolean
    {
        // TODO: this too could be cleaned up. See note in handleToggleFullscreen
        var o :Object = _ctx.getStage();
        try {
            return (undefined !== o.displayState);
        } catch (e :Error) {
        }
        return false;
    }

    /**
     * Convenience method for opening an external window and showing
     * the specified url. This is done when we want to show the user something
     * without unloading the msoy world.
     */
    public function showExternalURL (url :String) :void
    {
        if (!NetUtil.navigateToURL(url, false)) {
            _ctx.displayFeedback(null, MessageBundle.tcompose("e.no_navigate", url));
        }
    }

    /**
     * Handle the TOGGLE_FULLSCREEN command.
     */
    public function handleToggleFullscreen () :void
    {
        // TODO: once things are more up to date, we can use the real
        // class and StageDisplayState for the constants
        var o :Object = _ctx.getStage();
        o.displayState = (o.displayState == "normal") ? "fullScreen" : "normal";
    }

    /**
     * Handle the VIEW_ITEM command.
     */
    public function handleViewItem (ident :ItemIdent) :void
    {
        handleInternalGo("item", "" + ident.itemId);
    }

    /**
     * Handle the VIEW_MEMBER command.
     */
    public function handleViewMember (memberId :int) :void
    {
        handleInternalGo("profile", "" + memberId);
    }

    /**
     * Handle the GO_SCENE command.
     */
    public function handleGoScene (sceneId :int) :void
    {
        _ctx.getSceneDirector().moveTo(sceneId);
    }

    /**
     * Handle the GO_MEMBER_HOME command.
     */
    public function handleGoMemberHome (memberId :int, direct :Boolean = false) :void
    {
        _ctx.getWorldDirector().goToMemberHome(memberId);
    }

    /**
     * Handle the GO_GROUP_HOME command.
     */
    public function handleGoGroupHome (groupId :int, direct :Boolean = false) :void
    {
        _ctx.getWorldDirector().goToGroupHome(groupId);
    }

    /**
     * Handle the GO_LOCATION command to go to a placeobject.
     */
    public function handleGoLocation (placeOid :int) :void
    {
        if (!handleInternalGo("world", "l" + placeOid)) {
            // fall back to breaking the back button
            _ctx.getLocationDirector().moveTo(placeOid);
        }
    }

    /**
     * Handle JOIN_GAME_LOBBY.
     */
    public function handleJoinGameLobby (gameId :int) :void
    {
        var disp :FloatingTableDisplay = _ctx.getTopPanel().getTableDisplay();
        if (disp != null && disp.getGameId() == gameId) {
            // if we're already in a table for this game id, just rejoin the current lobby
            CommandEvent.dispatch(disp.getRenderer(), LobbyController.JOIN_LOBBY);
        } else {
            var lsvc :LobbyService = 
                (_ctx.getClient().requireService(LobbyService) as LobbyService);
            lsvc.identifyLobby(_ctx.getClient(), gameId,
                new ResultWrapper(function (cause :String) :void {
                    log.warning("fetching LobbyObject oid failed: " + cause);
                    _gameId = -1;
                },
                function (result :Object) :void {
                    // this will create a panel and add it to the side panel on the top level
                    new LobbyController(_ctx, int(result));
                    gameLobbyShown(gameId);
                }));
        }
    }

    /**
     * Handle JOIN_WORLD_GAME.
     */
    public function handleJoinWorldGame (gameId :int) :void
    {
        var wgsvc :WorldGameService =
            (_ctx.getClient().requireService(WorldGameService) as WorldGameService);
        wgsvc.joinWorldGame(_ctx.getClient(), gameId,
            new InvocationAdapter(function (cause :String) :void {
                log.warning("Ack: " + cause);
            }));
    }

    /**
     * Handle LEAVE_WORLD_GAME.
     */
    public function handleLeaveWorldGame () :void
    {
        var wgsvc :WorldGameService =
            (_ctx.getClient().requireService(WorldGameService) as WorldGameService);
        wgsvc.leaveWorldGame(_ctx.getClient(),
            new InvocationAdapter(function (cause :String) :void {
                log.warning("Ack: " + cause);
            }));
    }

    /**
     * Handles ALTER_FRIEND.
     */
    public function handleAlterFriend (args :Array) :void
    {
        _ctx.getMemberDirector().alterFriend(int(args[0]), Boolean(args[1]));
    }

    /**
     * Handles CHAT_PREFS.
     */
    public function handleChatPrefs () :void
    {
        new ChatPrefsDialog(_ctx);
    }

    /**
     * Handles PICK_AVATAR.
     */
    public function handlePickAvatar () :void
    {
        new AvatarSelectionDialog(_ctx);
    }

    /**
     * Handle the LOGON command.
     */
    public function handleLogon (creds :MsoyCredentials) :void
    {
        _ctx.getClient().logoff(false);
        _topPanel.callLater(function () :void {
            var client :Client = _ctx.getClient();
            if (creds == null) {
                creds = new MsoyCredentials(null, null);
                creds.ident = "";
            }
            log.info("Logging on [creds=" + creds + "].");
            client.setCredentials(creds);
            client.setVersion(DeploymentConfig.version);
            client.logon();
        });
    }

    /**
     * Figure out where we should be going, and go there.
     */
    public function goToPlace (params :Object) :void
    {
        _sceneIdString = params["sceneId"];
        
        // check for gameLobby first, so that the handleInternalGo call resulting from moveTo
        // adds the correct parameters to the external URL
        if (null != params["gameLobby"]) {
            handleJoinGameLobby(params["gameLobby"])
        }

        // first, see if we should hit a specific scene
        if (null != params["memberHome"]) {
            handleGoMemberHome(int(params["memberHome"]), true);

        } else if (null != params["groupHome"]) {
            handleGoGroupHome(int(params["groupHome"]), true);

        } else if (null != params["location"]) {
            _ctx.getLocationDirector().moveTo(int(params["location"]));

        } else if (null != params["noplace"]) {
            // go to no place- we just want to chat with our friends
            _ctx.getTopPanel().setPlaceView(new NoPlaceView(_ctx));

        } else {
            var sceneIdString :String = params["sceneId"];
            if (sceneIdString != null && sceneIdString.indexOf("g") != -1) {
                var idx :int = sceneIdString.indexOf("g");
                var gameId :int = int(sceneIdString.substring(idx + 1));
                sceneIdString = sceneIdString.substring(0, idx);
                if (_gameId == -1 || _gameId != gameId) {
                    handleJoinGameLobby(gameId);
                }

            } else if (sceneIdString == null && null != params["gameLobby"]) {
                // we want to stay in our current room, if we're in one
                var scene :Scene = _ctx.getSceneDirector().getScene();
                if (scene != null) {
                    sceneIdString = "" + scene.getId();
                }
            }

            var sceneId :int = int(sceneIdString);
            if (sceneId == 0) {
                sceneId = _ctx.getMemberObject().homeSceneId;
                if (sceneId == 0) {
                    sceneId = 1; // for "packwards combatability"
                }
            }
            _ctx.getSceneDirector().moveTo(sceneId);
        }

        // see if we should join a world game
        if (null != params["worldGame"]) {
            handleJoinWorldGame(int(params["worldGame"]));
        }
    }

    /**
     * Called by the scene director when we've traveled to a new scene.
     */
    public function wentToScene (sceneId :int) :void
    {
        // this will result in another request to move to the scene we're already in, but we'll
        // ignore it because we're already there
        handleInternalGo("world", "s" + sceneId + (_gameId != -1 ? "g" + _gameId : ""));
    }

    // from ClientObserver
    public function clientWillLogon (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientDidLogon (event :ClientEvent) :void
    {
        var memberObj :MemberObject = _ctx.getMemberObject();
        // if not a guest, save the username that we logged in with
        if (!memberObj.isGuest()) {
            var creds :MsoyCredentials = (_ctx.getClient().getCredentials() as MsoyCredentials);
            var name :Name = creds.getUsername();
            if (name != null) {
                Prefs.setUsername(name.toString());
            }
        }

        goToPlace(_topPanel.loaderInfo.parameters);
    }

    // from ClientObserver
    public function clientObjectDidChange (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientDidLogoff (event :ClientEvent) :void
    {
        _topPanel.clearSidePanel(null);
        _topPanel.setPlaceView(new DisconnectedPanel(_ctx, _logoffMessage));
        _logoffMessage = null;
    }

    // from ClientObserver
    public function clientFailedToLogon (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientConnectionFailed (event :ClientEvent) :void
    {
        _logoffMessage = Msgs.GENERAL.get("m.lost_connection");
    }

    // from ClientObserver
    public function clientWillLogoff (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientDidClear (event :ClientEvent) :void
    {
        // nada
    }

    /**
     * Called by the lobby controller when we've entered a game lobby.
     */
    public function gameLobbyShown (gameId :int) :void
    {
        if (gameId != _gameId) {
            _gameId = gameId;
            // perform our bookmarkable URL magic
            var scene :Scene = _ctx.getSceneDirector().getScene();
            if (scene != null) {
                wentToScene(scene.getId());
            }
        }
    }

    /**
     * Called by LobbyController to indicate that there is no longer a game lobby visible
     */
    public function gameLobbyCleared (gameId :int) :void
    {
        if (gameId == _gameId) {
            _gameId = -1;
            // perform our bookmarkable URL magic, if we're not minimized
            if (!_minimized) {
                var scene :Scene = _ctx.getSceneDirector().getScene();
                if (scene != null) {
                    wentToScene(scene.getId());
                }
            }
        }
    }

    /** 
     * Called by WorldClient when it finds out if we're embedded in a page or not.
     */
    public function setEmbedded (embedded :Boolean) :void
    {
        _embedded = embedded;
        _ctx.getTopPanel().getControlBar().setEmbedded(embedded);
    }

    /**
     * Find out whether this client is embedded in a non-whirled page.
     */
    public function isEmbedded () :Boolean
    {
        return _embedded;
    }

    /**
     * Lets us know that the Whirled client has either been minimized, or un-minimized
     */
    public function setMinimized (mini :Boolean) :void
    {
        _minimized = mini;
        if (mini) {
            _topPanel.clearSidePanel(null);
        }
    }

    /**
     * Find out if we're currently working in mini-land or not.  Other components should be able 
     * to check this value after they detect that the flash player's size has changed, to discover
     * our status in this regard.
     */
    public function getMinimized () :Boolean
    {
        return _minimized;
    }

    /**
     * Get a scene ID string that should show the current scene in an embedded flash client.
     */
    public function getSceneIdString () :String
    {
        return _sceneIdString;
    }

    /**
     * Return true if we should attempt to load sections of whirled by
     * visiting a new page.
     */
    protected function shouldLoadNewPages () :Boolean
    {
        var pt :String = Capabilities.playerType;
        return !_embedded && (pt !== "StandAlone") && (pt !== "External")
    }

    /**
     * Moves to a new location (scene, game room, etc.) by changing the URL of the browser so that 
     * our history mechanism is preserved. Returns true if we did so, false if we couldn't do so 
     * for whatever reason (are in the standalone client) and the caller should just go there 
     * directly.
     */
    protected function handleInternalGo (page :String, args :String) :Boolean
    {
        return shouldLoadNewPages() && NetUtil.navigateToURL("#" + page + "-" + args, true);
    }

    override protected function setControlledPanel (
        panel :IEventDispatcher) :void
    {
        // in addition to listening for command events, let's listen
        // for LINK events and handle them all here.
        if (_controlledPanel != null) {
            _controlledPanel.removeEventListener(TextEvent.LINK, handleLink);
            _controlledPanel.removeEventListener(KeyboardEvent.KEY_DOWN, handleKeyDown, true);
        }
        super.setControlledPanel(panel);
        if (_controlledPanel != null) {
            _controlledPanel.addEventListener(TextEvent.LINK, handleLink);
            _controlledPanel.addEventListener(KeyboardEvent.KEY_DOWN, handleKeyDown, true);
        }
    }

    /**
     * Handle a TextEvent.LINK event.
     */
    protected function handleLink (evt :TextEvent) :void
    {
        showExternalURL(evt.text);
    }

    /**
     * Handle global key events.
     */
    protected function handleKeyDown (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        // TODO: not F7
        case Keyboard.F7:
            Prefs.setShowingChatHistory(!Prefs.getShowingChatHistory());
            break;
        }

        // We check every keyboard event, see if it's a "word" character,
        // and then if it's not going somewhere reasonable, route it to chat.
        var c :int = event.charCode;
        if (c != 0 && !event.ctrlKey && !event.altKey &&
                // these are the ascii values for '/', a -> z,  A -> Z
                (c == 47 || (c >= 97 && c <= 122) || (c >= 65 && c <= 90))) {
            try {
                var focus :Object = _ctx.getStage().focus;
                if (!(focus is TextField) && !(focus is ChatCantStealFocus)) {
                    ChatControl.grabFocus();
                }
            } catch (err :Error) {
                // TODO: leave this in for now
                trace(err.getStackTrace());
            }
        }
    }

    /** Provides access to client-side directors and services. */
    protected var _ctx :WorldContext;

    /** The topmost panel in the msoy client. */
    protected var _topPanel :TopPanel;

    /** A special logoff message to use when we disconnect. */
    protected var _logoffMessage :String;

    /** The currently loaded game lobby, used for magic URL bookmarkable gamelobbies */
    protected var _gameId :int = -1;

    /** whether or not we're embedded */
    protected var _embedded :Boolean = true;

    /** Whether we're miniaturized or not */
    protected var _minimized :Boolean = false;

    /** A string to give up for embedding your local scene. */
    protected var _sceneIdString :String;
}
}
