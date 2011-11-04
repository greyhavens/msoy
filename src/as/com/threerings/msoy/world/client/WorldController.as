//
// $Id$

package com.threerings.msoy.world.client {

import flash.external.ExternalInterface;
import flash.geom.Point;

import mx.controls.Button;

import com.threerings.util.DelayUtil;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.NamedValueEvent;
import com.threerings.util.StringUtil;
import com.threerings.util.Util;
import com.threerings.util.ValueEvent;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.net.Credentials;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.media.AudioPlayer;
import com.threerings.media.MediaPlayerCodes;
import com.threerings.media.Mp3AudioPlayer;

import com.threerings.whirled.data.Scene;

import com.threerings.orth.data.MediaDesc;
import com.threerings.orth.data.MediaDescSize;
import com.threerings.orth.ui.MediaWrapper;

import com.threerings.flex.CommandMenu;

import com.threerings.msoy.avrg.client.AVRGamePanel;
import com.threerings.msoy.chat.client.IMRegisterDialog;
import com.threerings.msoy.client.BootablePlaceController;
import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.DeploymentConfig;
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
import com.threerings.msoy.data.Address;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.PlaceInfo;
import com.threerings.msoy.data.WorldCredentials;
import com.threerings.msoy.data.all.ContactEntry;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GatewayEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.client.GameDirector;
import com.threerings.msoy.game.client.GameGameService;
import com.threerings.msoy.game.client.ParlorGamePanel;
import com.threerings.msoy.game.client.TablesWaitingPanel;
import com.threerings.msoy.game.data.ParlorGameConfig;
import com.threerings.msoy.group.data.all.GroupMembership;
import com.threerings.msoy.item.client.FlagItemDialog;
import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.ItemMarshaller;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.room.client.RoomMusicDialog;
import com.threerings.msoy.room.client.RoomObjectController;
import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.client.snapshot.SnapshotPanel;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.PetName;
import com.threerings.msoy.room.data.PuppetName;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.ui.ColorPickerPanel;
import com.threerings.msoy.ui.skins.CommentButton;
import com.threerings.msoy.utils.Args;

/**
 * Extends the MsoyController with World specific bits.
 */
public class WorldController extends MsoyController
{
    // Testing constant, for the time being...
    public static const FUCK_THE_URL :Boolean = false;

    /** Command to display the chat channel menu. */
    public static const POP_CHANNEL_MENU :String = "PopChannelMenu";

    /** Command to display the room menu. */
    public static const POP_ROOM_MENU :String = "PopRoomMenu";

    /** Command to display the music dialog. */
    public static const SHOW_MUSIC :String = "ShowMusic";

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

    /** Command to play a game. */
    public static const PLAY_GAME :String = "PlayGame";

    /** Command to show a lobby. PLAY_GAME should be preferred in most cases. */
    public static const SHOW_LOBBY :String = "ShowLobby";

    /** Command to join a member's current game. */
    public static const JOIN_PLAYER_GAME :String = "JoinPlayerGame";

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

    /** Command to view the trophies awarded by a game, arg is [ gameId ] */
    public static const VIEW_TROPHIES :String = "ViewTrophies";

    /** Command to visit a member's current location */
    public static const VISIT_MEMBER :String = "VisitMember";

    /** Command to view a "stuff" page. Arg: [ itemType ] */
    public static const VIEW_STUFF :String = "ViewStuff";

    /** Command to view a "shop" page.
     * Args: nothing to view the general shop,
     * or [ itemType ] to view a category
     * or [ itemType, itemId ] to view a specific listing. */
    public static const VIEW_SHOP :String = "ViewShop";

    /** Command to view the "mail" page. */
    public static const VIEW_MAIL :String = "ViewMail";

    /** Command to view details for a specific game. Arg: [ gameId ] */
    public static const VIEW_GAME :String = "ViewGame";

    /** Command to view comments for a specific game. Arg: [ gameId ] */
    public static const VIEW_GAME_COMMENTS :String = "ViewGameComments";

    /** Command to issue an invite to a current guest. */
    public static const INVITE_GUEST :String = "InviteGuest";

    /** Command to respond to a request to follow another player. */
    public static const RESPOND_FOLLOW :String = "RespondFollow";

    /** Command to open the account creation UI. */
    public static const CREATE_ACCOUNT :String = "CreateAccount";

    /** Command to complain about a member. */
    public static const COMPLAIN_MEMBER :String = "ComplainMember";

    /** Command to invoke when the featured place was clicked. */
    public static const FEATURED_PLACE_CLICKED :String = "FeaturedPlaceClicked";

    /** Command to view the passport page. */
    public static const VIEW_PASSPORT :String = "ViewPassport";

    /** Command to display the game menu. */
    public static const POP_GAME_MENU :String = "PopGameMenu";

    /** Command to play music. Arg: null to stop, or [ Audio, playCounter (int) ] */
    public static const PLAY_MUSIC :String = "PlayMusic";

    /** Get info about the currently-playing music. */
    public static const MUSIC_INFO :String = "MusicInfo";

    /** Command to join a party. */
    public static const JOIN_PARTY :String = "JoinParty";

    /** Command to invite a member to the current party. */
    public static const INVITE_TO_PARTY :String = "InviteToParty";

    /** Command to request detailed info on a party. */
    public static const GET_PARTY_DETAIL :String = "GetPartyDetail";

    /** Command to start the whirled tour. */
    public static const START_TOUR :String = "StartTour";

    // statically reference classes we require
    ItemMarshaller;

    public function WorldController (ctx :WorldContext, topPanel :TopPanel)
    {
        super(ctx, topPanel);
        _wctx = ctx;

        Prefs.events.addEventListener(Prefs.BLEEPED_MEDIA, handleBleepChange, false, 0, true);
        Prefs.events.addEventListener(Prefs.PREF_SET, handleConfigValueSet, false, 0, true);
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
            callback: _wctx.getMsoyChatDirector().clearAllDisplays });
        CommandMenu.addSeparator(menuData);

        const place :PlaceView = _wctx.getPlaceView();
        const allowHistoryToggle :Boolean = !(place is ParlorGamePanel) ||
            (place as ParlorGamePanel).shouldUseChatOverlay();
        if (allowHistoryToggle) {
            menuData.push({ command: TOGGLE_CHAT_HIDE, label: Msgs.GENERAL.get(
                    Prefs.getShowingChatHistory() ? "b.hide_chat" : "b.show_chat") });
        }
        menuData.push({ command: TOGGLE_CHAT_SIDEBAR, label: Msgs.GENERAL.get(
            Prefs.getSidebarChat() ? "b.overlay_chat" : "b.sidebar_chat") });
        menuData.push({ command: TOGGLE_OCC_LIST, label: Msgs.GENERAL.get(place is ParlorGamePanel ?
            (Prefs.getShowingOccupantList() ? "b.hide_player_list" : "b.show_player_list") :
            (Prefs.getShowingOccupantList() ? "b.hide_occ_list" : "b.show_occ_list")) });

        CommandMenu.addSeparator(menuData);

        var groups :Array = (me.groups != null) ? me.getSortedGroups() : [];
        groups = groups.map(function (gm :GroupMembership, index :int, array :Array) :Object {
            return { label: gm.group.toString(), command: OPEN_CHANNEL, arg: gm.group };
        });
        if (groups.length == 0) {
            menuData.push({ label: Msgs.GENERAL.get("m.no_groups"),
                          enabled : false });
        } else if (groups.length > 4) {
            menuData.push({ label: Msgs.GENERAL.get("l.groups"), children: groups});
        } else {
            menuData = menuData.concat(groups);
        }

        var gateways :Array = me.getSortedGateways();
        if (gateways.length > 0) {
            CommandMenu.addSeparator(menuData);
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
                    // TODO: does this need to be disabled if it's already open? Prob not
                    subMenuData.push(
                        { label: ce.name.toString(), command: OPEN_CHANNEL, arg: ce.name });
                }
                if (contacts.length == 0) {
                    subMenuData.push({ label: Msgs.CHAT.get("m.no_im_contacts"), enabled: false});
                }
                CommandMenu.addSeparator(subMenuData);
                subMenuData.push({
                    label:Msgs.CHAT.get("m.im_logout"), command: UNREGISTER_IM, arg: ge.gateway });
            }
            menuData.push({ label: Msgs.CHAT.get("m." + ge.gateway), children: subMenuData});
        }

        popControlBarMenu(menuData.reverse(), trigger);
    }

    /**
     * Handles the POP_ROOM_MENU command.
     */
    public function handlePopRoomMenu (trigger :Button) :void
    {
        var menuData :Array = [];

        var roomView :RoomView = _wctx.getPlaceView() as RoomView;

        CommandMenu.addTitle(menuData, roomView.getPlaceName());
        var scene :MsoyScene = _wctx.getSceneDirector().getScene() as MsoyScene;
        if (scene != null) {
            var model :MsoySceneModel = scene.getSceneModel() as MsoySceneModel;
//            if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
//                menuData.push({ label: Msgs.GENERAL.get("b.group_page"),
//                    command: MsoyController.VIEW_GROUP, arg: model.ownerId });
//            }
            if (model.gameId != 0) {
                menuData.push({ label: Msgs.GENERAL.get("b.group_game"),
                    command: WorldController.PLAY_GAME, arg: model.gameId });
            }
        }

        CommandMenu.addSeparator(menuData);
        menuData.push({ label: Msgs.GENERAL.get("b.editScene"), icon: ROOM_EDIT_ICON,
            command: ROOM_EDIT, enabled: roomView.getRoomController().canManageRoom() });

        addFrameColorOption(menuData);

        menuData.push({ label: Msgs.GENERAL.get("b.viewItems"),
            callback: roomView.viewRoomItems });
        menuData.push({ label: Msgs.GENERAL.get("b.comment"), icon: CommentButton,
            command: MsoyController.VIEW_COMMENT_PAGE });
        menuData.push({ label: Msgs.GENERAL.get("b.snapshot"), icon: SNAPSHOT_ICON,
            command: doSnapshot });
        menuData.push({ label: Msgs.GENERAL.get("b.music"), icon: MUSIC_ICON,
            command: DelayUtil.delayFrame, arg: [ doShowMusic, [ trigger ] ],
            enabled: (_music != null) }); // pop it later so that it avoids the menu itself

        popControlBarMenu(menuData, trigger);
    }

    public function handleShowMusic (trigger :Button) :void
    {
        doShowMusic(trigger);
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
        isvc.getCatalogId(ident, _wctx.resultListener(resultHandler));
    }

    /**
     * Handles the VIEW_GAME command.
     */
    public function handleViewGame (gameId :int) :void
    {
        displayPage("games", "d_" + gameId);
    }

    /**
     * Handles the VIEW_GAME_COMMENTS command.
     */
    public function handleViewGameComments (gameId :int) :void
    {
        displayPage("games", "d_" + gameId + "_c");
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
     * Handles the VIEW_DISCUSSIONS command.
     */
    public function handleViewDiscussions (groupId :int) :void
    {
        displayPage("groups", "f_" + groupId);
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
    public function handleViewShop (itemType :int = Item.NOT_A_TYPE, itemId :int = 0) :void
    {
        var page :String = "";
        if (itemType != Item.NOT_A_TYPE) {
            page += (itemId == 0) ? itemType : ("l_" + itemType + "_" + itemId);
        }
        displayPage("shop", page);
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
// commented out Ray 2009-08-20       if (!displayPageGWT("world", "s" + sceneId)) {
        // just go straight there, the url will be updated when we arrive
        _wctx.getSceneDirector().moveTo(sceneId);
    }

    /**
     * Handles the GO_MEMBER_HOME command.
     */
    public function handleGoMemberHome (memberId :int) :void
    {
        _wctx.getWorldDirector().goToMemberHome(memberId);
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
        if (FUCK_THE_URL || !displayPageGWT("world", "l" + placeOid)) {
            // fall back to breaking the back button
            _wctx.getLocationDirector().moveTo(placeOid);
        }
    }

    /**
     * Handles PLAY_GAME.
     */
    public function handlePlayGame (gameId :int, ghost :String = null, gport :int = 0) :void
    {
        // if we're running in the GWT app, we need to route through GWT to keep the URL valid
        if (!FUCK_THE_URL && inGWTApp() && displayPage("world", "game_p_" + gameId)) {
            log.info("Doing play game via URL", "game", gameId, "ghost", ghost, "gport", gport);
        } else {
            // otherwise, play the game directly
            _wctx.getGameDirector().playNow(gameId, 0, ghost, gport);
        }
    }

    /**
     * Handles SHOW_LOBBY.
     */
    public function handleShowLobby (gameId :int, ghost :String = null, gport :int = 0) :void
    {
        // we do not try to generate a URL for this behavior. PLAY_GAME is preferred, and
        // this command is rarely used.
        _wctx.getGameDirector().enterLobby(gameId, ghost, gport);
    }

    /**
     * Handles the JOIN_PLAYER_GAME command. (Generated by chat-based invites.)
     */
    public function handleJoinPlayerGame (gameId :int, playerId :int) :void
    {
        // if we're running in the GWT app, we need to route through GWT to keep the URL valid
        if (!FUCK_THE_URL && inGWTApp() &&
                displayPage("world", "game_p_" + gameId + "_" + playerId)) {
            log.info("Doing join player via URL", "game", gameId, "player", playerId);
        } else {
            // otherwise, play the game directly
            _wctx.getGameDirector().playNow(gameId, playerId);
        }
    }

    /**
     * Handles JOIN_AVR_GAME.
     */
    public function handleJoinAVRGame (
        gameId :int, sceneId :int = int.MIN_VALUE,
        token :String = "", inviterMemberId :int = 0) :void
    {
        // 0 means "no scene"
        // MIN_VALUE means "user's home, or current scene"
        if (sceneId == int.MIN_VALUE) {
            var curScene :int = getCurrentSceneId();
            sceneId = (curScene != 0) ? curScene : _wctx.getMemberObject().getHomeSceneId();
        }

        // either we don't want a scene, or we're already in the right one, carry on
        if (sceneId == getCurrentSceneId()) {
            _wctx.getGameDirector().activateAVRGame(
                gameId, StringUtil.deNull(token), inviterMemberId);
            if (sceneId != 0 && inGWTApp() && !FUCK_THE_URL) {
                // the AVRG is not really a URL, so tell the browser to display the scene
                // (do it later so the WorldClient javascript method can exit first)
                // BUG: This causes problems on IE8 if our GWT app is in an iframe. Instead of
                // quitely changing the URL, it reloads the frame after a short interval, causing
                // the AVRG to exit. I suspect it is due to GWT 1.7's history implementation not
                // being updated to remove IE7 hacks and/or include new IE8 hacks. Only facebook-
                // integrated games that are not roomless seem to be affected. Since there are
                // currently none of those, I'm leaving it in for the moment because it still
                // provides a benefit for on-site AVRGs.
                // TODO: find a minimum test case and submit to GWT issues or maybe just shoot self
                DelayUtil.delayFrame(displayPage, ["world", "s" + sceneId]);
            }
            return;
        }

        // observe location changes
        var adapter :LocationAdapter;
        // retry this function on location change
        var didChange :Function = function (place :PlaceObject) :void {
            _wctx.getLocationDirector().removeLocationObserver(adapter);
            handleJoinAVRGame(gameId, sceneId, token, inviterMemberId);
        };
        // remove the observer on failure
        var changeFailed :Function = function (placeId :int, reason :String) :void {
            _wctx.getLocationDirector().removeLocationObserver(adapter);
        }
        adapter = new LocationAdapter(null, didChange, changeFailed);
        _wctx.getLocationDirector().addLocationObserver(adapter);

        // now, move to the scene
        _wctx.getSceneDirector().moveTo(sceneId);
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
     * Arg can be 0 to stop us from following anyone
     */
    public function handleRespondFollow (memberId :int) :void
    {
        WorldService(_wctx.getClient().requireService(WorldService)).
            followMember(memberId, _wctx.listener());
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
                gsvc.complainPlayer(memberId, complaint);
            };

        } else {
            service = function (complaint :String) :void {
                msvc().complainMember(memberId, complaint);
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
        svc.bootFromPlace(memberId, _wctx.confirmListener());
    }

    /**
     * Handles PLAY_MUSIC.
     */
    public function handlePlayMusic (music :Audio) :void
    {
        if (!Util.equals(music, _music)) {
            _musicInfoShown = false;
        }
        _music = music;

        _musicPlayer.unload();

        const play :Boolean = UberClient.isRegularClient() && (music != null) &&
            (Prefs.getSoundVolume() > 0) && !isMusicBleeped();
        if (play) {
            _musicPlayer.load(music.audioMedia.getMediaPath(),
                [ music.audioMedia, music.getIdent() ]);
        }
        if (music == null && _musicDialog != null) {
            _musicDialog.close();
        }
    }

    /**
     * Handles MUSIC_INFO.
     */
    public function handleMusicInfo () :void
    {
        handleViewItem(_music.getIdent());
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
        if (_wctx.getGameDirector().populateGameMenu(menuData)) {
            if (!_wctx.getGameDirector().isAVRGame()) {
                addFrameColorOption(menuData);
            }
            popControlBarMenu(menuData, trigger);
        }
    }

    /**
     * Handles the POP_PET_MENU command.
     */
    public function handlePopPetMenu (name :String, petId :int, ownerId :int) :void
    {
        var menuItems :Array = [];
        addPetMenuItems(new PetName(name, petId, ownerId), menuItems);
        CommandMenu.createMenu(menuItems, _mctx.getTopPanel()).popUpAtMouse();
    }

    /**
     * Handles SUBSCRIBE.
     */
    public function handleSubscribe () :void
    {
        displayPage("billing", "subscribe");
    }

    /**
     * Displays a new page either in our GWT application or by reloading the current web page with
     * the full GWT application, restoring our current location and then displaying the page.
     */
    public function displayPage (page :String, args :String) :Boolean
    {
        if (inGWTApp()) {
            return displayPageGWT(page, args);
        }

        // otherwise we're embedded and we need to route through the swizzle servlet to stuff our
        // session token into a cookie which will magically authenticate us with GWT
        const ptoken :String = page + (StringUtil.isBlank(args) ? "" : ("-" + args));
        const stoken :String = (_wctx.getClient().getCredentials() as MsoyCredentials).sessionToken;
        const url :String = DeploymentConfig.serverURL + "swizzle/" + stoken + "/" + ptoken;
        log.info("Showing external URL " + url);
        return super.handleViewUrl(url, null);
    }

    /**
     * Displays a new page at a given address either in our GWT application or by reloading the
     * current web page with the full GWT application, restoring our current location and then
     * displaying the page.
     */
    public function displayAddress (address :Address) :Boolean
    {
        if (address.page != null) {
            return displayPage(address.page.path, Args.join.apply(null, address.args));
        } else {
            return handleViewUrl(address.args.join("/"));
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
        if (FUCK_THE_URL || UberClient.isFeaturedPlaceView()) {
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
     * Figure out where we should be going, and go there.
     */
    public function goToPlace (params :Object) :void
    {
        // first, see if we should hit a specific scene
        if (null != params["memberHome"]) {
            _suppressTokenForScene = true;
            var memberId :int = int(params["memberHome"]);
            if (memberId == 0) {
                // let's take this as a signal that we're after our own home room
                memberId = _wctx.getMemberObject().getMemberId();
            }
            handleGoMemberHome(memberId);

        } else if (null != params["groupHome"]) {
            _suppressTokenForScene = true;
            handleGoGroupHome(int(params["groupHome"]));

        } else if (null != params["memberScene"]) {
            _suppressTokenForScene = true;
            handleVisitMember(int(params["memberScene"]));

        } else if (null != params["gameOid"]) {
            // note: this check *must* come before the gameId != null check below
            _suppressTokenForScene = true;
            _wctx.getGameDirector().enterGame(int(params["gameId"]), int(params["gameOid"]));

        } else if (null != params["gameId"] || null != params["gameLobby"] /*legacy*/) {
            _suppressTokenForScene = true;
            var gameId :int = int(params["gameId"]) || int(params["gameLobby"] /* legacy */);
            _wctx.getGameDirector().playNow(gameId, int(params["playerId"]),
                params["ghost"] as String, int(params["gport"]),
                params["inviteToken"] as String, int(params["inviterMemberId"]));

        } else if (null != params["noplace"]) {
            // go to no place- we just want to chat with our friends
            _wctx.setPlaceView(new NoPlaceView());

        } else if (null != params["worldGame"]) {
            handleJoinAVRGame(int(params["worldGame"]), int(params["gameRoomId"]),
                              params["inviteToken"] as String, int(params["inviterMemberId"]));

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
                displayPageGWT("world", "m" + _wctx.getMyId());
            }

        } else if (null != params["sceneId"]) {
            var sceneId :int = int(params["sceneId"]);
            if (sceneId == 0) {
                log.warning("Moving to scene 0, I hope that's what we actually want.",
                    "raw arg", params["sceneId"]);
                //sceneId = _wctx.getMemberObject().getHomeSceneId();
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
            // Note this only happens when we are in a specific A/B test group and get here via
            // the landing page. If the user reloads the client from some other url, the home page
            // grid will no longer be accessible. Se la vi.
            if (null != params["myplaces"]) {
                var ctrlBar :WorldControlBar = (_wctx.getControlBar() as WorldControlBar);
                if (ctrlBar != null) {
                    ctrlBar.showHomePageGrid();
                }
            }

            // go to our home scene (this doe the right thing for guests as well)
            _wctx.getSceneDirector().moveTo(_wctx.getMemberObject().getHomeSceneId());
        }
    }

    /**
     * Handles the START_TOUR command.
     */
    public function handleStartTour () :void
    {
        _wctx.getTourDirector().startTour();
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

    /**
     * Show (or hide) the tables waiting display.
     */
    public function showTablesWaiting (show :Boolean = true) :void
    {
        if (show) {
            if (_tablesPanel == null) {
                _tablesPanel = new TablesWaitingPanel(_wctx);
                _tablesPanel.addCloseCallback(function () :void {
                    _tablesPanel = null;
                });
                _tablesPanel.open();
            } else {
                _tablesPanel.refresh();
            }

        } else if (_tablesPanel != null) {
            _tablesPanel.close();
        }
    }

    // from MsoyController
    override public function getPlaceInfo () :PlaceInfo
    {
        var plinfo :PlaceInfo = new PlaceInfo();

        var scene :Scene = _wctx.getSceneDirector().getScene();
        plinfo.sceneId = (scene == null) ? 0 : scene.getId();
        plinfo.sceneName = (scene == null) ? null : scene.getName();

        var gdir :GameDirector = _wctx.getGameDirector();
        plinfo.gameId = gdir.getGameId();
        plinfo.gameName = gdir.getGameName();
        plinfo.avrGame = gdir.isAVRGame();

        return plinfo;
    }

    // from MsoyController
    override public function canManagePlace () :Boolean
    {
        // TODO: this function should really be split into canManageRoom and canManageGame because
        // of AVRGs being both. ShareDialog currently allows a room manager to embed out an AVRG
        // because of this, but it's not like the feature is that popular

        // support can manage any place...
        if (_wctx.getTokens().isSupport()) {
            return true;
        }

        const view :Object = _topPanel.getPlaceView();
        if (view is RoomView) {
            return RoomView(view).getRoomController().canManageRoom();
        }

        const gameCfg :ParlorGameConfig = _wctx.getGameDirector().getGameConfig();
        if (gameCfg != null) {
            // in games, we can "manage" if we're the owner
            return gameCfg.game.creatorId == _wctx.getMyId();
        }

        return false;
    }

    // from MsoyController
    override public function addMemberMenuItems (
        name :MemberName, menuItems :Array, addWorldItems :Boolean = true) :void
    {
        const memId :int = name.getId();
        const us :MemberObject = _wctx.getMemberObject();
        const isUs :Boolean = (memId == us.getMemberId());
        const isMuted :Boolean = !isUs && _wctx.getMuteDirector().isMuted(name);
        const isPuppet :Boolean = (name is PuppetName);
        const isSupport :Boolean = _wctx.getTokens().isSupport();
        var placeCtrl :Object = null;
        if (addWorldItems) {
            placeCtrl = _wctx.getLocationDirector().getPlaceController();
            if (placeCtrl == null) {
                // check the gamecontext's place
                placeCtrl = _wctx.getGameDirector().getGameController();
            }
        }

        var followItem :Object = null;
        if (addWorldItems && !isPuppet) {
            var followItems :Array = [];
            if (isUs) {
                // if we have followers, add a menu item for clearing them
                if (us.followers.size() > 0) {
                    followItems.push({ label: Msgs.GENERAL.get("b.clear_followers"),
                        callback: ditchFollower });
                }
                // if we're following someone, add a menu item for stopping
                if (us.following != null) {
                    followItems.push({ label: Msgs.GENERAL.get("b.stop_following"),
                        callback: handleRespondFollow, arg: 0 });
                }
            } else {
                // we could be following them...
                if (name.equals(us.following)) {
                    followItems.push({ label: Msgs.GENERAL.get("b.stop_following"),
                        callback: handleRespondFollow, arg: 0 });
                } else {
                    followItems.push({ label: Msgs.GENERAL.get("b.follow_other"),
                        callback: handleRespondFollow, arg: memId, enabled: !isMuted });
                }
                // and/or they could be following us...
                if (us.followers.containsKey(memId)) {
                    followItems.push({ label: Msgs.GENERAL.get("b.ditch_follower"),
                        callback: ditchFollower, arg: memId });
                } else {
                    followItems.push({ label: Msgs.GENERAL.get("b.invite_follow"),
                        callback: inviteFollow, arg: memId, enabled: !isMuted });
                }
            }
            if (followItems.length > 0) {
                followItem = { label: Msgs.GENERAL.get("l.following"), children: followItems };
            }
        }

        var icon :* = null;
        if (isUs) {
            icon = MediaWrapper.createView(
                us.memberName.getPhoto(), MediaDescSize.QUARTER_THUMBNAIL_SIZE);
//        } else if (name is VizMemberName) {
//            icon = MediaWrapper.createView(
//                VizMemberName(name).getPhoto(), MediaDesc.QUARTER_THUMBNAIL_SIZE);
        }
        CommandMenu.addTitle(menuItems, name.toString(), icon);
        if (isUs) {
            if (followItem != null) {
                menuItems.push(followItem);
            }

        } else {
            const onlineFriend :Boolean = us.isOnlineFriend(memId);
            const isInOurRoom :Boolean = (placeCtrl is RoomObjectController) &&
                RoomObjectController(placeCtrl).containsPlayer(name);
            // whisper
            if (!isPuppet) {
                menuItems.push({ label: Msgs.GENERAL.get("b.open_channel"), icon: WHISPER_ICON,
                    command: OPEN_CHANNEL, arg: name, enabled: !muted });
            }
            // add as friend
            if (!onlineFriend) {
                menuItems.push({ label: Msgs.GENERAL.get("l.add_as_friend"), icon: ADDFRIEND_ICON,
                    command: INVITE_FRIEND, arg: memId, enabled: !muted });
            }
            // visit
            if ((onlineFriend || isSupport) && !isPuppet) {
                var label :String = onlineFriend ?
                    Msgs.GENERAL.get("b.visit_friend") : "Visit (as agent)";
                menuItems.push({ label: label, icon: VISIT_ICON,
                    command: VISIT_MEMBER, arg: memId, enabled: !isInOurRoom });
            }
// Visit Home disabled. Jon says it's pointless.
//            menuItems.push({ label: Msgs.GENERAL.get("b.visit_home"),
//                command: GO_MEMBER_HOME, arg: memId });
            // profile
            menuItems.push({ label: Msgs.GENERAL.get("b.view_member"),
                command: VIEW_MEMBER, arg: memId });
            // following
            if (followItem != null) {
                menuItems.push(followItem);
            }
            // partying
            if (!isPuppet && _wctx.getPartyDirector().canInviteToParty()) {
                menuItems.push({ label: Msgs.PARTY.get("b.invite_member"),
                    command: INVITE_TO_PARTY, arg: memId,
                    enabled: !muted && !_wctx.getPartyDirector().partyContainsPlayer(memId) });
            }

            CommandMenu.addSeparator(menuItems);
            // muting
            var muted :Boolean = _mctx.getMuteDirector().isMuted(name);
            menuItems.push({ label: Msgs.GENERAL.get(muted ? "b.unmute" : "b.mute"),
                icon: BLOCK_ICON,
                callback: _mctx.getMuteDirector().setMuted, arg: [ name, !muted ] });
            // booting
            if (!isPuppet && addWorldItems && isInOurRoom &&
                    (placeCtrl is BootablePlaceController) &&
                    BootablePlaceController(placeCtrl).canBoot()) {
                menuItems.push({ label: Msgs.GENERAL.get("b.boot"),
                    callback: handleBootFromPlace, arg: memId });
            }
            // reporting
            if (!isPuppet) {
                menuItems.push({ label: Msgs.GENERAL.get("b.complain"), icon: REPORT_ICON,
                    command: COMPLAIN_MEMBER, arg: [ memId, name ] });
            }
        }

        // now the items specific to the avatar
        if (addWorldItems && (placeCtrl is RoomObjectController)) {
            RoomObjectController(placeCtrl).addAvatarMenuItems(name, menuItems);
        }

        // login/logout
        if (isUs && !_wctx.getMsoyClient().getEmbedding().hasGWT()) {
            if (_wctx.getMemberObject().isPermaguest()) {
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

    /**
     * Add pet menu items.
     */
    public function addPetMenuItems (petName :PetName, menuItems :Array) :void
    {
        const ownerMuted :Boolean = _wctx.getMuteDirector().isOwnerMuted(petName);
        if (ownerMuted) {
            menuItems.push({ label: Msgs.GENERAL.get("b.unmute_owner"), icon: BLOCK_ICON,
                callback: _mctx.getMuteDirector().setMuted,
                arg: [ new MemberName("", petName.getOwnerId()), false ] });
        } else {
            const isMuted :Boolean = _wctx.getMuteDirector().isMuted(petName);
            menuItems.push({ label: Msgs.GENERAL.get(isMuted ? "b.unmute_pet" : "b.mute_pet"),
                icon: BLOCK_ICON,
                callback: _wctx.getMuteDirector().setMuted, arg: [ petName, !isMuted ] });
        }
    }

    // from MsoyController
    override public function handleClosePlaceView () :void
    {
        // give the handlers a chance to prevent closure
        if (!sanctionClosePlaceView()) {
            return;
        }
        if (_wctx.getPlaceView() is ParlorGamePanel) {
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
        var memObj :MemberObject = _wctx.getMemberObject();
        if (memObj == null) {
            return false;
        }
        if (memObj.getHomeSceneId() != curSceneId) {
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

    // from MsoyController
    override public function reconnectClient () :void
    {
        _didFirstLogonGo = false;
        super.reconnectClient();
    }

    // from ClientObserver
    override public function clientDidLogon (event :ClientEvent) :void
    {
        super.clientDidLogon(event);

        var memberObj :MemberObject = _wctx.getMemberObject();
        // if not a permaguest, save the username that we logged in with
        if (!memberObj.isPermaguest()) {
            var name :Name = (_wctx.getClient().getCredentials() as MsoyCredentials).getUsername();
            if (name != null) {
                Prefs.setUsername(name.toString());
            }
        }

        if (!_didFirstLogonGo) {
            _didFirstLogonGo = true;
            goToPlace(MsoyParameters.get());
        } else if (_postLogonScene != 0) {
            // we gotta go somewhere
            _wctx.getSceneDirector().moveTo(_postLogonScene);
            _postLogonScene = 0;
        }
    }

    // from ClientObserver
    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        // if we're actually terminating our session, clear out our AVG panel
        if (!event.isSwitchingServers()) {
            setAVRGamePanel(null);
        }
    }

    /**
     * Inform our parent web page that our display name has changed.
     */
    public function refreshDisplayName () :void
    {
        try {
            if (ExternalInterface.available) {
                ExternalInterface.call("refreshDisplayName");
            }
        } catch (e :Error) {
        }
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
                log.warning("Unable to display page via Javascript", "page", page, "args", args, e);
            }
        }
        return false;
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

    protected function handleBleepChange (event :NamedValueEvent) :void
    {
        if (_music == null) {
            return; // couldn't possibly concern us..
        }
        if (isMusicBleeped() == musicIsPlayingOrPaused()) {
            // just call play again with the same music, it'll handle it
            handlePlayMusic(_music);
        }
    }

    protected function handleConfigValueSet (event :NamedValueEvent) :void
    {
        // if the volume got turned up and we were not playing music, play it now.
        if ((event.name == Prefs.VOLUME) && (event.value > 0) && (_music != null) &&
               !musicIsPlayingOrPaused()) {
            handlePlayMusic(_music);
        }
    }

    protected function isMusicBleeped () :Boolean
    {
        return Prefs.isGlobalBleep() ||
            (_music != null && Prefs.isMediaBleeped(_music.audioMedia.getMediaId()));
    }

    protected function musicIsPlayingOrPaused () :Boolean
    {
        switch (_musicPlayer.getState()) {
        default: return false;
        case MediaPlayerCodes.STATE_PLAYING: // fall through
        case MediaPlayerCodes.STATE_STOPPED: // fall through
        case MediaPlayerCodes.STATE_PAUSED: return true;
        }
    }

    protected function handleMusicMetadata (event :ValueEvent) :void
    {
        if (_musicInfoShown) {
            return;
        }
        var id3 :Object = event.value;
        var artist :String = id3.artist as String;
        var songName :String = id3.songName as String;
        if (!StringUtil.isBlank(artist) || !StringUtil.isBlank(songName)) {
            if (StringUtil.isBlank(artist)) {
                artist = "unknown";
            }
            if (StringUtil.isBlank(songName)) {
                songName = "unknown";
            }
            var owner :MemberName = null;
            var room :RoomObject = _wctx.getLocationDirector().getPlaceObject() as RoomObject;
            if (room != null && room.inDjMode()) {
                var info :MemberInfo = room.getMemberInfo(_music.ownerId);
                if (info != null) {
                    owner = MemberName(info.username);
                }
            }
            _wctx.getNotificationDirector().notifyMusic(owner, songName, artist);
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

    // from MsoyController
    override protected function setIdle (nowIdle :Boolean) :void
    {
        const wasIdle :Boolean = isIdle();

        super.setIdle(nowIdle);

        // only change game idleness when it truly changes
        if (wasIdle != isIdle()) {
            // let AVRGs know about our idleness changes
            var gd :GameDirector = _wctx.getGameDirector();
            if (gd != null) { // studio has no GameDirector
                // game idleness = whirled idle or whirled away
                gd.setIdle(isIdle());
            }
        }
    }

    // from MsoyController
    override protected function locationDidChange (place :PlaceObject) :void
    {
        super.locationDidChange(place);

        // if we moved to a scene, set things up thusly
        var scene :Scene = _wctx.getSceneDirector().getScene();
        if (scene != null) {
            addRecentScene(scene);
        }
    }

    // from MsoyController
    override protected function populateGoMenu (menuData :Array) :void
    {
        super.populateGoMenu(menuData);

        const me :MemberObject = _wctx.getMemberObject();
        const curSceneId :int = getCurrentSceneId();
        const ourHomeId :int = me.homeSceneId;
        const homeless :Boolean =
            _wctx.getMsoyClient().getEmbedding().isMinimal() || ourHomeId == 0;

        var scenes :Array = homeless ?
            _recentScenes.slice() :
            _recentScenes.filter(function (scene :Object, ..._) :Boolean {
                return scene.id != ourHomeId;
            });

        // Recent rooms
        if (scenes.length > 0) {
            scenes.reverse();
            for each (var entry :Object in scenes) {
                menuData.push({ label: StringUtil.truncate(entry.name, 50, "..."),
                    command: GO_SCENE, arg: entry.id, enabled: (entry.id != curSceneId) });
            }
            CommandMenu.addSeparator(menuData);
        }

        // Go home
        if (!homeless) {
            menuData.push({ label: Msgs.GENERAL.get("b.go_home"), command: GO_SCENE, arg: ourHomeId,
                enabled: (ourHomeId != curSceneId) });
        }
    }

    /**
     * Sends an invitation to the specified member to follow us.
     */
    protected function inviteFollow (memId :int) :void
    {
        WorldService(_wctx.getClient().requireService(WorldService)).
            inviteToFollow(memId, _wctx.listener());
    }

    /**
     * Tells the server we no longer want someone following us. If target memberId is 0, all
     * our followers are ditched.
     */
    protected function ditchFollower (memId :int = 0) :void
    {
        WorldService(_wctx.getClient().requireService(WorldService)).
            ditchFollower(memId, _wctx.listener());
    }

    /**
     * Convenience.
     */
    protected function msvc () :MemberService
    {
        return MemberService(_wctx.getClient().requireService(MemberService));
    }

    protected function doSnapshot () :void
    {
        if (_snapPanel == null) {
            _snapPanel = new SnapshotPanel(_wctx);
            _snapPanel.addCloseCallback(function () :void {
                _snapPanel = null;
            });
        }
    }

    protected function doShowMusic (trigger :Button) :void
    {
        if (_music != null && _musicDialog == null) {
            var room :RoomObject = _wctx.getLocationDirector().getPlaceObject() as RoomObject;
            var scene :MsoyScene = _wctx.getSceneDirector().getScene() as MsoyScene;
            _musicDialog = new RoomMusicDialog(
                _wctx, trigger.localToGlobal(new Point()), room, scene);
            _musicDialog.addCloseCallback(function () :void {
                _musicDialog = null;
            });
            _musicDialog.open();
        }
    }

    protected function addFrameColorOption (menuData :Array) :void
    {
        menuData.push({ label: Msgs.GENERAL.get("b.frame_color"),
            command: doShowColorPicker });
    }

    protected function doShowColorPicker () :void
    {
        if (_picker == null) {
            _picker = new ColorPickerPanel(_wctx);
            _picker.addCloseCallback(function () :void {
                _picker = null;
            });
            _picker.open();
        }
    }

    /** Giver of life, context. */
    protected var _wctx :WorldContext;

    /** The player of music. */
    protected var _musicPlayer :Mp3AudioPlayer = new Mp3AudioPlayer();

    /** The currently playing music. */
    protected var _music :Audio;

    /** Have we displayed music info in a notification? */
    protected var _musicInfoShown :Boolean;

    /** Have we paused the music so that we can play some media in gwt? */
    protected var _musicPausedForGwt :Boolean;

    protected var _musicDialog :RoomMusicDialog;

    protected var _snapPanel :SnapshotPanel;

    protected var _tablesPanel :TablesWaitingPanel;

    protected var _picker :ColorPickerPanel;

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

    [Embed(source="../../../../../../../rsrc/media/skins/controlbar/editroom.png")]
    protected static const ROOM_EDIT_ICON :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/controlbar/music.png")]
    protected static const MUSIC_ICON :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/controlbar/snapshot.png")]
    protected static const SNAPSHOT_ICON :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/menu/addfriend.png")]
    protected static const ADDFRIEND_ICON :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/menu/block.png")]
    protected static const BLOCK_ICON :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/menu/report.png")]
    protected static const REPORT_ICON :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/menu/visit.png")]
    protected static const VISIT_ICON :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/menu/whisper.png")]
    protected static const WHISPER_ICON :Class;
}
}
