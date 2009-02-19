//
// $Id$

package com.threerings.msoy.world.client {

import flash.geom.Rectangle;

import flash.external.ExternalInterface;
import flash.system.Capabilities;

import mx.controls.Button;

import com.threerings.util.ArrayUtil;
import com.threerings.util.ConfigValueSetEvent;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.net.Credentials;

import com.threerings.crowd.client.PlaceView;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.flash.media.AudioPlayer;
import com.threerings.flash.media.MediaPlayerCodes;
import com.threerings.flash.media.Mp3AudioPlayer;

import com.threerings.flex.CommandMenu;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.chat.client.IMRegisterDialog;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.item.client.FlagItemDialog;
import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.ItemMarshaller;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.avrg.client.AVRGamePanel;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.client.GameDirector;
import com.threerings.msoy.game.client.GameGameService;
import com.threerings.msoy.game.client.MsoyGamePanel;
import com.threerings.msoy.game.data.MsoyGameConfig;

import com.threerings.msoy.client.BootablePlaceController;
import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.LogonPanel;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.MsoyParameters;
import com.threerings.msoy.client.NoPlaceView;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.UberClient;

import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.WorldCredentials;
import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.data.all.ContactEntry;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GatewayEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.room.client.RoomObjectController;
import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.data.MsoySceneModel;

import com.threerings.msoy.utils.Args;

/**
 * Extends the MsoyController with World specific bits.
 */
public class WorldController extends MsoyController
{
    /** Command to display the chat channel menu. */
    public static const POP_CHANNEL_MENU :String = "PopChannelMenu";

    /** Opens up a new toolbar and a new room editor. */
    public static const ROOM_EDIT :String = "RoomEdit";

    /** Command to rate the current scene. */
    public static const ROOM_RATE :String = "RoomRate";

    /** Command to go to a particular place (by Oid). */
    public static const GO_LOCATION :String = "GoLocation";

    /** Command to go to a particular scene. */
    public static const GO_SCENE :String = "GoScene";

    /** Command to go to a member's home scene. */
    public static const GO_MEMBER_HOME :String = "GoMemberHome";

    /** Command to join a member's current game. */
    public static const JOIN_PLAYER_GAME :String = "JoinPlayerGame";

    /** Command to join a game lobby. */
    public static const JOIN_GAME_LOBBY :String = "JoinGameLobby";

    /** Command to join an in-world game. */
    public static const JOIN_AVR_GAME :String = "JoinAVRGame";

    /** Command to invite someone to be a friend. */
    public static const INVITE_FRIEND :String = "InviteFriend";

    /** Command to open the chat interface for a particular chat channel. */
    public static const OPEN_CHANNEL :String = "OpenChannel";

    /** Command to logon to an im account. */
    public static const REGISTER_IM :String = "RegisterIM";

    /** Command to logoff an im account. */
    public static const UNREGISTER_IM :String = "UnregisterIM";

    /** Command to view a member's profile, arg is [ memberId ] */
    public static const VIEW_MEMBER :String = "ViewMember";

    /** Command to view a game, arg is [ gameId ] */
    public static const VIEW_GAME :String = "ViewGame";

    /** Command to view the trophies awarded by a game, arg is [ gameId ] */
    public static const VIEW_TROPHIES :String = "ViewTrophies";

    /** Command to visit a member's current location */
    public static const VISIT_MEMBER :String = "VisitMember";

    /** Command to view a "stuff" page. Arg: [ itemType ] */
    public static const VIEW_STUFF :String= "ViewStuff";

    /** Command to view a "shop" page. Arg: [ itemType ] */
    public static const VIEW_SHOP :String= "ViewShop";

    /** Command to view the "mail" page. */
    public static const VIEW_MAIL :String= "ViewMail";

    /** Command to issue an invite to a current guest. */
    public static const INVITE_GUEST :String = "InviteGuest";

    /** Command to respond to a request to follow another player. */
    public static const RESPOND_FOLLOW :String = "RespondFollow";

    /** Command to open the account creation UI. */
    public static const CREATE_ACCOUNT :String = "CreateAccount";

    /** Command to complain about a member. */
    public static const COMPLAIN_MEMBER :String = "ComplainMember";

    /** Command to toggle the client to full browser height. */
    public static const TOGGLE_HEIGHT :String = "ToggleHeight";

    /** Command to invoke when the featured place was clicked. */
    public static const FEATURED_PLACE_CLICKED :String = "FeaturedPlaceClicked";

    /** Command to view the passport page. */
    public static const VIEW_PASSPORT :String = "ViewPassport";

    /** Command to display the game menu. */
    public static const POP_GAME_MENU :String = "PopGameMenu";

    /** Command to play music. Arg: null to stop, or [ MediaDesc,  ItemIdent ] */
    public static const PLAY_MUSIC :String = "PlayMusic";

    /** Get info about the currently-playing music. */
    public static const MUSIC_INFO :String = "MusicInfo";

    /** Command to join a party. */
    public static const JOIN_PARTY :String = "JoinParty";

    /** Command to invite a member to the current party. */
    public static const INVITE_TO_PARTY :String = "InviteToParty";

    /** Command to request detailed info on a party. */
    public static const GET_PARTY_DETAIL :String = "GetPartyDetail";

    // statically reference classes we require
    ItemMarshaller;

    public function WorldController (ctx :WorldContext, topPanel :TopPanel)
    {
        super(ctx, topPanel);
        _wctx = ctx;

        Prefs.config.addEventListener(Prefs.BLEEPED_MEDIA, handleBleepChange, false, 0, true);
        Prefs.config.addEventListener(ConfigValueSetEvent.CONFIG_VALUE_SET, handleConfigValueSet,
            false, 0, true);
        _musicPlayer.addEventListener(MediaPlayerCodes.METADATA, handleMusicMetadata);

        try {
            ExternalInterface.addCallback("gwtMediaPlayback", externalPlayback);
        } catch (err :Error) {
            // oh well
        }
    }

    /**
     * Sets (or clears) the current AVRG overlay.
     */
    public function setAVRGamePanel (panel :AVRGamePanel) :void
    {
        var container :PlaceBox = _wctx.getTopPanel().getPlaceContainer();

        if (_avrGamePanel) {
            if (_avrGamePanel == panel) {
                return;
            }
            container.removeOverlay(_avrGamePanel);
            _avrGamePanel = null;
        }
        if (panel) {
            container.addOverlay(panel, PlaceBox.LAYER_AVRG_PANEL);
            _avrGamePanel = panel;
        }
    }

    /**
     * Handles the OPEN_CHANNEL command.
     */
    public function handleOpenChannel (name :Name) :void
    {
        _wctx.getMsoyChatDirector().openChannel(name);
    }

    /**
     * Handles the REGISTER_IM command.
     */
    public function handleRegisterIM (gateway :String) :void
    {
        _topPanel.callLater(function () :void { new IMRegisterDialog(_wctx, gateway); });
    }

    /**
     * Handles the UNREGISTER_IM command;
     */
    public function handleUnregisterIM (gateway :String) :void
    {
        _wctx.getMsoyChatDirector().unregisterIM(gateway);
    }

    /**
     * Handles the POP_CHANNEL_MENU command.
     */
    public function handlePopChannelMenu (trigger :Button) :void
    {
        // if we don't yet have a member object, it's too early to pop!
        const me :MemberObject = _wctx.getMemberObject();
        if (me == null) {
            return;
        }

        var menuData :Array = [];
        menuData.push({ label: Msgs.GENERAL.get("b.chatPrefs"), command: CHAT_PREFS });
        menuData.push({ label: Msgs.GENERAL.get("b.clearChat"),
            callback: _wctx.getChatDirector().clearDisplays });
        menuData.push({ type: "separator" });

        const place :PlaceView = _wctx.getPlaceView();
        const allowHistoryToggle :Boolean = !(place is MsoyGamePanel) ||
            (place as MsoyGamePanel).shouldUseChatOverlay();
        var addedSomething :Boolean = false;
        if (allowHistoryToggle) {
            menuData.push({ command: TOGGLE_CHAT_HIDE, label: Msgs.GENERAL.get(
                    Prefs.getShowingChatHistory() ? "b.hide_chat" : "b.show_chat") });
            addedSomething = true;
        }
        if (!(place is MsoyGamePanel)) {
            menuData.push({ command: TOGGLE_CHAT_SIDEBAR, label: Msgs.GENERAL.get(
                    Prefs.getSidebarChat() ? "b.overlay_chat" : "b.sidebar_chat") });
            menuData.push({ command: TOGGLE_OCC_LIST, label: Msgs.GENERAL.get(
                    Prefs.getShowingOccupantList() ? "b.hide_occ_list" : "b.show_occ_list") });
            addedSomething = true;
        }
        if (addedSomething) {
            menuData.push({ type: "separator" });
        }

        // slap your friends in a menu
        var friends :Array = new Array();
        for each (var fe :FriendEntry in me.getSortedOnlineFriends()) {
            var item :Object = {
                label: fe.name.toString(), command: OPEN_CHANNEL, arg: fe.name }
            checkChatChannelOpen(fe.name, item);
            friends.push(item);
        }
        if (friends.length == 0) {
            friends.push({ label: Msgs.GENERAL.get("m.no_friends"),
                        enabled: false });
        }
        menuData.push({ label: Msgs.GENERAL.get("l.friends"), children: friends });

        var groups :Array = (me.groups != null) ? me.getSortedGroups() : [];
        groups = groups.map(function (gm :GroupMembership, index :int, array :Array) :Object {
            var item :Object = { label: gm.group.toString(), command: OPEN_CHANNEL, arg: gm.group };
            checkChatChannelOpen(gm.group, item);
            return item;
        });
        if (groups.length == 0) {
            groups.push({ label: Msgs.GENERAL.get("m.no_groups"),
                          enabled : false });
        } else if (groups.length > 4) {
            menuData.push({ label: Msgs.GENERAL.get("l.groups"), children: groups});
        } else {
            menuData = menuData.concat(groups);
        }

        var gateways :Array = me.getSortedGateways();
        if (gateways.length > 0) {
            menuData.push({ type: "separator"});
        }
        for each (var ge :GatewayEntry in gateways) {
            var subMenuData :Array = [];
            if (!ge.online) {
                subMenuData.push(
                    { label: Msgs.CHAT.get("m.im_login"), command: REGISTER_IM, arg: ge.gateway });
            } else {
                var contacts :Array = me.getSortedImContacts(ge.gateway);
                for each (var ce :ContactEntry in contacts) {
                    if (!ce.online) {
                        continue;
                    }
                    var aitem :Object = {
                        label: ce.name.toString(), command: OPEN_CHANNEL, arg: ce.name }
                    checkChatChannelOpen(ce.name, aitem);
                    subMenuData.push(aitem);
                }
                if (contacts.length == 0) {
                    subMenuData.push({ label: Msgs.CHAT.get("m.no_im_contacts"), enabled: false});
                }
                subMenuData.push({ type: "separator"});
                subMenuData.push({
                    label:Msgs.CHAT.get("m.im_logout"), command: UNREGISTER_IM, arg: ge.gateway });
            }
            menuData.push({ label: Msgs.CHAT.get("m." + ge.gateway), children: subMenuData});
        }

        var r :Rectangle = trigger.getBounds(trigger.stage);
        var menu :CommandMenu = CommandMenu.createMenu(menuData.reverse(), _topPanel);
        menu.variableRowHeight = true;
        menu.setBounds(_wctx.getTopPanel().getMainAreaBounds());
        menu.popUpAt(r.left, r.top, true);
    }

    /**
     * Handles the VIEW_COMMENTED_ITEM command.
     */
    public function handleViewCommentedItem (itemType :int, itemId :int) :void
    {
        // in this case we are looking for an item that we were told was commented, so we can
        // assume that it's listed in the shop
        displayPage("shop", "l_" + itemType + "_" + itemId);
    }

    /**
     * Handles the VIEW_ITEM command.
     */
    public function handleViewItem (ident :ItemIdent) :void
    {
        var resultHandler :Function = function (result :Object) :void {
            if (result == null) {
                // it's an object we own, or it's not listed but we are support+
                displayPage("stuff", "d_" + ident.type + "_" + ident.itemId);

            } else if (result == 0) {
                _wctx.displayFeedback(MsoyCodes.ITEM_MSGS,
                    MessageBundle.compose("m.not_listed", Item.getTypeKey(ident.type)));

            } else {
                displayPage("shop", "l_" + ident.type + "_" + result);
            }
        };
        var isvc :ItemService = _wctx.getClient().requireService(ItemService) as ItemService;
        isvc.getCatalogId(_wctx.getClient(), ident, _wctx.resultListener(resultHandler));
    }

    /**
     * Handles the FLAG_ITEM command.
     */
    public function handleFlagItem (ident :ItemIdent) :void
    {
        new FlagItemDialog(_wctx, ident);
    }

    /**
     * Handles the VIEW_MEMBER command.
     */
    public function handleViewMember (memberId :int) :void
    {
        displayPage("people", "" + memberId);
    }

    /**
     * Handles hte VISIT_MEMBER command.
     */
    public function handleVisitMember (memberId :int) :void
    {
        _wctx.getWorldDirector().goToMemberLocation(memberId);
    }

    /**
     * Handles the VIEW_GROUP command.
     */
    public function handleViewGroup (groupId :int) :void
    {
        displayPage("groups", "d_" + groupId);
    }

    /**
     * Handles the VIEW_ROOM command.
     */
    public function handleViewRoom (sceneId :int) :void
    {
        displayPage("rooms", "room_" + sceneId);
    }

    /**
     * Handles the VIEW_COMMENT_PAGE command.
     */
    public function handleViewCommentPage () :void
    {
        const sceneId :int = getCurrentSceneId();
        if (sceneId != 0) {
            handleViewRoom(sceneId);
            return;
        }
    }

    /**
     * Handles the VIEW_FULL_VERSION command, used in embedded clients.
     */
    public function handleViewFullVersion () :void
    {
        // log that the full version button was clicked
        _wctx.getMsoyClient().trackClientAction("flashFullVersionClicked", null);

        // then go to the appropriate place..
        const sceneId :int = getCurrentSceneId();
        if (sceneId != 0) {
            displayPage("world", "s" + sceneId);

        } else {
            const gameId :int = getCurrentGameId();
            if (gameId != 0) {
                displayPage("games", "d_" + gameId);

            } else {
                displayPage("", "");
            }
        }
    }

    /**
     * Handles the FEATURED_PLACE_CLICKED command.
     */
    public function handleFeaturedPlaceClicked () :void
    {
        if (_wctx.getMsoyClient().isEmbedded()) {
            handleViewFullVersion();
        } else {
            var sceneId :int = getCurrentSceneId();
            if (sceneId == 0) {
                // TODO: before falling back to the initial scene, we should try
                // any pending scene...
                sceneId = int(MsoyParameters.get()["sceneId"]);
            }
            handleGoScene(sceneId);
        }
    }

    /**
     * Handles the VIEW_GAME command.
     */
    public function handleViewGame (gameId :int) :void
    {
// (MDB) for now we're always sending players right into the lobby; we can link to the detail from
// there if we like
//
//         // when a player clicks a game in the whirled, we try to display that game's detail page,
//         // but if we can't do that, then fall back to displaying the game lobby
//         if (!inGWTApp() || !displayPage("games", "d_" + gameId)) {
            handleJoinGameLobby(gameId);
//         }
    }

    /**
     * Handles the VIEW_TROPHIES command.
     */
    public function handleViewTrophies (gameId :int) :void
    {
        displayPage("games", "d_" + gameId + "_t");
    }

    /**
     * Handles the VIEW_PASSPORT command.
     */
    public function handleViewPassport () :void
    {
        displayPage("me", "passport");
    }

    /**
     * Handles the VIEW_GAMES command.
     */
    override public function handleViewGames () :void
    {
        // log that the view games event was fired
        _wctx.getMsoyClient().trackClientAction("flashViewGames", null);
        displayPage("games", "");
    }

    /**
     * Handles the VIEW_STUFF command.
     */
    public function handleViewStuff (itemType :int) :void
    {
        displayPage("stuff", ""+itemType);
    }

    /**
     * Handles the VIEW_SHOP command.
     */
    public function handleViewShop (itemType :int) :void
    {
        displayPage("shop", ""+itemType);
    }

    /**
     * Handles the VIEW_MAIL command.
     */
    public function handleViewMail () :void
    {
        displayPage("mail", "");
    }

    /**
     * Handles the SHOW_SIGN_UP command.
     */
    public function handleShowSignUp () :void
    {
        displayPage("account", "create");
    }

    /**
     * Handles the GO_SCENE command.
     */
    public function handleGoScene (sceneId :int) :void
    {
        if (!displayPageGWT("world", "s" + sceneId)) {
            // fall back to breaking the back button
            log.info("Can't go to scene via GWT. Going direct to " + sceneId + ".");
            _wctx.getSceneDirector().moveTo(sceneId);
        }
    }

    /**
     * Handles the GO_MEMBER_HOME command.
     */
    public function handleGoMemberHome (memberId :int) :void
    {
        _wctx.getWorldDirector().goToMemberHome(memberId);
    }

    /**
     * Handles the JOIN_PLAYER_GAME command. (Generated by chat-based invites.)
     */
    public function handleJoinPlayerGame (gameId :int, playerId :int) :void
    {
        _wctx.getGameDirector().joinPlayer(gameId, playerId);
    }

    /**
     * Handles the JOIN_PARTY command.
     */
    public function handleJoinParty (partyId :int) :void
    {
        _wctx.getPartyDirector().joinParty(partyId);
    }

    /**
     * Handles the GET_PARTY_DETAIL command.
     */
    public function handleGetPartyDetail (partyId :int) :void
    {
        _wctx.getPartyDirector().getPartyDetail(partyId);
    }

    /**
     * Handles the GO_GROUP_HOME command.
     */
    public function handleGoGroupHome (groupId :int) :void
    {
        _wctx.getWorldDirector().goToGroupHome(groupId);
    }

    /**
     * Handles the GO_LOCATION command to go to a placeobject.
     */
    public function handleGoLocation (placeOid :int) :void
    {
        if (!displayPageGWT("world", "l" + placeOid)) {
            // fall back to breaking the back button
            _wctx.getLocationDirector().moveTo(placeOid);
        }
    }

    /**
     * Handles the GO_GAME command to go to a game.
     */
    public function handleGoGame (
        gameId :int, placeOid :int, inviterMemberId :int, inviteToken :String) :void
    {
        // route our entry to the game through GWT so that we can handle non-Flash games
        log.debug("Handling go game", "oid", placeOid, "inviterMemberId", inviterMemberId,
           "inviteToken", inviteToken);
        var args :String = Args.join("game", "g", gameId, placeOid);
        if ((inviteToken != null && inviteToken.length > 0) || inviterMemberId != 0) {
            args = Args.join(args, inviteToken, inviterMemberId);
        }
        if (!inGWTApp() || !displayPage("world", args)) {
            // fall back to breaking the back button
            log.info("Going straight into game [oid=" + placeOid + "].");
            _wctx.getGameDirector().enterGame(gameId, placeOid, inviterMemberId, inviteToken);
            // TODO: if this is a Java game and we're in embedded mode, try popping up a new
            // browser window
            // NetUtil.navigateToURL("/#game-" + gameId + "_" + placeOid, false);
        } else {
            log.info("Routed game ready through URL", "oid", placeOid, "args", args);
        }
    }

    /**
     * Handles JOIN_GAME_LOBBY (and gameLobby=XX).
     */
    public function handleJoinGameLobby (gameId :int, ghost :String = null, gport :int = 0) :void
    {
        // TODO: support "inviteToken" and "inviterMemberId" here?
        // if we're running in the GWT app, we need to route through GWT to keep the URL valid
        if (inGWTApp() && displayPage("world", "game_l_" + gameId)) {
            log.info("Routed join lobby through URL", "game", gameId, "ghost", ghost,
                "gport", gport);

        } else {
            // otherwise, display the lobby interface directly
            _wctx.getGameDirector().displayLobby(gameId, ghost, gport);
        }
    }

    /**
     * Handles JOIN_AVR_GAME.
     */
    public function handleJoinAVRGame (gameId :int, token :String = "", 
        inviterMemberId :int = 0) :void
    {
        _wctx.getGameDirector().activateAVRGame(gameId, token == null ? "" : token, inviterMemberId);
    }

    /**
     * Handles INVITE_FRIEND.
     */
    public function handleInviteFriend (memberId :int) :void
    {
        _wctx.getMemberDirector().inviteToBeFriend(memberId);
    }

    /**
     * Handles RESPOND_FOLLOW.
     */
    public function handleRespondFollow (memberId :int) :void
    {
        var msvc :MemberService = _wctx.getClient().requireService(MemberService) as MemberService;
        msvc.followMember(_wctx.getClient(), memberId, _wctx.listener());
    }

    /**
     * Handle the ROOM_EDIT command.
     */
    public function handleRoomEdit () :void
    {
        (_topPanel.getPlaceView() as RoomObjectView).getRoomObjectController().handleRoomEdit();
    }

    /**
     * Handle the ROOM_RATE command.
     */
    public function handleRoomRate (rating :Number) :void
    {
        (_topPanel.getPlaceView() as RoomObjectView).getRoomObjectController().
                handleRoomRate(rating);
    }

    /**
     * Handles the CREATE_ACCOUNT command (generated by the InviteOverlay).
     */
    public function handleCreateAccount (invite :String = null) :void
    {
        // log that the create account event was fired
        _wctx.getMsoyClient().trackClientAction("flashCreateAccount", null);
        displayPage("account", (invite == null) ? "create" : ("create_" + invite));
    }

    /**
     * Handles the COMPLAIN_MEMBER command.
     */
    public function handleComplainMember (memberId :int, username :String) :void
    {
        var service :Function;
        var gctx :GameContext = _wctx.getGameDirector().getGameContext();
        // if we're playing a game, but it's not an AVRG, this complaint goes to the game server
        if (gctx != null && _wctx.getGameDirector().getAVRGameBackend() == null) {
            service = function (complaint :String) :void {
                var gsvc :GameGameService =
                    (gctx.getClient().requireService(GameGameService) as GameGameService);
                gsvc.complainPlayer(gctx.getClient(), memberId, complaint);
            };

        } else {
            service = function (complaint :String) :void {
                var msvc :MemberService =
                    (_wctx.getClient().requireService(MemberService) as MemberService);
                msvc.complainMember(_wctx.getClient(), memberId, complaint);
            };
        }

        _topPanel.callLater(function () :void { new ComplainDialog(_wctx, username, service); });
    }

    /**
     * Handles booting a user.
     */
    public function handleBootFromPlace (memberId :int) :void
    {
        var svc :MemberService = _wctx.getClient().requireService(MemberService) as MemberService;
        svc.bootFromPlace(_wctx.getClient(), memberId, _wctx.confirmListener());
    }

    /**
     * Handles the TOGGLE_HEIGHT command.
     */
    public function handleToggleHeight () :void
    {
        if (inGWTApp()) {
            try {
                if (ExternalInterface.available) {
                    ExternalInterface.call("toggleClientHeight");
                    return;
                }
            } catch (e :Error) {
                log.warning("Unable to handleToggleHeight via Javascript: " + e);
            }
        } else {
            log.warning("Can't access GWT to handleToggleHeight");
        }
    }

    /**
     * Handles PLAY_MUSIC.
     */
    public function handlePlayMusic (music :MediaDesc, ident :ItemIdent) :void
    {
        _musicDesc = music;
        _musicIdent = ident;
        _musicInfoShown = false;

        // TODO: fade out music if no new, unless the current music is bleeped
        _musicPlayer.unload();

        const play :Boolean = UberClient.isRegularClient() && (music != null) &&
            (Prefs.getSoundVolume() > 0) && !Prefs.isMediaBlocked(music.getMediaId());
        if (play) {
            _musicPlayer.load(music.getMediaPath());
        }
        WorldControlBar(_wctx.getControlBar()).setMusicPlaying(music != null);
    }

    /**
     * Handles MUSIC_INFO.
     */
    public function handleMusicInfo () :void
    {
        handleViewItem(_musicIdent);
    }

    /**
     * Handles INVITE_TO_PARTY.
     */
    public function handleInviteToParty (memberId :int) :void
    {
        _wctx.getPartyDirector().inviteMember(memberId);
    }

    /**
     * Access the music player. Don't be too nefarious now boys!
     */
    public function getMusicPlayer () :AudioPlayer
    {
        return _musicPlayer;
    }

    /**
     * Handles the POP_GAME_MENU command.
     */
    public function handlePopGameMenu (trigger :Button) :void
    {
        var menuData :Array = [];
        if (!_wctx.getGameDirector().populateGameMenu(menuData)) {
            return;
        }

        var r :Rectangle = trigger.getBounds(trigger.stage);
        var menu :CommandMenu = CommandMenu.createMenu(menuData, _topPanel);
        menu.variableRowHeight = true;
        menu.setBounds(_wctx.getTopPanel().getMainAreaBounds());
        menu.popUpAt(r.left, r.top, true);
    }

    /**
     * Displays a new page either in our GWT application or by reloading the current web page with
     * the full GWT application, restoring our current location and then displaying the page.
     */
    public function displayPage (page :String, args :String) :Boolean
    {
        if (inGWTApp()) {
            return displayPageGWT(page, args);

        } else {
            const tracking :String = _wctx.getMemberObject().visitorInfo.getTrackingArgs();
            const pageToken :String = StringUtil.isBlank(page) ? tracking
                                                               : (page + "-" + args + tracking);
            const url :String = createPageLink(pageToken, true);
            log.info("Showing external URL " + url);
            return super.handleViewUrl(url, null);
        }
    }

    /**
     * Returns the current sceneId, or 0 if none.
     */
    public function getCurrentSceneId () :int
    {
        const scene :Scene = _wctx.getSceneDirector().getScene();
        return (scene == null) ? 0 : scene.getId();
    }

    /**
     * Returns the game id, or 0 if none.
     */
    public function getCurrentGameId () :int
    {
        return _wctx.getGameDirector().getGameId();
    }

    /**
     * Called by the scene director when we've traveled to a new scene.
     */
    public function wentToScene (sceneId :int) :void
    {
        if (UberClient.isFeaturedPlaceView()) {
            return;
        }
        // this will result in another request to move to the scene we're already in, but we'll
        // ignore it because we're already there
        if (!_suppressTokenForScene) {
            displayPageGWT("world", "s" + sceneId);
        }
        _suppressTokenForScene = false;
    }

    /**
     * Convienience function to restore our GWT page URL for the current scene.
     */
    public function restoreSceneURL () :void
    {
        const sceneId :int = getCurrentSceneId();
        if (sceneId != 0) {
            displayPageGWT("world", "s" + sceneId);
        }
    }

    /**
     * If we're joining a game lobby and have not yet logged onto the world server, we start the
     * game lobby connection process immediately instead of waiting until we're connected to the
     * world server. This short-circuits the normal logon-go process.
     */
    public function preLogonGo (params :Object) :void
    {
        _didFirstLogonGo = true;
        goToPlace(params);
    }

    /**
     * Updates our availability state.
     */
    public function updateAvailability (availability :int) :void
    {
        var msvc :MemberService = _wctx.getClient().requireService(MemberService) as MemberService;
        msvc.updateAvailability(_wctx.getClient(), availability);
        _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS, "m.avail_tip_" + availability);
    }

    /**
     * Figure out where we should be going, and go there.
     */
    public function goToPlace (params :Object) :void
    {
        // first, see if we should hit a specific scene
        if (null != params["memberHome"]) {
            _suppressTokenForScene = true;
            handleGoMemberHome(int(params["memberHome"]));

        } else if (null != params["groupHome"]) {
            _suppressTokenForScene = true;
            handleGoGroupHome(int(params["groupHome"]));

        } else if (null != params["memberScene"]) {
            _suppressTokenForScene = true;
            handleVisitMember(int(params["memberScene"]));

        } else if (null != params["playerTable"]) {
            _suppressTokenForScene = true;
            _wctx.getGameDirector().joinPlayerTable(
                int(params["gameLobby"]), int(params["playerTable"]),
                String(params["ghost"]), int(params["gport"]));

        } else if (null != params["gameOid"]) {
            _suppressTokenForScene = true;
            var token :String = params["inviteToken"] != null ? String(params["inviteToken"]) : "";
            _wctx.getGameDirector().enterGame(int(params["gameId"]), int(params["gameOid"]),
                int(params["inviterMemberId"]), token);

        } else if (null != params["noplace"]) {
            // go to no place- we just want to chat with our friends
            _wctx.setPlaceView(new NoPlaceView());

        } else if (null != params["gameLobby"]) {
            // TODO: support "inviteToken" and "inviterMemberId" here?
            _wctx.getGameDirector().displayLobby(
                int(params["gameLobby"]), String(params["ghost"]), int(params["gport"]));

        } else if (null != params["playNow"]) {
            _wctx.getGameDirector().playNow(int(params["playNow"]), params["gameMode"] as String,
                                            String(params["ghost"]), int(params["gport"]),
                                            params["inviteToken"] as String, 
                                            int(params["inviterMemberId"]));

        } else if (null != params["worldGame"]) {
            handleJoinAVRGame(int(params["worldGame"]), params["inviteToken"] as String,
                int(params["inviterMemberId"]));

        } else if ("true" == params["tour"]) {
            _wctx.getTourDirector().startTour();

        } else if (null != params["groupChat"]) {
            var groupId :int = int(params["groupChat"]);
            var gm :GroupMembership =
                _wctx.getMemberObject().groups.get(groupId) as GroupMembership;
            if (gm != null) {
                handleOpenChannel(gm.group);
            }

            // fix the URL
            if (_wctx.getGameDirector().getGameConfig() != null) {
                // For now, leave the URL alone if we're in a game.
            } else if (_wctx.getSceneDirector().getScene() != null) {
                displayPageGWT("world", "s" + _wctx.getSceneDirector().getScene().getId());
            } else {
                displayPageGWT("world", "m" + _wctx.getMemberObject().getMemberId());
            }

        } else if (null != params["sceneId"]) {
            var sceneId :int = int(params["sceneId"]);
            if (sceneId == 0) {
                sceneId = _wctx.getMemberObject().getHomeSceneId();
            }
            _wctx.getSceneDirector().moveTo(sceneId);

            // if we have a redirect page we need to show, do that (we do this by hand to avoid
            // potential infinite loops if something goes awry with opening external pages)
            try {
                var redirect :String = params["page"];
                if (redirect != null && ExternalInterface.available) {
                	var args :String = params["args"] == null ? "" : params["args"];
                    ExternalInterface.call("displayPage", redirect, args);
                }
            } catch (error :Error) {
                // nothing we can do here...
            }

        } else {
            // display the My Whirled Places grid (and also fall through and go home)
            //if (null != params["myplaces"]) {
            //    var ctrlBar :WorldControlBar = (_wctx.getControlBar() as WorldControlBar);
            //    if (ctrlBar != null && ctrlBar.homePageGridBtn.enabled) {
            //        if (_wctx.getMemberObject().hasOnlineFriends()) { // also excludes guests
            //            ctrlBar.friendsBtn.activate();
            //        }
            //        if (Prefs.getGridAutoshow()) {
            //            ctrlBar.homePageGridBtn.activate();
            //        }
            //    }
            //}

            // go to our home scene (this doe the right thing for guests as well)
            _wctx.getSceneDirector().moveTo(_wctx.getMemberObject().getHomeSceneId());
        }
    }

    // from MsoyController
    override public function handleViewUrl (url :String, windowOrTab :String = null) :Boolean
    {
        // if our page refers to a Whirled page...
        var gwtPrefix :String = DeploymentConfig.serverURL + "#";
        var gwtUrl :String;
        if (url.indexOf(gwtPrefix) == 0) {
            gwtUrl = url.substring(gwtPrefix.length);
        } else if (url.indexOf("#") == 0) {
            gwtUrl = url.substring(1);
        } else {
            return super.handleViewUrl(url, windowOrTab);
        }

        // ...extract the page and arguments and tell GWT to display them properly
        var didx :int = gwtUrl.indexOf("-");
        if (didx == -1) {
            return displayPage(gwtUrl, "");
        } else {
            return displayPage(gwtUrl.substring(0, didx), gwtUrl.substring(didx+1));
        }
    }

    // from MsoyController
    override public function getPlaceInfo () :Array
    {
        if (getCurrentGameId() != 0) {
            return [ true, _wctx.getGameDirector().getGameName(), getCurrentGameId() ];
        } else {
            const scene :Scene = _wctx.getSceneDirector().getScene();
            if (scene != null) {
                return [ false, scene.getName(), scene.getId() ];
            } else {
                return [ false, null, 0 ];
            }
        }
    }

    // from MsoyController
    override public function canManagePlace () :Boolean
    {
        // support can manage any place...
        if (_wctx.getTokens().isSupport()) {
            return true;
        }

        const view :Object = _topPanel.getPlaceView();
        if (view is RoomView) {
            return RoomView(view).getRoomController().canManageRoom();
        }
        const gameCfg :MsoyGameConfig = _wctx.getGameDirector().getGameConfig();
        if (gameCfg != null) {
            // in games, we can "manage" if we're the owner
            return gameCfg.game.creatorId == _wctx.getMyName().getMemberId();
        }
        return false;
    }

    // from MsoyController
    override public function addMemberMenuItems (
        member :MemberName, menuItems :Array,
        addPlaceItems :Boolean = false, addAvatarItems :Boolean = false) :void
    {
        const memId :int = member.getMemberId();
        const us :MemberObject = _wctx.getMemberObject();
        const isUs :Boolean = (memId == us.getMemberId());
        const isGuest :Boolean = MemberName.isGuest(memId);
        var placeCtrl :Object = null;
        if (addPlaceItems) {
            placeCtrl = _wctx.getLocationDirector().getPlaceController();
            if (placeCtrl == null) {
                // check the gamecontext's place
                placeCtrl = _wctx.getGameDirector().getGameController();
            }
        }

        // if we're not a guest, populate availability menu.
        if (isUs && !isGuest) {
            var availActions :Array = [];
            for (var ii :int = MemberObject.AVAILABLE; ii <= MemberObject.UNAVAILABLE; ii++) {
                availActions.push({
                    label: Msgs.GENERAL.get("l.avail_" + ii), callback: updateAvailability, arg: ii,
                    enabled: (ii != us.availability) });
            }
            menuItems.push({ label: Msgs.GENERAL.get("l.avail_menu"), children: availActions });

        } else if (!isUs) {
            if (_mctx.getMuteDirector().isMuted(member)) {
                menuItems.push({ label: Msgs.GENERAL.get("b.unmute"),
                    callback: _mctx.getMuteDirector().setMuted, arg: [ member, false ] });
            } else {
                menuItems.push({ label: Msgs.GENERAL.get("b.open_channel"),
                                 command: OPEN_CHANNEL, arg: member });
                menuItems.push({ label: Msgs.GENERAL.get("b.mute"),
                        callback: _mctx.getMuteDirector().setMuted, arg: [ member, true ] });
            }
            if (!isGuest) {
                menuItems.push({ label: Msgs.GENERAL.get("b.view_member"),
                                 command: VIEW_MEMBER, arg: memId });
                menuItems.push({ label: Msgs.GENERAL.get("b.visit_home"),
                                 command: GO_MEMBER_HOME, arg: memId });
                if (!us.isGuest() && !us.friends.containsKey(memId)) {
                    menuItems.push({ label: Msgs.GENERAL.get("l.add_as_friend"),
                                     command: INVITE_FRIEND, arg: memId });
                }
            }
            menuItems.push({ label: Msgs.GENERAL.get("b.complain"),
                             command: COMPLAIN_MEMBER, arg: [memId, member] });

            // possibly add a menu item for booting this user
            if ((placeCtrl is BootablePlaceController) &&
                    BootablePlaceController(placeCtrl).canBoot()) {
                menuItems.push({ label: Msgs.GENERAL.get("b.boot"),
                    callback: handleBootFromPlace, arg: memId });
            }

            if (_wctx.getPartyDirector().canInviteToParty()) {
                menuItems.push({ label: Msgs.PARTY.get("b.invite_member"),
                    command: INVITE_TO_PARTY, arg: memId,
                    enabled: !_wctx.getPartyDirector().partyContainsPlayer(memId) });
            }
        }

        if (addAvatarItems && (placeCtrl is RoomObjectController)) {
            RoomObjectController(placeCtrl).addAvatarMenuItems(member, menuItems);
        }

        if (isUs && _wctx.getMsoyClient().isEmbedded()) {
            if (isGuest) {
                menuItems.push({ label: Msgs.GENERAL.get("b.logon"),
                    callback: function () :void {
                        (new LogonPanel(_wctx)).open();
                    }});
            } else {
                var creds :WorldCredentials = new WorldCredentials(null, null);
                creds.ident = "";
                menuItems.push({ label: Msgs.GENERAL.get("b.logout"),
                                 command: MsoyController.LOGON, arg: creds });
            }
        }
    }

    // from MsoyController
    override public function addFriendMenuItems (member :MemberName, menuItems :Array) :void
    {
        var memId :int = member.getMemberId();
        menuItems.push({ label: Msgs.GENERAL.get("b.open_channel"),
                         command: OPEN_CHANNEL, arg: member });
        menuItems.push({ label: Msgs.GENERAL.get("b.view_member"),
                         command: VIEW_MEMBER, arg: memId });
        menuItems.push({ label: Msgs.GENERAL.get("b.visit_member"),
                         command: VISIT_MEMBER, arg: memId });
        if (_wctx.getPartyDirector().canInviteToParty()) {
            menuItems.push({ label: Msgs.PARTY.get("b.invite_member"),
                command: INVITE_TO_PARTY, arg: memId,
                enabled: !_wctx.getPartyDirector().partyContainsPlayer(memId) });
        }
    }

    // from MsoyController
    override public function handleClosePlaceView () :void
    {
        if (_wctx.getPlaceView() is MsoyGamePanel) {
            // if we're in a game, closing means closing the game and going back to our place
            handleMoveBack(true);
        } else {
            // if we're in the whirled, closing means closing the flash client totally
            _wctx.getMsoyClient().closeClient();
        }
    }

    // from MsoyController
    override public function handleMoveBack (closeInsteadOfHome :Boolean = false) :void
    {
        // go to the first recent scene that's not the one we're in
        const curSceneId :int = getCurrentSceneId();
        for each (var entry :Object in _recentScenes) {
            if (entry.id != curSceneId) {
                handleGoScene(entry.id);
                return;
            }
        }

        // there are no recent scenes, so either close the client or go home
        if (closeInsteadOfHome && inGWTApp()) {
            _wctx.getWorldClient().closeClient();
        } else {
            handleGoScene(_wctx.getMemberObject().getHomeSceneId());
        }
    }

    // from MsoyController
    override public function canMoveBack () :Boolean
    {
        // you can only NOT move back if you are in your home room and there are no
        // other scenes in your history
        const curSceneId :int = getCurrentSceneId();
        if (_wctx.getMemberObject().getHomeSceneId() != curSceneId) {
            return true;
        }
        for each (var entry :Object in _recentScenes) {
            if (entry.id != curSceneId) {
                return true;
            }
        }
        return false;
    }

    // from MsoyController
    override public function handleLogon (creds :Credentials) :void
    {
        // if we're currently logged on, save our current scene so that we can go back there once
        // we're relogged on as a non-guest; otherwise go to Brave New Whirled
        const currentSceneId :int = getCurrentSceneId();
        _postLogonScene = (currentSceneId == 0) ? 1 : currentSceneId;
        _wctx.getClient().logoff(false);

        super.handleLogon(creds);
    }

    // from ClientObserver
    override public function clientDidLogon (event :ClientEvent) :void
    {
        super.clientDidLogon(event);

        var memberObj :MemberObject = _wctx.getMemberObject();
        // if not a guest, save the username that we logged in with
        if (!memberObj.isGuest()) {
            var name :Name = _wctx.getClient().getCredentials().getUsername();
            if (name != null) {
                Prefs.setUsername(name.toString());
            }
            _wctx.getTopPanel().getHeaderBar().getChatTabs().memberObjectUpdated(memberObj);

        } else {
            // if we are a guest, let the GWT application know the guest id as whom we're
            // authenticated so that it can pass that guest id along to the server if we register
            // and the server can transfer any flow we earn as this guest to our new account
            _wctx.getMsoyClient().gotGuestIdToGWT(memberObj.getMemberId());
        }

        if (!_didFirstLogonGo) {
            _didFirstLogonGo = true;
            goToPlace(MsoyParameters.get());
        } else if (_postLogonScene != 0) {
            // we gotta go somewhere
            _wctx.getSceneDirector().moveTo(_postLogonScene);
            _postLogonScene = 0;
        }

        _wctx.getGameDirector().checkMemberAVRGame();
    }

    // from ClientObserver
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);
        if (!_wctx.getClient().isSwitchingServers()) {
            setAVRGamePanel(null);
        }
    }

    /**
     * Inform our parent web page that our display name has changed.
     */
    public function refreshDisplayName () :void
    {
        if (ExternalInterface.available) {
            ExternalInterface.call("refreshDisplayName");
        }
    }

    override protected function setIdle (nowIdle :Boolean) :void
    {
        const wasGameIdle :Boolean = (_idle || _away);

        super.setIdle(nowIdle);

        // only change game idleness when it truly changes
        if (wasGameIdle != (_idle || _away)) {
            // let AVRGs know about our idleness changes
            var gd :GameDirector = _wctx.getGameDirector();
            if (gd != null) { // studio has no GameDirector
                // game idleness = whirled idle or whirled away
                gd.setIdle(_idle || _away);
            }
        }
    }

    override public function setAway (nowAway :Boolean, message :String = null) :void
    {
        const wasGameIdle :Boolean = (_idle || _away);

        super.setAway(nowAway, message);

        // only change game idleness when it truly changes
        if (wasGameIdle != (_idle || _away)) {
            // let AVRGs know about our idleness changes
            var gd :GameDirector = _wctx.getGameDirector();
            if (gd != null) { // studio has no GameDirector
                // game idleness = whirled idle or whirled away
                gd.setIdle(_idle || _away);
            }
        }
    }

    /**
     * Indicate on the menu item whether or not we have the specified chat channel open
     * or not.
     */
    protected function checkChatChannelOpen (name :Name, menuItem :Object) :void
    {
        menuItem["enabled"] = !_wctx.getMsoyChatDirector().hasOpenChannel(name);
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

    // from MsoyController
    override protected function updateTopPanel (headerBar :HeaderBar, controlBar :ControlBar) :void
    {
        super.updateTopPanel(headerBar, controlBar);

        // TODO: The way I think we should consider doing this is have PlaceView's dispatch
        // some sort of NewPlaceEvent when they're showing and have downloaded whatever data
        // needed, and then various components up the hierarchy can react to this event.

        // if we moved to a scene, set things up thusly
        var scene :Scene = _wctx.getSceneDirector().getScene();
        if (scene != null) {
            _wctx.getMsoyClient().setWindowTitle(scene.getName());
            headerBar.setLocationName(scene.getName());

            // update the owner link
            var model :MsoySceneModel = scene.getSceneModel() as MsoySceneModel;
            if (model != null) {
                if (model.ownerName != null) {
                    headerBar.setOwnerLink(model.ownerName.toString(),
                        (model.ownerType == MsoySceneModel.OWNER_TYPE_MEMBER) ? handleViewMember
                                                                              : handleViewGroup,
                        model.ownerId);
                } else {
                    headerBar.setOwnerLink("");
                }
            }

            addRecentScene(scene);
            return;
        }

        // if we're in a game, display the game name and activate the back button
        var cfg :MsoyGameConfig = _wctx.getGameDirector().getGameConfig();
        if (cfg != null) {
            _wctx.getMsoyClient().setWindowTitle(cfg.game.name);
            headerBar.setLocationName(cfg.game.name);
            headerBar.setOwnerLink("");
        }
    }

    protected function addRecentScene (scene :Scene) :void
    {
        const id :int = scene.getId();

        // first, see if it's already in the list of recent scenes, and remove it if so
        for (var ii :int = _recentScenes.length - 1; ii >= 0; ii--) {
            if (_recentScenes[ii].id == id) {
                _recentScenes.splice(ii, 1);
                break;
            }
        }

        // now add it to the beginning of the list
        _recentScenes.unshift({ name: scene.getName(), id: id });

        // and make sure we're not tracking too many
        _recentScenes.length = Math.min(_recentScenes.length, MAX_RECENT_SCENES);
    }

    override protected function populateGoMenu (menuData :Array) :void
    {
        super.populateGoMenu(menuData);

        const curSceneId :int = getCurrentSceneId();
        var sceneSubmenu :Array = [];
        for each (var entry :Object in _recentScenes) {
            sceneSubmenu.push({ label: StringUtil.truncate(entry.name, 50, "..."),
                command: GO_SCENE, arg: entry.id, enabled: (entry.id != curSceneId) });
        }
        if (sceneSubmenu.length == 0) {
            sceneSubmenu.push({ label: Msgs.GENERAL.get("m.none"), enabled: false });
        }
        menuData.push({ label: Msgs.WORLD.get("l.recent_scenes"), children: sceneSubmenu });

        const me :MemberObject = _wctx.getMemberObject();
        const ourHomeId :int = me.homeSceneId;
        if (ourHomeId != 0) {
            menuData.push({ label: Msgs.GENERAL.get("b.go_home"), command: GO_SCENE, arg: ourHomeId,
                enabled: (ourHomeId != curSceneId) });
        }

        var friends :Array = new Array();
        for each (var fe :FriendEntry in me.getSortedOnlineFriends()) {
            friends.push({ label: fe.name.toString(),
                command: VISIT_MEMBER, arg: fe.name.getMemberId() });
        }
        if (friends.length == 0) {
            friends.push({ label: Msgs.GENERAL.get("m.no_friends"), enabled: false });
        }
        menuData.push({ label: Msgs.GENERAL.get("l.visit_friends"), children: friends });
    }

    protected function handleBleepChange (event :ValueEvent) :void
    {
        if (_musicDesc == null) {
            return; // couldn't possibly concern us..
        }
        const isBleeped :Boolean = Prefs.isMediaBlocked(_musicDesc.getMediaId());
        if (isBleeped == musicIsPlayingOrPaused()) {
            // just call play again with the same music, it'll handle it
            handlePlayMusic(_musicDesc, _musicIdent);
        }
    }

    protected function handleConfigValueSet (event :ConfigValueSetEvent) :void
    {
        // if the volume got turned up and we were not playing music, play it now.
        if ((event.name == Prefs.VOLUME) && (event.value > 0) && (_musicDesc != null) &&
               !musicIsPlayingOrPaused()) {
            handlePlayMusic(_musicDesc, _musicIdent);
        }
    }

    protected function musicIsPlayingOrPaused () :Boolean
    {
        switch (_musicPlayer.getState()) {
        default: return false;
        case MediaPlayerCodes.STATE_PLAYING: // fall through
        case MediaPlayerCodes.STATE_PAUSED: return true;
        }
    }

    protected function handleMusicMetadata (event :ValueEvent) :void
    {
        if (_musicInfoShown) {
            return;
        }
        var id3 :Object = event.value;
        var artist :String = String(id3.artist);
        var songName :String = String(id3.songName);
        if (!StringUtil.isBlank(artist) && !StringUtil.isBlank(songName)) {
            _wctx.getNotificationDirector().notifyMusic(songName, artist);
            _musicInfoShown = true;
        }
    }

    /**
     * Called when we start or stop playing music in GWT.
     */
    protected function externalPlayback (started :Boolean) :void
    {
        if (started) {
            if (_musicPlayer.getState() == MediaPlayerCodes.STATE_PLAYING) {
                _musicPausedForGwt = true;
                _musicPlayer.pause();
            }

        } else if (_musicPausedForGwt) {
            _musicPausedForGwt = false;
            _musicPlayer.play();
        }
    }

    /** Giver of life, context. */
    protected var _wctx :WorldContext;

    /** The player of music. */
    protected var _musicPlayer :Mp3AudioPlayer = new Mp3AudioPlayer(true /*loop*/);

    /** The currently playing music. */
    protected var _musicDesc :MediaDesc;

    /** ItemIdent of the currently playing music. */
    protected var _musicIdent :ItemIdent;

    /** Have we displayed music info in a notification? */
    protected var _musicInfoShown :Boolean;

    /** Have we paused the music so that we can play some media in gwt? */
    protected var _musicPausedForGwt :Boolean;

    /** Tracks whether we've done our first-logon movement so that we avoid trying to redo it as we
     * subsequently move between servers (and log off and on in the process). */
    protected var _didFirstLogonGo :Boolean;

    /** A scene to which to go after we logon. */
    protected var _postLogonScene :int;

    /** Set to true when we're displaying a page that has an alias, like "world-m1". */
    protected var _suppressTokenForScene :Boolean = true; // also, we suppress the first one

    /** The current AVRG display, if any. */
    protected var _avrGamePanel :AVRGamePanel;

    /** Recently visited scenes, ordered from most-recent to least-recent */
    protected var _recentScenes :Array = [];

    /** The maximum number of recent scenes we track. */
    protected static const MAX_RECENT_SCENES :int = 11;

    private static const log :Log = Log.getLog(WorldController);
}
}
