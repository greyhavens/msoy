//
// $Id$

package com.threerings.msoy.client {

import flash.events.IEventDispatcher;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.events.TextEvent;
import flash.events.TimerEvent;

import flash.external.ExternalInterface;
import flash.system.Capabilities;
import flash.text.TextField;
import flash.ui.Keyboard;

import flash.net.URLRequest;
import flash.net.navigateToURL; // function import
import flash.utils.Timer;
import flash.utils.getTimer; // function import

import mx.controls.Button;

import com.threerings.util.Controller;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.NetUtil;
import com.threerings.util.StringUtil;
import com.threerings.util.CommandEvent;

import com.threerings.crowd.client.BodyService;
import com.threerings.crowd.data.CrowdCodes;
import com.threerings.crowd.chat.data.ChatCodes;

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
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyCredentials;

import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupMembership;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.SceneBookmarkEntry;

import com.threerings.msoy.chat.client.ChatControl;
import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.chat.client.ReportingListener;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.game.client.AVRGameService;

import com.threerings.msoy.world.client.RoomView;

public class MsoyController extends Controller
    implements ClientObserver
{
    public static const log :Log = Log.getLog(MsoyController);

    /** Command to show the 'about' dialog. */
    public static const ABOUT :String = "About";

    /** Command to log us on. */
    public static const LOGON :String = "Logon";

    /** Command to display the recent scenes list. */
    public static const POP_ROOMS_MENU :String = "PopRoomsMenu";

    /** Command to display the chat channel menu. */
    public static const POP_CHANNEL_MENU :String = "PopChannelMenu";

    /** Command to display the notificaitons popup. */
    public static const POPUP_NOTIFICATIONS :String = "PopupNotifications";

    /** Command to go to a particular place (by Oid). */
    public static const GO_LOCATION :String = "GoLocation";

    /** Command to go to a particular scene. */
    public static const GO_SCENE :String = "GoScene";

    /** Command to go to a member's home scene. */
    public static const GO_MEMBER_HOME :String = "GoMemberHome";

    /** Command to go to a group's home scene. */
    public static const GO_GROUP_HOME :String = "GoGroupHome";

    /** Command to go to a member's current scene. */
    public static const GO_MEMBER_LOCATION :String = "GoMemberLocation";

    /** Command to join a member's currently pending game table. */
    public static const JOIN_PLAYER_TABLE :String = "JoinPlayerTable";

    /** Command to go to a running game (gameId + placeOid). */
    public static const GO_GAME :String = "GoGame";

    /** Command to join a game lobby. */
    public static const JOIN_GAME_LOBBY :String = "JoinGameLobby";

    /** Command to join an in-world game. */
    public static const JOIN_AVR_GAME :String = "JoinAVRGame";

    /** Command to leave the in-world game. */
    public static const LEAVE_AVR_GAME :String = "LeaveAVRGame";

    /** Command to invite someone to be a friend. */
    public static const INVITE_FRIEND :String = "InviteFriend";

    /** Command to open the chat interface for a particular chat channel. */
    public static const OPEN_CHANNEL :String = "OpenChannel";

    /** Command to edit preferences. */
    public static const CHAT_PREFS :String = "ChatPrefs";

    /** Command to view an item, arg is [ itemTypeId, itemId ] */
    public static const VIEW_ITEM :String = "ViewItem";

    /** Command to view a member's profile, arg is [ memberId ] */
    public static const VIEW_MEMBER :String = "ViewMember";

    /** Command to view a groups's page, arg is [ groupId ] */
    public static const VIEW_GROUP :String = "ViewGroup";

    /** Command to view the "my avatars" page. */
    public static const VIEW_MY_AVATARS :String= "ViewMyAvatars";

    /** Command to view the "my avatars" page. */
    public static const VIEW_MY_FURNITURE :String= "ViewMyFurniture";

    /** Command to view the "mail" page. */
    public static const VIEW_MAIL :String= "ViewMail";

    /** Command to view the app in full-screen mode. */
    public static const TOGGLE_FULLSCREEN :String = "ToggleFullscreen";

    /** Command to issue an invite to a current guest. */
    public static const INVITE_GUEST :String = "InviteGuest";

    /**
     * Create the msoy controller.
     */
    public function MsoyController (ctx :WorldContext, topPanel :TopPanel)
    {
        _ctx = ctx;
        _ctx.getClient().addServiceGroup(CrowdCodes.CROWD_GROUP);
        _ctx.getClient().addClientObserver(this);
        _topPanel = topPanel;
        setControlledPanel(ctx.getStage());

        _tipTimer = new Timer(15000, 1);
        _tipTimer.addEventListener(TimerEvent.TIMER, displayChatTip);

        _idleTimer = new Timer(ChatCodes.DEFAULT_IDLE_TIME, 1);
        _idleTimer.addEventListener(TimerEvent.TIMER, function (... ignored) :void {
            setIdle(true)
        });
        restartIdleTimer();
    }

    /**
     * Handles the OPEN_CHANNEL command.
     */
    public function handleOpenChannel (name :Name) :void
    {
        (_ctx.getChatDirector() as MsoyChatDirector).openChannel(name);
    }

    /**
     * Handle the POP_CHANNEL_MENU command.
     */
    public function handlePopChannelMenu (trigger :Button) :void
    {
        var me :MemberObject = _ctx.getMemberObject();
        var friends :Array = new Array();
        for each (var fe :FriendEntry in me.getSortedEstablishedFriends()) {
            if (fe.online) {
                var item :Object = {
                    label: fe.name.toString(), command: OPEN_CHANNEL, arg: fe.name }
                checkChatChannelOpen(fe.name, item);
                friends.push(item);
            }
        }
        if (friends.length == 0) {
            friends.push({ label: Msgs.GENERAL.get("m.no_friends"),
                           enabled: false });
        }

        var groups :Array = (me.groups != null) ? me.groups.toArray() : [];
        groups = groups.map(function (gm :GroupMembership, index :int, array :Array) :Object {
            var item :Object = { label: gm.group.toString(), command: OPEN_CHANNEL, arg: gm.group };
            checkChatChannelOpen(gm.group, item);
            return item;
        });
        if (groups.length == 0) {
            groups.push({ label: Msgs.GENERAL.get("m.no_groups"),
                          enabled : false });
        }

        var menuData :Array = [];
        menuData = menuData.concat(friends);
        menuData.push({ type: "separator" });
        menuData = menuData.concat(groups);
        CommandMenu.createMenu(menuData).popUp(trigger, true, true);
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
        friends = friends.map(function (fe :FriendEntry, index :int, array :Array) :Object {
            return { label: fe.name.toString(), command: GO_MEMBER_HOME, arg: fe.getMemberId()
            };
        });

        var recent :Array = memberObj.recentScenes.toArray();
        recent.sort(function (sb1 :SceneBookmarkEntry, sb2 :SceneBookmarkEntry) :int {
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
            menuData.push({ label: Msgs.GENERAL.get("l.visit_friends"), children: friends });
        }
        // add owned scenes, if any
        if (owned.length > 0) {
            menuData.push({ label: Msgs.GENERAL.get("l.owned_scenes"), children: owned});
        }
        // always add recent scenes
        menuData.push({ label: Msgs.GENERAL.get("l.recent_scenes"), children: recent });

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
     * Handle the POPUP_NOTIFICATIONS command.
     */
    public function handlePopupNotifications (... ignored) :void
    {
        _ctx.getNotificationDirector().displayNotifications();
    }

    /**
     * Handle the ABOUT command.
     */
    public function handleAbout () :void
    {
        new AboutDialog(_ctx);
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
     * Force the user to be idle immediately, and give them the specified number
     * of seconds to move the mouse and such before we automatically de-idle them.
     */
    public function forceIdle (secondsOfLeeway :int) :void
    {
        _idleOverrideStamp = (secondsOfLeeway * 1000) + getTimer();
        setIdle(true);
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
        // TODO: use a proper item info page
        displayPage("inventory", ident.type + "_0_" + ident.itemId);
    }

    /**
     * Handle the VIEW_MEMBER command.
     */
    public function handleViewMember (memberId :int) :void
    {
        displayPage("profile", "" + memberId);
    }

    /**
     * Handle the VIEW_GROUP command.
     */
    public function handleViewGroup (groupId :int) :void
    {
        displayPage("group", "" + groupId);
    }

    /**
     * Handle the VIEW_MY_AVATARS command.
     */
    public function handleViewMyAvatars () :void
    {
        displayPage("inventory", "" + Item.AVATAR);
    }

    /**
     * Handle the VIEW_MY_FURNITURE command.
     */
    public function handleViewMyFurniture () :void
    {
        displayPage("inventory", "" + Item.FURNITURE);
    }

    /**
     * Handle the VIEW_MAIL command.
     */
    public function handleViewMail () :void
    {
        displayPage("mail", "");
    }

    /**
     * Handle the GO_SCENE command.
     */
    public function handleGoScene (sceneId :int) :void
    {
        if (!displayPageGWT("world", "s" + sceneId)) {
            // fall back to breaking the back button
            _ctx.getSceneDirector().moveTo(sceneId);
        }
    }

    /**
     * Handle the GO_MEMBER_HOME command.
     */
    public function handleGoMemberHome (memberId :int, direct :Boolean = false) :void
    {
        _ctx.getWorldDirector().goToMemberHome(memberId);
    }

    /**
     * Handle the GO_MEMBER_LOCATION command.
     */
    public function handleGoMemberLocation (memberId :int) :void
    {
        // pass the buck to the world director
        _ctx.getWorldDirector().goToMemberLocation(memberId);
    }

    /**
     * Handle the JOIN_PLAYER_TABLE command.
     */
    public function handleJoinPlayerTable (memberId :int) :void
    {
        var msvc :MemberService = (_ctx.getClient().requireService(MemberService) as MemberService);
        msvc.getCurrentMemberLocation(_ctx.getClient(), memberId, new ResultWrapper(
            function (cause :String) :void {
                _ctx.displayFeedback(null, cause);
            },
            function (location :MemberLocation) :void {
                if (location.gameId == 0) {
                    _ctx.displayFeedback(MsoyCodes.GAME_MSGS, "e.no_longer_lobbying");
                    return;
                } 
                if (location.sceneId == 0) {
                    // if the game already started, take them straight into it.
                    _ctx.getWorldDirector().goToMemberLocation(location.memberId, location);
                    return;
                }
                _ctx.getGameDirector().joinPlayerTable(location.gameId, location.memberId);
            }));
        restoreSceneURL();
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
        if (!displayPageGWT("world", "l" + placeOid)) {
            // fall back to breaking the back button
            _ctx.getLocationDirector().moveTo(placeOid);
        }
    }

    /**
     * Handle the GO_GAME command to go to a non-Flash game.
     */
    public function handleGoGame (gameId :int, placeOid :int) :void
    {
        // route our entry to the game through GWT so that we can handle non-Flash games
        if (!displayPage("game", gameId + "_" + placeOid)) {
            // fall back to breaking the back button
            log.info("Going straight into game [oid=" + placeOid + "].");
            _ctx.getGameDirector().enterGame(placeOid);
            // TODO: if this is a Java game and we're in embedded mode, try popping up a new
            // browser window
            // NetUtil.navigateToURL("/#game-" + gameId + "_" + placeOid, false);
        } else {
            log.info("Routed game ready through URL [oid=" + placeOid + "].");
        }
    }

    /**
     * Handle JOIN_GAME_LOBBY.
     */
    public function handleJoinGameLobby (gameId :int) :void
    {
        // if we're not running in the GWT app, we need to display a page externally
        if (!inGWTApp() && displayPage("game", "" + gameId)) {
            return;
        }

        // if we're not in a scene, go to our home scene while we're displaying the lobby
        var sceneId :int;
        if (_ctx.getSceneDirector().getScene() == null) {
            sceneId = _ctx.getMemberObject().homeSceneId;
            // default to scene 1 for guests.
            _ctx.getSceneDirector().moveTo(sceneId != 0 ? sceneId : 1);
        } else {
            sceneId = _ctx.getSceneDirector().getScene().getId();
        }

        // now display the lobby interface
        _ctx.getGameDirector().displayLobby(gameId);

        // replace the #game-XXX URL with a #world-sXXX URL for our current scene
        displayPageGWT("world", "s" + sceneId);
    }

    /**
     * Handle JOIN_AVR_GAME.
     */
    public function handleJoinAVRGame (gameId :int) :void
    {
        _ctx.getGameDirector().activateAVRGame(gameId);
    }

    /**
     * Handle LEAVE_AVR_GAME.
     */
    public function handleLeaveAVRGame () :void
    {
        _ctx.getGameDirector().leaveAVRGame();
    }

    /**
     * Handles INVITE_FRIEND.
     */
    public function handleInviteFriend (memberId :int) :void
    {
        _ctx.getMemberDirector().inviteToBeFriend(memberId);
    }

    /**
     * Handles CHAT_PREFS.
     */
    public function handleChatPrefs () :void
    {
        new ChatPrefsDialog(_ctx);
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
            log.info("Logging on [creds=" + creds + ", version=" + DeploymentConfig.version + "].");
            client.setCredentials(creds);
            client.logon();
        });
    }

    /**
     * Handle the INVITE_GUEST command.
     */
    public function handleInviteGuest (guest :MemberName) :void
    {
        var msvc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        var errorHandler :Function = function (cause :String) :void {
            _ctx.displayFeedback(MsoyCodes.GENERAL_MSGS, cause);
        };
        var resultHandler :Function = function (result :Object) :void {
            _ctx.displayFeedback(MsoyCodes.GENERAL_MSGS, MessageBundle.tcompose("m.invite_sent",
                result));
        };
        msvc.issueInvitation(_ctx.getClient(), guest,
                             new ResultWrapper(errorHandler, resultHandler));
    }

    /**
     * Figure out where we should be going, and go there.
     */
    public function goToPlace (params :Object) :void
    {
        // first, see if we should hit a specific scene
        if (null != params["memberHome"]) {
            _sceneIdString = null;
            handleGoMemberHome(int(params["memberHome"]), true);

        } else if (null != params["groupHome"]) {
            _sceneIdString = null;
            handleGoGroupHome(int(params["groupHome"]), true);

        } else if (null != params["memberScene"]) {
            _sceneIdString = null;
            handleGoMemberLocation(int(params["memberScene"]));

        } else if (null != params["playerTable"]) {
            _sceneIdString = null;
            handleJoinPlayerTable(int(params["playerTable"]));

        } else if (null != params["gameLocation"]) {
            _sceneIdString = null;
            _ctx.getGameDirector().enterGame(int(params["gameLocation"]));

        } else if (null != params["noplace"]) {
            // go to no place- we just want to chat with our friends
            _ctx.getTopPanel().setPlaceView(new NoPlaceView(_ctx));

        } else if (null != params["gameLobby"]) {
            handleJoinGameLobby(int(params["gameLobby"]));

        } else if (null != params["worldGame"]) {
            handleJoinAVRGame(int(params["worldGame"]));

        } else if (null != params["groupChat"]) {
            var group :GroupName = new GroupName(null, int(params["groupChat"]));
            // get the real GroupName, complete with text name
            var groupMembership :GroupMembership =
                _ctx.getMemberObject().groups.get(group) as GroupMembership;
            if (groupMembership != null) {
                handleOpenChannel(groupMembership.group);
            }

            // fix the URL
            if (_ctx.getGameDirector().getGameConfig() != null) {
                // For now, leave the URL alone if we're in a game.
            } else if (_ctx.getSceneDirector().getScene() != null) {
                displayPageGWT("world", "s" + _ctx.getSceneDirector().getScene().getId());
            } else {
                displayPageGWT("world", "m" + _ctx.getMemberObject().getMemberId());
            }

        } else if (null != params["sceneId"]) {
            var sceneId :int = int(params["sceneId"]);
            if (sceneId == 0) {
                sceneId = _ctx.getMemberObject().homeSceneId;
                if (sceneId == 0) {
                    sceneId = 1; // for "backwards combatability"
                }
            }
            _ctx.getSceneDirector().moveTo(sceneId);

            // if we have a redirect page we need to show, do that (we do this by hand to avoid
            // potential infinite loops if something goes awry with opening external pages)
            try {
                var redirect :String = params["page"];
                if (redirect != null && ExternalInterface.available) {
                    ExternalInterface.call("displayPage", redirect, "");
                }
            } catch (error :Error) {
                // nothing we can do here...
            }

        } else if (null != params["featuredPlace"]) {
            _ctx.getSceneDirector().moveTo(int(params["featuredPlace"]));

        } else if (_ctx.getMemberObject().getMemberId() != 0) {
            _ctx.getWorldDirector().goToMemberHome(_ctx.getMemberObject().getMemberId());

        } else {
            // this only happens in the standalone client when we have no credentials
            _ctx.getSceneDirector().moveTo(1);
        }
    }

    /**
     * Called by the scene director when we've traveled to a new scene.
     */
    public function wentToScene (sceneId :int) :void
    {
        if (_ctx.getWorldClient().isFeaturedPlaceView()) {
            return;
        }
        // this will result in another request to move to the scene we're already in, but we'll
        // ignore it because we're already there
        var scene :String = sceneId + "";
        if (_sceneIdString != null) {
            displayPageGWT("world", "s" + scene);
        }
        _sceneIdString = scene;
    }

    /**
     * Convienience function to restore our GWT page URL for the current scene.
     */
    public function restoreSceneURL () :void
    {
        if (_ctx.getSceneDirector().getScene() != null) {
            displayPageGWT("world", "s" + _ctx.getSceneDirector().getScene().getId());
        }
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

        if (!_didFirstLogonGo) {
            _didFirstLogonGo = true;
            goToPlace(_topPanel.loaderInfo.parameters);
        }

        if (memberObj.avrGameId > 0) {
            if (_ctx.getGameDirector().getGameId() == 0) {
                _ctx.getGameDirector().activateAVRGame(memberObj.avrGameId);
            }
        }
    }

    // from ClientObserver
    public function clientObjectDidChange (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientDidLogoff (event :ClientEvent) :void
    {
        _topPanel.clearLeftPanel(null);
        _topPanel.clearBottomPanel(null);
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
     * Get a scene ID string that should show the current scene in an embedded flash client.
     */
    public function getSceneIdString () :String
    {
        return _sceneIdString;
    }

    /**
     * Indicate on the menu item whether or not we have the specified chat channel open
     * or not.
     */
    protected function checkChatChannelOpen (name :Name, menuItem :Object) :void
    {
        if ((_ctx.getChatDirector() as MsoyChatDirector).hasOpenChannel(name)) {
            // TODO: use an icon or something instead?
            menuItem["type"] = "check";
            menuItem["toggled"] = true;
        }
    }

    /**
     * Return true if we are running in the GWT application shell, false otherwise.
     */
    protected function inGWTApp () :Boolean
    {
        var pt :String = Capabilities.playerType;
        if (pt == "StandAlone" || pt == "External") {
            return false;
        }
        if (_ctx.getWorldClient().isEmbedded()) {
            return false;
        }
        if (_ctx.getPartner() == "facebook") {
            return false;
        }
        return true;
    }

    /**
     * Calls our GWT application and requests that the specified page be displayed.
     */
    protected function displayPageGWT (page :String, args :String) :Boolean
    {
        if (inGWTApp()) {
            try {
                if (ExternalInterface.available) {
                    ExternalInterface.call("displayPage", page, args);
                    return true;
                }
            } catch (e :Error) {
                log.warning("Unable to display page via Javascript [page=" + page +
                            ", args=" + args +"]: " + e);
            }
        }
        return false;
    }

    /**
     * Displays a new page either in our GWT application or by reloading the current web page with
     * the full GWT application, restoring our current location and then displaying the page.
     */
    protected function displayPage (page :String, args :String) :Boolean
    {
        if (inGWTApp()) {
            return displayPageGWT(page, args);

        } else {
            var fullURL :String;
            var scene :Scene = _ctx.getSceneDirector().getScene();
            if (scene == null) {
                fullURL = "/#" + page + "-" + args;
            } else {
                fullURL = "/#world-s" + scene.getId() + "-" + page + "-" + args;
            }
            log.info("Showing external URL " + fullURL);
            try {
                navigateToURL(new URLRequest(fullURL), "_top");
                return true;
            } catch (e :Error) {
                log.warning("Failed to display URL [url=" + fullURL + "]: " + e);
            }
            return false;
        }
    }

    /**
     * Display a tip of the day in chat.
     */
    protected function displayChatTip (... ignored) :void
    {
        // TODO: ideally, we use MessageBundle.getAll(), but we can't currently
        // get all the keys from a resource bundle...
        try {
            var numTips :int = StringUtil.parseInteger(Msgs.GENERAL.get("n.tip_count"));
            _ctx.displayInfo(MsoyCodes.GENERAL_MSGS, "m.tip_" + int(1 + (Math.random() * numTips)));
        } catch (err :Error) {
            // just omit the tip
        }

        // we are now done with this timer..
        _tipTimer = null;
    }

    override protected function setControlledPanel (panel :IEventDispatcher) :void
    {
        // in addition to listening for command events, let's listen
        // for LINK events and handle them all here.
        if (_controlledPanel != null) {
            _controlledPanel.removeEventListener(TextEvent.LINK, handleLink);
            _controlledPanel.removeEventListener(KeyboardEvent.KEY_DOWN, handleKeyDown, true);
            _controlledPanel.removeEventListener(MouseEvent.MOUSE_MOVE, handleMouseMove);
        }
        super.setControlledPanel(panel);
        if (_controlledPanel != null) {
            _controlledPanel.addEventListener(TextEvent.LINK, handleLink);
            _controlledPanel.addEventListener(KeyboardEvent.KEY_DOWN, handleKeyDown, true);
            _controlledPanel.addEventListener(MouseEvent.MOUSE_MOVE, handleMouseMove);
        }
    }

    /**
     * Handle a TextEvent.LINK event.
     */
    protected function handleLink (evt :TextEvent) :void
    {
        var url :String = evt.text;
        if (StringUtil.startsWith(url, COMMAND_URL)) {
            var cmd :String = url.substring(COMMAND_URL.length);
            var arg :String = null;
            var slash :int = cmd.indexOf("/");
            if (slash != -1) {
                arg = cmd.substring(slash + 1);
                cmd = cmd.substring(0, slash);
            }
            CommandEvent.dispatch(evt.target as IEventDispatcher, cmd, arg);

        } else {
            // A regular URL
            showExternalURL(url);
        }
    }

    /**
     * Handle global key events.
     */
    protected function handleKeyDown (event :KeyboardEvent) :void
    {
        restartIdleTimer();

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

    /**
     * Handle mouse movement on the stage.
     */
    protected function handleMouseMove (event :MouseEvent) :void
    {
        restartIdleTimer();
    }

    /**
     * Called when we've detected user activity, like mouse movement or key presses.
     */
    protected function restartIdleTimer () :void
    {
        // see if we want to honor this request..
        if (_idleOverrideStamp != 0) {
            if (getTimer() < _idleOverrideStamp) {
                return;
            } else {
                _idleOverrideStamp = 0;
            }
        }

        setIdle(false);
        _idleTimer.reset();
        _idleTimer.start();

        if (_tipTimer != null) {
            _tipTimer.reset();
            _tipTimer.start();
        }
    }

    /**
     * Update our idle status.
     */
    protected function setIdle (nowIdle :Boolean) :void
    {
        if (nowIdle != _idle) {
            _idle = nowIdle;
            var bsvc :BodyService = _ctx.getClient().requireService(BodyService) as BodyService;
            bsvc.setIdle(_ctx.getClient(), nowIdle);
        }
    }

    /** Provides access to client-side directors and services. */
    protected var _ctx :WorldContext;

    /** The topmost panel in the msoy client. */
    protected var _topPanel :TopPanel;

    /** Tracks whether we've done our first-logon movement so that we avoid trying to redo it as we
     * subsequently move between servers (and log off and on in the process). */
    protected var _didFirstLogonGo :Boolean;

    /** A special logoff message to use when we disconnect. */
    protected var _logoffMessage :String;

    /** Whether we think we're idle or not. */
    protected var _idle :Boolean = false;

    /** A timer to watch our idleness. */
    protected var _idleTimer :Timer;

    /** A timestamp (from flash.utils.getTimer()) before which we ignore non-idling behavior. */
    protected var _idleOverrideStamp :Number = 0;

    /** A timer to wait for a little bit of idle to pop up a chat tip. */
    protected var _tipTimer :Timer;

    /** A string to give up for embedding your local scene. */
    protected var _sceneIdString :String;

    /** The URL prefix for 'command' URLs, that post CommendEvents. */
    protected static const COMMAND_URL :String = "command://";
}
}
