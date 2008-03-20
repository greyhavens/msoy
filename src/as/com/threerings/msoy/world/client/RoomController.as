//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;
import flash.ui.ContextMenuItem;
import flash.ui.Keyboard;
import flash.utils.ByteArray;

import mx.controls.Button;
import mx.core.Application;
import mx.core.IChildList;
import mx.core.IToolTip;
import mx.core.UIComponent;
import mx.managers.ISystemManager;
import mx.managers.ToolTipManager;

import com.threerings.io.TypedArray;

import com.threerings.util.ArrayUtil;
import com.threerings.util.ClassUtil;
import com.threerings.util.Integer;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.NetUtil;
import com.threerings.util.ObjectMarshaller;
import com.threerings.util.ValueEvent;

import com.threerings.flash.MenuUtil;
import com.threerings.flex.CommandMenu;

import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.ResultWrapper;

import com.threerings.presents.dobj.ChangeListener;
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;
import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.LogonPanel;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Pet;

import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.editor.DoorTargetEditController;
import com.threerings.msoy.world.client.editor.ItemUsedDialog;
import com.threerings.msoy.world.client.editor.RoomEditorController;
import com.threerings.msoy.world.client.editor.SnapshotController;
import com.threerings.msoy.world.client.updates.FurniUpdateAction;
import com.threerings.msoy.world.client.updates.SceneUpdateAction;
import com.threerings.msoy.world.client.updates.UpdateAction;
import com.threerings.msoy.world.client.updates.UpdateStack;

import com.threerings.msoy.world.data.ActorInfo;
import com.threerings.msoy.world.data.AudioData;
import com.threerings.msoy.world.data.Controllable;
import com.threerings.msoy.world.data.ControllableAVRGame;
import com.threerings.msoy.world.data.ControllableEntity;
import com.threerings.msoy.world.data.EffectData;
import com.threerings.msoy.world.data.EntityControl;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MemberInfo;
import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.ModifyFurniUpdate;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.PetInfo;
import com.threerings.msoy.world.data.RoomPropertyEntry;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.SceneAttrsUpdate;

import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.RadialMenu;
import com.threerings.msoy.ui.ScalingMediaContainer;

import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.chat.client.ReportingListener;

/**
 * Manages the various interactions that take place in a room scene.
 */
public class RoomController extends SceneController
{
    private const log :Log = Log.getLog(RoomController);

    public static const EDIT_DOOR :String = "EditDoor";
    public static const FURNI_CLICKED :String = "FurniClicked";
    public static const AVATAR_CLICKED :String = "AvatarClicked";
    public static const PET_CLICKED :String = "PetClicked";

    public static const ORDER_PET :String = "OrderPet";

    /**
     * Get the instanceId of all the entity instances in the room.  This is used so that two
     * instances of a pet can negotiate which client will control it, for example.
     */
    public function getEntityInstanceId () :int
    {
        // every sprite uses our own OID as the instanceid.
        return _wdctx.getMemberObject().getOid();
    }

    /**
     * Get the display name of the user viewing a particular instance.
     */
    public function getViewerName (instanceId :int = 0) :String
    {
        if (instanceId == 0) {
            // get our name
            return _wdctx.getMemberObject().getVisibleName().toString();
        }

        // otherwise, locate the name in the OccupantInfos
        var occInfo :OccupantInfo = _roomObj.occupantInfo.get(instanceId) as OccupantInfo;
        return (occInfo == null) ? null : occInfo.username.toString();
    }

    /**
     * Returns true if we are in edit mode, false if not.
     */
    public function isEditMode () :Boolean
    {
        // currently holding shift down puts us in edit mode, soon this will be based on whether or
        // not the hammer has been clicked
        return _shiftDown || isRoomEditing();
    }

    /**
     * Requests that this client be given control of the specified item.
     */
    public function requestControl (ident :ItemIdent) :void
    {
        if (_roomObj == null) {
            log.warning("Cannot request entity control, no room object [ident=" + ident + "].");
            return;
        }

        var result :Object = hasEntityControl(ident);
        // side-effect of calling hasEntityControl: the sprite will be notified (possibly again)
        // that it has control if it does
        if (result == null) {
            // only if nobody currently has control do we issue the request
            _roomObj.roomService.requestControl(_wdctx.getClient(), ident);
        }
    }

    /**
     * Handles a request by an item in our room to send an "action" (requires control) or a
     * "message" (doesn't require control).
     */
    public function sendSpriteMessage (
        ident :ItemIdent, name :String, arg :Object, isAction :Boolean) :void
    {
        if (isAction && !checkCanRequest(ident, "triggerAction")) {
            log.info("Dropping message for lack of control [ident=" + ident +
                     ", name=" + name + "].");
            return;
        }

        // send the request off to the server
        log.info("Sending sprite message [ident=" + ident + ", name=" + name + "].");
        var data :ByteArray = ObjectMarshaller.validateAndEncode(arg);
        _roomObj.roomService.sendSpriteMessage(_wdctx.getClient(), ident, name, data, isAction);
    }

    /**
     * Handles a request by an item in our room to send a "signal" to all the instances of
     * all the entities in the room. This does not require control.
     */
    public function sendSpriteSignal (name :String, arg :Object) :void
    {
        // send the request off to the server
        log.info("Sending sprite signal [name=" + name + "].");
        var data :ByteArray = ObjectMarshaller.validateAndEncode(arg);
        _roomObj.roomService.sendSpriteSignal(_wdctx.getClient(), name, data);
    }

    /**
     * Handles a request by an actor item to change its persistent state.  Requires control.
     */
    public function setActorState (ident :ItemIdent, actorOid :int, state :String) :void
    {
        if (!checkCanRequest(ident, "setState")) {
            log.info("Dropping state change for lack of control [ident=" + ident +
                     ", state=" + state + "].");
            return;
        }

        log.info("Changing actor state [ident=" + ident + ", state=" + state + "].");
        _roomObj.roomService.setActorState(_wdctx.getClient(), ident, actorOid, state);
    }

    /**
     * Handles a request by an entity item to send a chat message.
     */
    public function sendPetChatMessage (msg :String, info :ActorInfo) :void
    {
        var svc :PetService = (_wdctx.getClient().requireService(PetService) as PetService);
        if (checkCanRequest(info.getItemIdent(), "PetService")) {
            svc.sendChat(_wdctx.getClient(), info.bodyOid, _scene.getId(), msg,
                         new ReportingListener(_wdctx));
        }
    }

    /**
     * Handles a request by an item in our room to update its memory.
     */
    public function updateMemory (ident :ItemIdent, key :String, value: Object) :Boolean
    {
// NOTE: I've disabled the need to be in control to update memory (Ray July 6, 2007)
//        if (!checkCanRequest(ident, "updateMemory")) {
//            return false;
//        }

        // serialize datum
        var data :ByteArray = ObjectMarshaller.validateAndEncode(value);

        // TODO: total up item's used memory, ensure it doesn't exceed the allowed limit

        // ship the update request off to the server
        _roomObj.roomService.updateMemory(
            _wdctx.getClient(), new EntityMemoryEntry(ident, key, data));
        return true;
    }

    /**
     * Handles a request to update a property in this room.
     */
    public function setRoomProperty (key :String, value: Object) :Boolean
    {
        // serialize datum
        var data :ByteArray = ObjectMarshaller.validateAndEncode(value);

        if (key.length > RoomPropertyEntry.MAX_KEY_LENGTH ||
            (data != null && data.length > RoomPropertyEntry.MAX_VALUE_LENGTH)) {
            return false;
        }

        var entry :RoomPropertyEntry = new RoomPropertyEntry(key, data);

        if (_roomObj.roomProperties.contains(entry) &&
            _roomObj.roomProperties.size() >= RoomPropertyEntry.MAX_ENTRIES) {
            return false;
        }

        // ship the update request off to the server
        _roomObj.roomService.setRoomProperty(_wdctx.getClient(), entry);
        return true;
    }

    /**
     * Handles a request by an actor to change its location. Returns true if the request was
     * dispatched, false if funny business prevented it.
     */
    public function requestMove (ident :ItemIdent, newloc :MsoyLocation) :Boolean
    {
        if (!checkCanRequest(ident, "requestMove")) {
            return false;
        }
        _roomObj.roomService.changeLocation(_wdctx.getClient(), ident, newloc);
        return true;
    }

    /**
     * Sends an invitation to the specified member to follow us. If member is null, all our
     * existing follows will be cleared.
     */
    public function inviteFollow (member :MemberName) :void
    {
        var msvc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        if (member == null) {
            msvc.inviteToFollow(_ctx.getClient(), 0, new ReportingListener(
                                    _wdctx, MsoyCodes.GENERAL_MSGS, null, "m.follows_cleared"));
        } else {
            msvc.inviteToFollow(_ctx.getClient(), member.getMemberId(), new ReportingListener(
                                    _wdctx, MsoyCodes.GENERAL_MSGS, null,
                                    MessageBundle.tcompose("m.invited_to_follow", member)));
        }
    }

    /**
     * Tells the server we no longer want to be following anyone.
     */
    public function clearFollow () :void
    {
        var msvc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        msvc.followMember(_ctx.getClient(), 0, new ReportingListener(
                              _wdctx, MsoyCodes.GENERAL_MSGS, null, "m.not_following"));
    }

    /**
     * Called to enact the avatar action globally: all users will see it.
     */
    public function doAvatarAction (action :String) :void
    {
        sendSpriteMessage(_roomView.getMyAvatar().getItemIdent(), action, null, true);
    }

    /**
     * Called to enact an avatar state change.
     */
    public function doAvatarState (state :String) :void
    {
        var avatar :MemberSprite = _roomView.getMyAvatar();
        setActorState(avatar.getItemIdent(), avatar.getOid(), state);
    }

    /**
     * Updates our availability state.
     */
    public function updateAvailability (availability :int) :void
    {
        var msvc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        msvc.updateAvailability(_ctx.getClient(), availability);
        _wdctx.displayFeedback(MsoyCodes.GENERAL_MSGS, "m.avail_tip_" + availability);
    }

    /**
     * Returns true if we are in edit mode, false if not.
     */
    public function isRoomEditing () :Boolean
    {
        return _editor.isEditing();
    }

    /**
     * Takes a snapshot of the current room.
     */
    public function takeSnapshot () :void
    {
        _snap.takeScreenshot(_roomView);
    };

    /**
     * Handles EDIT_DOOR.
     */
    public function handleEditDoor (furniData :FurniData) :void
    {
        if (isRoomEditing()) {
            cancelRoomEditing();
        }

        _roomObj.roomService.editRoom(_wdctx.getClient(), new ResultWrapper(
            function (cause :String) :void {
                _wdctx.displayFeedback(MsoyCodes.GENERAL_MSGS, cause);
            },
            function (result :Object) :void {
                DoorTargetEditController.start(furniData, _wdctx);
            }));
    }

    /**
     * Handle the ROOM_EDIT command.
     */
    public function handleRoomEdit () :void
    {
        _roomObj.roomService.editRoom(_wdctx.getClient(), new ResultWrapper(
            function (cause :String) :void {
                _wdctx.displayFeedback(MsoyCodes.GENERAL_MSGS, cause);
            },
            function (result :Object) :void {
                // if we're editing, let's finish, otherwise let's start!
                if (isRoomEditing()) {
                    cancelRoomEditing();
                } else {
                    beginRoomEditing();
                }
            }));
    }

    /**
     * Handles FURNI_CLICKED.
     */
    public function handleFurniClicked (furni :FurniData) :void
    {
        switch (furni.actionType) {
        case FurniData.ACTION_URL:
            NetUtil.navigateToURL(furni.splitActionData()[0] as String);
            return;

        case FurniData.ACTION_WORLD_GAME:
            postAction(WorldController.JOIN_AVR_GAME, int(furni.splitActionData()[0]));
            return;

        case FurniData.ACTION_LOBBY_GAME:
            postAction(MsoyController.VIEW_GAME, int(furni.splitActionData()[0]));
            return;

        case FurniData.ACTION_PORTAL:
            (_wdctx.getSceneDirector() as MsoySceneDirector).traversePortal(furni.id);
            return;

        case FurniData.ACTION_HELP_PAGE:
            var actionData :Array = furni.splitActionData();
            var tabName :String = String(actionData[0]);
            var url :String = String(actionData[1]);
            // TBD: how to display help pages?
            return;

        default:
            log.warning("Clicked on unhandled furni action type [actionType=" + furni.actionType +
                        ", actionData=" + furni.actionData + "].");
            return;
        }
    }

    /**
     * Handles AVATAR_CLICKED.
     */
    public function handleAvatarClicked (avatar :MemberSprite) :void
    {
        var occInfo :MemberInfo = (avatar.getActorInfo() as MemberInfo);
        if (occInfo == null) {
            log.info("Clicked on non-MemberInfo sprite [info=" + avatar.getActorInfo() + "].");
            return;
        }

        var us :MemberObject = _wdctx.getMemberObject();
        var menuItems :Array = [];
        if (occInfo.bodyOid == us.getOid()) {
            // see if we can control our own avatar right now...
            var canControl :Boolean = _wdctx.worldProps.userControlsAvatar;

            // if we have followers, add a menu item for clearing them
            if (us.followers.size() > 0) {
                menuItems.push({ label: Msgs.GENERAL.get("l.clear_followers"),
                                 callback: inviteFollow, arg: null });
            }

            // if we're following someone, add a menu item for stopping
            if (us.following != null) {
                menuItems.push({ label: Msgs.GENERAL.get("l.stop_following"),
                                 callback: clearFollow });
            }

            // create a menu for controlling our availability
            var availActions :Array = [];
            for (var ii :int = MemberObject.AVAILABLE; ii <= MemberObject.UNAVAILABLE; ii++) {
                availActions.push({
                    label: Msgs.GENERAL.get("l.avail_" + ii), callback: updateAvailability, arg: ii,
                    type: "check", toggled: (ii == us.availability) });
            }
            menuItems.push({ label: Msgs.GENERAL.get("l.avail_menu"),
                             children: availActions, enabled: canControl });

            // if we're not a guest add a menu for changing avatars
            if (!us.isGuest()) {
                menuItems.push(createChangeAvatarMenu(us, canControl));
            }

            // create a sub-menu for playing avatar actions
            var actions :Array = avatar.getAvatarActions();
            if (actions.length > 0) {
                var worldActions :Array = [];
                for each (var act :String in actions) {
                    worldActions.push({ label: act, callback: doAvatarAction, arg: act });
                }
                menuItems.push({ label: Msgs.GENERAL.get("l.avAction"),
                    children: worldActions, enabled: canControl });
            }

            // create a sub-menu for changing avatar states
            var states :Array = avatar.getAvatarStates();
            if (states.length > 0) {
                var worldStates :Array = [];
                var curState :String = avatar.getState();
                if (curState == null) {
                    curState = states[0];
                }
                for each (var state :String in states) {
                    worldStates.push({ label: state, callback: doAvatarState, arg :state,
                        type: "check", toggled: (curState == state) });
                }
                menuItems.push({ label: Msgs.GENERAL.get("l.avStates"),
                    children: worldStates, enabled: canControl });
            }

            if (_wdctx.getWorldClient().isEmbedded()) {
                if (us.isGuest()) {
                    menuItems.push({ label: Msgs.GENERAL.get("b.logon"),
                        callback: function () :void {
                            (new LogonPanel(_wdctx)).open();
                        }});
                } else {
                    menuItems.push({ label: Msgs.GENERAL.get("b.logout"),
                        callback: function () :void {
                            var sceneId :int = _scene.getId();
                            var observer :ClientAdapter;
                            var logon :Function = function (...ignored) :void {
                                _wdctx.getSceneDirector().moveTo(sceneId);
                                _wdctx.getClient().removeClientObserver(observer);
                            }
                            observer = new ClientAdapter(null, logon);
                            _wdctx.getClient().addClientObserver(observer);
                            _wdctx.getWorldController().handleLogon(null);
                        }});
                }
            }

        } else {
            // create a menu for clicking on someone else
            var memId :int = occInfo.getMemberId();
            menuItems.push({ label: Msgs.GENERAL.get("l.open_channel"),
                command: WorldController.OPEN_CHANNEL, arg: occInfo.username });

            if (MemberName.isGuest(memId)) {
                menuItems.push(
                    { label: Msgs.GENERAL.get("l.invite_to_whirled"),
                      command: WorldController.INVITE_GUEST, arg: occInfo.username });

            } else {
                // TODO: move up when we can forward MemberObjects between servers for guests
                menuItems.push({ label: Msgs.GENERAL.get("l.invite_follow"),
                                 callback: inviteFollow, arg: occInfo.username });
                menuItems.push(
                    { label: Msgs.GENERAL.get("l.visit_home"),
                      command: WorldController.GO_MEMBER_HOME, arg: memId });
                if (!_wdctx.getWorldClient().isEmbedded()) {
                    menuItems.push(
                        { label: Msgs.GENERAL.get("l.view_member"),
                          command: WorldController.VIEW_MEMBER, arg: memId });
                }
                if (!us.isGuest() && !us.friends.containsKey(memId)) {
                    menuItems.push(
                        { label: Msgs.GENERAL.get("l.add_as_friend"),
                          command: WorldController.INVITE_FRIEND, arg: [memId] });
                }
            }
        }

        // pop up the menu where the mouse is
        if (menuItems.length > 0) {
            var menu :CommandMenu = CommandMenu.createMenu(menuItems);
            menu.variableRowHeight = true;
            menu.setDispatcher(_roomView);
            menu.popUpAtMouse();

//            var menu :RadialMenu = new RadialMenu(100);
//            menu.dataProvider = menuItems;
//            menu.popUp(Application(Application.application));
        }
    }

    /**
     * Handles PET_CLICKED.
     */
    public function handlePetClicked (pet :ActorSprite) :void
    {
        var occInfo :PetInfo = (pet.getActorInfo() as PetInfo);
        if (occInfo == null) {
            log.warning("Pet has unexpected ActorInfo [info=" + pet.getActorInfo() + "].");
            return;
        }

        // no menu for non-owners for now
        if (occInfo.getOwnerId() != _wdctx.getMemberObject().getMemberId()) {
            return;
        }

        var petId :int = occInfo.getItemIdent().itemId;
        var menuItems :Array = [];

        // TODO: check for pet ownership, etc.
        menuItems.push(
//         { label: Msgs.GENERAL.get("b.order_pet_stay"),
//           command: ORDER_PET, arg: [ petId, Pet.ORDER_STAY ] },
//         { label: Msgs.GENERAL.get("b.order_pet_follow"),
//           command: ORDER_PET, arg: [ petId, Pet.ORDER_FOLLOW ] },
//         { label: Msgs.GENERAL.get("b.order_pet_go_home"),
//           command: ORDER_PET, arg: [ petId, Pet.ORDER_GO_HOME ] },
        { label: Msgs.GENERAL.get("b.order_pet_sleep"),
          command: ORDER_PET, arg: [ petId, Pet.ORDER_SLEEP ] }
        );

        // pop up the menu where the mouse is
        if (menuItems.length > 0) {
            var menu :CommandMenu = CommandMenu.createMenu(menuItems);
            menu.setDispatcher(_roomView);
            menu.popUpAtMouse();
        }
    }

    /**
     * Handles ORDER_PET.
     */
    public function handleOrderPet (petId :int, command :int) :void
    {
        var svc :PetService = (_wdctx.getClient().requireService(PetService) as PetService);
        svc.orderPet(_wdctx.getClient(), petId, command,
            new ReportingListener(_wdctx, MsoyCodes.GENERAL_MSGS, null, "m.pet_ordered" + command));
    }

    /**
     * Get the top-most sprite mouse-capturing sprite with a non-transparent pixel at the specified
     * location.
     *
     * @return undefined if the mouse isn't in our bounds, or null, or an MsoySprite.
     */
    public function getHitSprite (stageX :Number, stageY :Number, all :Boolean = false) :*
    {
        // first, avoid any popups
        var smgr :ISystemManager = Application.application.systemManager as ISystemManager;
        var ii :int;
        var disp :DisplayObject;
        for (ii = smgr.numChildren - 1; ii >= 0; ii--) {
            disp = smgr.getChildAt(ii)
            if (disp is Application) {
                continue;
            }
            if (disp.hitTestPoint(stageX, stageY)) {
                return undefined;
            }
        }

        // then check with the PlaceBox
        if (_wdctx.getTopPanel().getPlaceContainer().overlaysMousePoint(stageX, stageY)) {
            return undefined;
        }

        // then avoid any chat glyphs that are clickable
        if (_roomView.getChatOverlay().hasClickableGlyphsAtPoint(stageX, stageY)) {
            return undefined;
        }

        // we search from last-drawn to first drawn to get the topmost...
        for (var dex :int = _roomView.numChildren - 1; dex >= 0; dex--) {
            var spr :MsoySprite = (_roomView.getChildAt(dex) as MsoySprite);
            if ((spr != null) && (all || (spr.isActive() && spr.capturesMouse())) &&
                spr.hitTestPoint(stageX, stageY, true))
            {
                return spr;
            }
        }

        return null;
    }

    /**
     * Returns true if the local client is allowed to edit the current scene.
     */
    public function canEditRoom () : Boolean
    {
        return (_scene != null && _scene.canEdit(_wdctx.getMemberObject()));
    }

    /**
     * Called from JavaScript to determine if the item in question is in use (as decor, furni, the
     * user's current avatar, an active pet, anything).
     */
    public function isItemInUse (itemType :int, itemId :int) :Boolean
    {
        if (itemType == Item.AVATAR) {
            var avatar :Avatar = _wdctx.getMemberObject().avatar;
            return (avatar == null) ? false : (avatar.itemId == itemId);

        } else if (itemType == Item.PET) {
            // ensure this pet really is in this room
            for each (var pet :PetSprite in _roomView.getPets()) {
                if (pet.getItemIdent().itemId == itemId) {
                    return true;
                }
            }
            return false;

        } else if (itemType == Item.DECOR) {
            return (_scene.getDecor() != null) && (_scene.getDecor().itemId == itemId);

        } else if (itemType == Item.AUDIO) {
            return (_scene.getAudioData() != null) && (_scene.getAudioData().itemId == itemId);

        } else {
            for each (var furni :FurniData in _scene.getFurni()) {
                if (furni.itemType == itemType && furni.itemId == itemId) {
                    return true;
                }
            }
            return false;
        }
    }

    /**
     * This is called from JavaScript to select this room's decor, audio, or add a piece of furni.
     */
    public function useItem (itemType :int, itemId :int) :void
    {
        if (itemType == Item.AVATAR) {
            _wdctx.getWorldDirector().setAvatar(itemId, 1);
            return;
        }

        if (itemType == Item.PET) {
            var svc :PetService = _ctx.getClient().requireService(PetService) as PetService;
            svc.callPet(_wdctx.getClient(), itemId,
                        new ReportingListener(_wdctx, MsoyCodes.GENERAL_MSGS, null, "m.pet_called"));
            return;
        }

        if (!canEditRoom()) {
            _wdctx.displayInfo(MsoyCodes.EDITING_MSGS, "e.no_permission");
            return;
        }

        if (itemType != Item.DECOR && itemType != Item.AUDIO) {
            _openEditor = true;
        }

        var isvc :ItemService = _wdctx.getClient().requireService(ItemService) as ItemService;
        var ident :ItemIdent = new ItemIdent(itemType, itemId);

        var gotItem :Function = function (item :Item) :void {

            // a function we'll invoke when we're ready to use the item
            var useNewItem :Function = function () :void {
                var newScene :MsoyScene;

                if (item.getType() == Item.DECOR) {
                    newScene = _scene.clone() as MsoyScene;
                    var newSceneModel :MsoySceneModel = (newScene.getSceneModel() as MsoySceneModel);
                    newSceneModel.decor = item as Decor;
                    applyUpdate(new SceneUpdateAction(_wdctx, _scene, newScene));
                    _wdctx.getGameDirector().tutorialEvent("decorInstalled");

                } else if (item.getType() == Item.AUDIO) {
                    newScene = _scene.clone() as MsoyScene;
                    var ad :AudioData = (newScene.getSceneModel() as MsoySceneModel).audioData;
                    var audio :Audio = item as Audio;
                    ad.itemId = audio.itemId;
                    ad.media = audio.audioMedia;
                    applyUpdate(new SceneUpdateAction(_wdctx, _scene, newScene));

                } else {
                    // create a generic furniture descriptor
                    var furni :FurniData = new FurniData();
                    furni.id = _scene.getNextFurniId(0);
                    furni.itemType = item.getType();
                    furni.itemId = item.itemId;
                    furni.media = item.getFurniMedia();
                    // create it at the front of the scene, centered on the floor
                    furni.loc = new MsoyLocation(0.5, 0, 0);
                    if (item is Furniture) {
                        furni.hotSpotX = (item as Furniture).hotSpotX;
                        furni.hotSpotY = (item as Furniture).hotSpotY;
                    }
                    if (item is Game) {
                        var game :Game = (item as Game);
                        furni.actionType = game.isInWorld() ?
                            FurniData.ACTION_WORLD_GAME : FurniData.ACTION_LOBBY_GAME;
                        furni.actionData = String(game.gameId) + ":" + game.name;
                    }
                    applyUpdate(new FurniUpdateAction(_wdctx, null, furni));
                    _wdctx.getGameDirector().tutorialEvent("furniInstalled");
                }
            };

            if (item.isUsed()) {
                var msg :String = Item.getTypeKey(itemType);
                (new ItemUsedDialog(_wdctx, Msgs.ITEM.get(msg), function () :void {
                    var confWrap :ConfirmAdapter = new ConfirmAdapter(
                        // failure function
                        function (cause :String) :void {
                            Log.getLog(this).debug(
                                "Failed to remove item from its current location " +
                                "[id=" + item.itemId + ", type=" + item.getType() +
                                ", cause=" + cause + "]");
                            _wdctx.displayInfo(MsoyCodes.EDITING_MSGS, "e.failed_to_remove");
                        }, useNewItem);
                    isvc.reclaimItem(_wdctx.getClient(), ident, confWrap);
                })).open(true);
            } else {
                useNewItem();
            }
        };

        isvc.peepItem(_wdctx.getClient(), ident, new ResultWrapper(
            function (cause :String) :void {
                _wdctx.displayFeedback(MsoyCodes.EDITING_MSGS, cause);
            }, gotItem));
    }

    /**
     * This is called from JavaScript to clear an item from use, be it furni, decor, a pet, etc.
     */
    public function clearItem (itemType :int, itemId :int) :void
    {
        if (itemType == Item.AVATAR) {
            _wdctx.getWorldDirector().setAvatar(0, 0);

        } else if (itemType == Item.PET) {
            // ensure this pet really is in this room
            for each (var pet :PetSprite in _roomView.getPets()) {
                if (pet.getItemIdent().itemId == itemId) {
                    handleOrderPet(itemId, Pet.ORDER_SLEEP);
                    break;
                }
            }

        } else if (itemType == Item.DECOR || itemType == Item.AUDIO) {
            var newScene :MsoyScene = _scene.clone() as MsoyScene;
            if (itemType == Item.DECOR) {
                var newSceneModel :MsoySceneModel = (newScene.getSceneModel() as MsoySceneModel);
                newSceneModel.decor = MsoySceneModel.defaultMsoySceneModelDecor();
                applyUpdate(new SceneUpdateAction(_wdctx, _scene, newScene));

            } else if (itemType == Item.AUDIO) {
                (newScene.getSceneModel() as MsoySceneModel).audioData.itemId = 0;
                applyUpdate(new SceneUpdateAction(_wdctx, _scene, newScene));
            }

        } else {
            for each (var furni :FurniData in _scene.getFurni()) {
                if (furni.itemType == itemType && furni.itemId == itemId) {
                    applyUpdate(new FurniUpdateAction(_wdctx, furni, null));
                    break;
                }
            }
        }
    }

    /**
     * Called over the GWT bridge so that the item browser can know which items are in the current
     * room.
     */
    public function getFurniList () :Array
    {
        var furnis :Array = [];
        for each (var furni :FurniData in _scene.getFurni()) {
            furnis.push([ furni.itemType, furni.itemId ]);
        }
        return furnis;
    }

    /**
     * End editing the room.
     */
    public function cancelRoomEditing () :void
    {
        _editor.endEditing();
    }

    /**
     * Applies a specified room update object to the current room.
     */
    public function applyUpdate (update :UpdateAction) :void
    {
        _updates.push(update);
    }

    /**
     * Undo the effects of the most recent update. Returns true if the update stack contains more
     * actions, false if it's become empty.
     */
    public function undoLastUpdate () :Boolean
    {
        _updates.pop();
        return _updates.length != 0;
    }

    /**
     * Set the hover for all furniture on or off.
     */
    public function hoverAllFurni (on :Boolean) :void
    {
        var sprite :FurniSprite;

        if (on) {
            for each (sprite in _roomView.getFurniSprites().values()) {
                if (!sprite.isActive() || !sprite.capturesMouse() || !sprite.hasAction()) {
                    continue;
                }
                var tipText :Object = sprite.setHovered(true);
                if (tipText is String) {
                    var p :Point = sprite.getLayoutHotSpot();
                    p = sprite.localToGlobal(p);
                    addHoverTip(sprite, String(tipText), p.x, p.y);
                }
            }
        } else {
            for each (sprite in _roomView.getFurniSprites().values()) {
                sprite.setHovered(false);
            }

            removeHoverTips();
        }
    }

    public function setBackgroundMusic (data :AudioData) :void
    {
        if (_wdctx.getWorldClient().isFeaturedPlaceView()) {
            return;
        }

        if (!_musicIsBackground) {
            if (_music.isPlaying()) {
                // don't disrupt the other music..
                return;

            } else {
                // oh, this other music is done. Sure, let's go for
                // the background music again
                _music.close();
                _music = null;
                _musicIsBackground = true;
            }
        }

        var isPathValid :Boolean = data.isInitialized() && data.media != null;
        var path :String = isPathValid ? data.media.getMediaPath() : null;

        // maybe shutdown old music
        // if _music is playing the right thing, let it keep on playing
        if (_music != null && _music.getURL() != path) {
            _music.close();
            _music = null;
        }
        // TODO: temporary hack to disable even the downloading of room music if sound is off
        const shouldEvenTry :Boolean = (Prefs.getSoundVolume() > 0);
        // set up new music, if needed
        if (shouldEvenTry && _music == null && isPathValid) {
            _music = new SoundPlayer(path);
            _music.addEventListener(Event.COMPLETE, musicFinishedPlaying);
            // TODO: we probably need to wait for COMPLETE
            _music.loop();
        }
        // set the volume, even if we're just re-setting it on
        // already-playing music
        if (_music != null) {
            _music.setVolume(data.volume);
        }
    }

    /**
     * Do any needed clientside adjustments to the effect data.
     */
    public function adjustEffectData (effect :EffectData) :EffectData
    {
        switch (effect.actionType) {
        default:
            log.warning("Unhandled EffectData parameter mode: " + effect.actionType);
            Log.dumpStack();
            // fall through to MODE_NONE...

        case EffectData.MODE_NONE:
            return effect;

        case EffectData.MODE_XLATE:
            effect.actionData = Msgs.GENERAL.xlate(effect.actionData);
            break;
        }

        // set the mode to MODE_NONE to indicate that we've adjusted
        effect.actionType = EffectData.MODE_NONE;
        return effect;
    }

    // documentation inherited
    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);

        _wdctx = (ctx as WorldContext);
        _editor = new RoomEditorController(_wdctx, _roomView);

        if (_wdctx.getWorldClient().isFeaturedPlaceView()) {
            // show the pointer cursor
            _roomView.buttonMode = true;
            _roomView.mouseChildren = false;
            _roomView.useHandCursor = true;
        }
    }

    // documentation inherited
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _roomView = new RoomView(ctx as WorldContext, this);
        return _roomView;
    }

    // documentation inherited
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _roomObj = (plobj as RoomObject);
        _roomListener = new MessageAdapter(msgReceivedOnRoomObj);
        _roomObj.addListener(_roomListener);

        // get a copy of the scene
        _scene = (_wdctx.getSceneDirector().getScene() as MsoyScene);

        _snap = new SnapshotController(_wdctx, _scene.getId());

        _walkTarget.visible = false;
        _flyTarget.visible = false;
        _roomView.addChildAt(_flyTarget, _roomView.numChildren);
        _roomView.addChildAt(_walkTarget, _roomView.numChildren);

        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.addEventListener(MouseEvent.CLICK, mouseWillClick, true);
        _roomView.addEventListener(Event.ENTER_FRAME, checkMouse);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyEvent);

        // watch for when we're un-minimized and the display list is valid, so that we can open the
        // editor, and place things correctly when necessary
        _ctx.getClient().addEventListener(MsoyClient.MINI_WILL_CHANGE, miniWillChange);
    }

    // documentation inherited
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _updates.reset();
        if (isRoomEditing()) {
            cancelRoomEditing();
        }

        _ctx.getClient().removeEventListener(MsoyClient.MINI_WILL_CHANGE, miniWillChange);

        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.removeEventListener(MouseEvent.CLICK, mouseWillClick, true);
        _roomView.removeEventListener(Event.ENTER_FRAME, checkMouse);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyEvent);

        _roomView.removeChild(_walkTarget);
        _roomView.removeChild(_flyTarget);
        hoverAllFurni(false);
        setHoverSprite(null);

        _roomObj.removeListener(_roomListener);

        _snap = null;
        _scene = null;
        _roomObj = null;

        closeAllMusic(false);

        super.didLeavePlace(plobj);
    }

    /**
     * Does this client have control over the current AVRG?
     *
     * @returns true, false, or null if nobody currently has control.
     */
    public function hasAVRGameControl () :Object
    {
        var gameId :int = _wdctx.getGameDirector().getGameId();
        if (gameId == 0) {
            log.warning("Got AVRG control request, but we don't seem to be playing one.");
            return null;
        }

        var ctrl :EntityControl =
            _roomObj.controllers.get(new ControllableAVRGame(gameId)) as EntityControl;
        if (ctrl != null) {
            return ctrl.controllerOid == _wdctx.getMemberObject().getOid();
        }
        return null;
    }

    /**
     * Called when the client is minimized and unminimized.
     */
    protected function miniWillChange (event :ValueEvent) :void
    {
        if (!(event.value as Boolean)) {
            if (_openEditor && !isRoomEditing()) {
                beginRoomEditing();
            }
            _openEditor = false;
        }
    }

    /**
     * Close and reset all music.
     */
    protected function closeAllMusic (resumeBackground :Boolean) :void
    {
        if (_music != null && !(resumeBackground && _musicIsBackground)) {
            _music.close();
            _music = null;
            _musicIsBackground = true;
        }
        if (_loadingMusic != null) {
            _loadingMusic.close();
            _loadingMusic = null;
        }
        if (resumeBackground && _music == null) {
            setBackgroundMusic(_scene.getAudioData());
        }
    }

    /**
     * Create the menu item that allows a user to change their own avatar.
     */
    protected function createChangeAvatarMenu (us :MemberObject, canControl :Boolean) :Object
    {
        var avItems :Array = [];
        var avatars :Array = (us.avatarCache != null) ? us.avatarCache.toArray() : [];
        ArrayUtil.sort(avatars);
        var iconW :Number = 20;
        var iconH :Number = 20; // 1/3 of thumbnail height
        for (var ii :int = 0; ii < avatars.length; ii++) {
            var av :Avatar = avatars[ii] as Avatar;
            avItems.push({ label: av.name, enabled: !av.equals(us.avatar),
                iconObject: MediaWrapper.createScaled(av.getThumbnailMedia(), iconW, iconH),
                callback: _wdctx.getWorldDirector().setAvatar, arg: av.itemId });
        }
        // add defaults
        avItems.push({ label: Msgs.ITEM.get("m.default"), enabled: (us.avatar != null),
            iconObject: MediaWrapper.createScaled(
                Avatar.getDefaultMemberAvatarMedia(), iconW, iconH),
            callback: _wdctx.getWorldDirector().setAvatar, arg: 0 });

        avItems.push({ type: "separator" });
        avItems.push({ label: Msgs.GENERAL.get("b.avatars_full"),
            command: WorldController.VIEW_MY_AVATARS,
            enabled: !_wdctx.getWorldClient().isEmbedded() });

        // return a menu item for changing their avatar
        return { label: Msgs.GENERAL.get("b.change_avatar"), children: avItems,
            enabled: canControl };
    }

    /**
     * Begins editing the room.
     */
    protected function beginRoomEditing () :void
    {
        _walkTarget.visible = false;
        _flyTarget.visible = false;
        setHoverSprite(null);

        // put the room edit button in the selected state
        var roomEditBtn :Button =
            (_wdctx.getTopPanel().getControlBar() as WorldControlBar).roomEditBtn;
        roomEditBtn.selected = true;

        // this function will be called when the edit panel is closing
        var wrapupFn :Function = function () :void {
            if (_music != null && ! _musicIsBackground) {
                _music.play(); // restart non-background music
            }
            roomEditBtn.selected = false;
        }

        if (_music != null && ! _musicIsBackground) {
            _music.close();    // stop non-background music
            _music = null;
            _musicIsBackground = true;
        }

        _editor.startEditing(wrapupFn);
        _editor.updateUndoStatus(_updates.length != 0);
    }

    /**
     * Sends the entire array of room edits to the server.
     *
     * @param updates a TypedArray containing instances of SceneUpdate object.
     */
    protected function updateRoom (updates :TypedArray /* of SceneUpdate */) :void
    {
        _roomObj.roomService.updateRoom(_wdctx.getClient(), updates, new ReportingListener(_wdctx));
    }

    /**
     * Handles ENTER_FRAME and see if the mouse is now over anything.  Normally the flash player
     * will dispatch mouseOver/mouseLeft for an object even if the mouse isn't moving: the sprite
     * could move.  Since we're hacking in our own mouseOver handling, we emulate that.  Gah.
     */
    protected function checkMouse (event :Event) :void
    {
        // no mouse fiddling while we're minimized
        if (_wdctx.getTopPanel().isMinimized() || _wdctx.getWorldClient().isFeaturedPlaceView()) {
            setHoverSprite(null);
            return;
        }

        // in case a mouse event was captured by an entity, prevent it from adding a popup later
        _entityAllowedToPop = null;

        // freak not out if we're temporarily removed from the stage
        if (_roomView.stage == null || !_roomView.isShowing()) {
            return;
        }

        var sx :Number = _roomView.stage.mouseX;
        var sy :Number = _roomView.stage.mouseY;
        var showWalkTarget :Boolean = false;
        var showFlyTarget :Boolean = false;
        var hoverTarget :MsoySprite = null;

        // if shift is being held down, we're looking for locations only, so
        // skip looking for hitSprites.
        var hit :* = (_shiftDownSpot == null) ? getHitSprite(sx, sy, isRoomEditing()) : null;
        var hitter :MsoySprite = (hit as MsoySprite);
        // ensure we hit no pop-ups
        if (hit !== undefined) {
            hoverTarget = hitter;
            if (hitter == null) {
                var cloc :ClickLocation = _roomView.layout.pointToAvatarLocation(
                    sx, sy, _shiftDownSpot, RoomMetrics.N_UP);

                if (cloc != null && _wdctx.worldProps.userControlsAvatar) {
                    addAvatarYOffset(cloc);
                    if (cloc.loc.y != 0) {
                        _flyTarget.setLocation(cloc.loc);
                        _roomView.layout.updateScreenLocation(_flyTarget);
                        showFlyTarget = true;

                        // Set the Y to 0 and use it for the walkTarget
                        cloc.loc.y = 0;
                        _walkTarget.alpha = .5;

                    } else {
                        _walkTarget.alpha = 1;
                    }

                    // don't show the walk target if we're "in front" of the room view
                    showWalkTarget = (cloc.loc.z >= 0);
                    _walkTarget.setLocation(cloc.loc);
                    _roomView.layout.updateScreenLocation(_walkTarget);
                }

            } else if (!hoverTarget.hasAction()) {
                // it may have captured the mouse, but it doesn't actually
                // have any action, so we don't hover it.
                hoverTarget = null;
            }

            // if we're editing the room, don't highlight any furni at all,
            if (isRoomEditing()) {
                hoverTarget = null;

                // let the editor override our decision to display walk targets
                showWalkTarget = (showWalkTarget && _editor.isMovementEnabled());
                showFlyTarget = (showFlyTarget && _editor.isMovementEnabled());

                // and tell the editor which sprite was being hovered (whether highlighted or not)
                _editor.mouseOverSprite(hitter);
            }

        }

        _walkTarget.visible = showWalkTarget;
        _flyTarget.visible = showFlyTarget;

        setHoverSprite(hoverTarget, sx, sy);
    }

    /**
     * Set the sprite that the mouse is hovering over.
     */
    protected function setHoverSprite (
        sprite :MsoySprite, stageX :Number = 0, stageY :Number = 0) :void
    {
        // if the same sprite is glowing, we don't have to change as much..
        if (_hoverSprite == sprite) {
            updateHovered(stageX, stageY);
            if (_hoverSprite != null) {
                // but we do want to tell it about it, in case it wants
                // to glow differently depending on the location...
                _hoverSprite.setHovered(true, stageX, stageY);
            }
            return;
        }

        // otherwise, unglow the old sprite (and remove any tooltip)
        if (_hoverSprite != null) {
            _hoverSprite.setHovered(false);
            removeHoverTips();
        }

        // assign the new hoversprite
        _hoverSprite = sprite;

        // and glow the new hoversprite
        updateHovered(stageX, stageY);
    }

    /**
     * Update the hovered status of the current _hoverSprite.
     */
    protected function updateHovered (stageX :Number, stageY :Number) :void
    {
        if (_hoverSprite == null) {
            return;
        }

        var text :Object = _hoverSprite.setHovered(true, stageX, stageY);
        if (text === true) {
            return;
        }
        var tip :IToolTip = (_hoverTips.length == 1) ? IToolTip(_hoverTips[0]) : null;
        if (tip != null && tip.text != text) {
            removeHoverTips();
            tip = null;
        }
        if (tip == null && text != null) {
            addHoverTip(_hoverSprite, String(text), stageX, stageY);
        }
    }

    /**
     * Utility method to create and style the hover tip for a sprite.
     */
    protected function addHoverTip (
        sprite :MsoySprite, tipText :String, stageX :Number, stageY :Number) :void
    {
        var tip :IToolTip = ToolTipManager.createToolTip(tipText, stageX, stageY);
        var tipComp :UIComponent = UIComponent(tip);
        tipComp.styleName = "roomToolTip";
        tipComp.x -= tipComp.width/2;
        tipComp.y -= tipComp.height/2;
        var hoverColor :uint = sprite.getHoverColor();
        tipComp.setStyle("color", hoverColor);
        if (hoverColor == 0) {
            tipComp.setStyle("backgroundColor", 0xFFFFFF);
        }

        _hoverTips.push(tip);
    }

    /**
     * Remove all currently-shown hover tips. Does not unglow the sprites.
     */
    protected function removeHoverTips () :void
    {
        for each (var tip :IToolTip in _hoverTips) {
            ToolTipManager.destroyToolTip(tip);
        }
        _hoverTips.length = 0; // truncate
    }

    protected function mouseWillClick (event :MouseEvent) :void
    {
        if (_shiftDownSpot != null || isRoomEditing()) {
            return;
        }

        // this method is called for the CAPTURE phase of the MouseEvent, the entity
        // has not yet received the click, but we see which it was to see if we'll allow
        // the popup.
        _entityAllowedToPop = getHitSprite(event.stageX, event.stageY, false);
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
        // if we're in a featured place view, any click should take the member to this room.
        if (_wdctx.getWorldClient().isFeaturedPlaceView()) {
            _wdctx.getWorldController().handleGoScene(_scene.getId());
            return;
        }

        // at this point, the mouse click is bubbling back out, and the entity is no
        // longer allowed to pop up a popup.
        _entityAllowedToPop = null;

        // if shift is being held down, we're looking for locations only, so skip looking for
        // hitSprites.
        var hit :* = (_shiftDownSpot == null) ?
            getHitSprite(event.stageX, event.stageY, isRoomEditing()) : null;
        if (hit === undefined) {
            return;
        }

        // deal with the target
        var hitter :MsoySprite = (hit as MsoySprite);
        if (hitter != null) {
            // let the sprite decide what to do with it
            hitter.mouseClick(event);

        } else if (_wdctx.worldProps.userControlsAvatar) {
            var curLoc :MsoyLocation = _roomView.getMyCurrentLocation();
            if (curLoc == null) {
                return; // we've already left, ignore the click
            }

            // calculate where the location is
            var cloc :ClickLocation = _roomView.layout.pointToAvatarLocation(
                event.stageX, event.stageY, _shiftDownSpot, RoomMetrics.N_UP);
            // disallow clicking in "front" of the scene when minimized
            if (cloc != null && cloc.loc.z >= 0) {
                // orient the location as appropriate
                addAvatarYOffset(cloc);
                var newLoc :MsoyLocation = cloc.loc;
                var degrees :Number = 180 / Math.PI *
                    Math.atan2(newLoc.z - curLoc.z, newLoc.x - curLoc.x);
                // we rotate so that 0 faces forward
                newLoc.orient = (degrees + 90 + 360) % 360;
                _wdctx.getSpotSceneDirector().changeLocation(newLoc, null);
            }
        }
    }

    protected function keyEvent (event :KeyboardEvent) :void
    {
        try {
            var keyDown :Boolean = event.type == KeyboardEvent.KEY_DOWN;
            switch (event.keyCode) {
            case Keyboard.F4:
                _roomView.dimAvatars(keyDown);
                return;

            case Keyboard.F5:
                _roomView.dimFurni(keyDown);
                return;

            case Keyboard.F6:
                _roomView.getChatOverlay().setClickableGlyphs(keyDown);
                return;

            case Keyboard.SHIFT:
                _shiftDown = keyDown;
                if (keyDown) {
                    if (_walkTarget.visible && (_shiftDownSpot == null)) {
                        // record the y position at this
                        _shiftDownSpot = new Point(_roomView.stage.mouseX, _roomView.stage.mouseY);
                    }

                } else {
                    _shiftDownSpot = null;
                }
                return;
            }

            if (keyDown) {
                switch (event.charCode) {
                case 91: // '['
                    _roomView.scrollViewBy(-ROOM_SCROLL_INCREMENT);
                    break;

                case 93: // ']'
                    _roomView.scrollViewBy(ROOM_SCROLL_INCREMENT);
                    break;
                }
            }

        } finally {
            event.updateAfterEvent();
        }
    }

    /**
     * Return the avatar's preferred y offset for normal mouse positioning,
     * unless shift is being held down, in which case use 0 so that the user
     * can select their height precisely.
     */
    protected function addAvatarYOffset (cloc :ClickLocation) :void
    {
        if (_shiftDownSpot != null) {
            return;
        }

        var av :MemberSprite = _roomView.getMyAvatar();
        if (av != null) {
            var prefY :Number = av.getPreferredY() / _roomView.layout.metrics.sceneHeight;
            // but clamp their preferred Y into the normal range
            cloc.loc.y = Math.min(1, Math.max(0, prefY));
        }
    }

    /**
     * Called when a message is received on the room object.
     */
    protected function msgReceivedOnRoomObj (event :MessageEvent) :void
    {
        var args :Array = event.getArgs();
        switch (event.getName()) {
        case RoomObject.LOAD_MUSIC:
            if (_wdctx.getWorldClient().isFeaturedPlaceView()) {
                break;
            }
            if (_loadingMusic != null) {
                _loadingMusic.close();
            }
            _loadingMusic = new SoundPlayer(String(args[0]));
            // TODO: dispatched MUSIC_LOADED back...
            break;

        case RoomObject.PLAY_MUSIC:
            if (args == null || args.length == 0) {
                closeAllMusic(true);
                break;
            }
            var url :String = (args[0] as String);
            if (_loadingMusic != null) {
                if (_loadingMusic.getURL() == url) {
                    // awesome
                    if (_music != null) {
                        _music.close();
                    }
                    _music = _loadingMusic;
                    _loadingMusic = null;
                    _musicIsBackground = false;
                    _music.addEventListener(Event.COMPLETE, musicFinishedPlaying);
                    _music.play();

                } else {
                    log.warning("Asked to play music different from loaded? " +
                        "[loaded=" + _loadingMusic.getURL() +
                        ", toPlay=" + url + "].");
                }
            }
            break;

        case RoomObject.ADD_EFFECT:
            addTransientEffect(args[0] as int /*bodyOid*/, args[1] as EffectData);
            break;
        }
    }

    /**
     * Callback when the music finishes.
     */
    protected function musicFinishedPlaying (... ignored) :void
    {
        _roomObj.manager.invoke(RoomObject.MUSIC_ENDED, _music.getURL());
    }

    /**
     * Add a transient effect to an actor sprite.
     */
    protected function addTransientEffect (bodyOid :int, effect :EffectData) :void
    {
        var actor :OccupantSprite = _roomView.getOccupant(bodyOid);
        if (actor != null) {
            actor.addTransientEffect(adjustEffectData(effect));

        } else {
            log.info("Unable to find actor for transient effect [bodyOid=" + bodyOid + "].");
        }
    }

    /**
     * Ensures that we can issue a request to update the distributed state of the specified item,
     * returning true if so, false if we don't yet have a room object or are not in control of that
     * item.
     */
    protected function checkCanRequest (ident :ItemIdent, from :String) :Boolean
    {
        if (_roomObj == null) {
            log.warning("Cannot issue request for lack of room object [from=" + from +
                        ", ident=" + ident + "].");
            return false;
        }

        // make sure we are in control of this entity (or that no one has control)
        var result :Object = hasEntityControl(ident);
        if (result == null || result == true) {
            // it's ok if nobody has control
            return true;
        }

        log.info("Dropping request as we are not controller [from=" + from +
                 ", item=" + ident + "].");
        return false;
    }

    /**
     * Does this client have control over the specified entity?
     *
     * Side-effect: The gotControl() will always be re-dispatched to the entity if it does.
     * The newest EntityControl will suppress repeats.
     *
     * @returns true, false, or null if nobody currently has control.
     */
    protected function hasEntityControl (ident :ItemIdent) :Object
    {
        var ourOid :int = _wdctx.getMemberObject().getOid();

        // first, let's check all the MemberInfos
        for each (var occInfo :Object in _roomObj.occupantInfo.toArray()) {
            if (occInfo is MemberInfo) {
                var winfo :MemberInfo = (occInfo as MemberInfo);
                if (ident.equals(winfo.getItemIdent())) {
                    if (winfo.bodyOid == ourOid) {
                        // dispatch got-control to the avatar, it should supress repeats
                        _roomView.dispatchEntityGotControl(ident);
                        return true;

                    } else {
                        return false; // we can't control another's avatar!
                    }
                }
            }
        }
        // ok, the ident does not belong to a member's avatar..

        var ctrl :EntityControl =
            _roomObj.controllers.get(new ControllableEntity(ident)) as EntityControl;
        if (ctrl == null) {
            return null;

        } else if (ctrl.controllerOid == ourOid) {
            // redispatch that we have control, just in case the media
            // started up after the last dispatch...
            _roomView.dispatchEntityGotControl(ident);
            return true;

        } else {
            return false;
        }
    }

   override protected function sceneUpdated (update :SceneUpdate) :void
    {
        if (update is SceneAttrsUpdate) {
            var attrsUpdate :SceneAttrsUpdate = update as SceneAttrsUpdate;
            var newId :int = attrsUpdate.decor.itemId;
            var oldId :int = _scene.getDecor().itemId;
            if (newId != oldId) {
                _wdctx.getWorldClient().dispatchEventToGWT(BACKGROUND_CHANGED_EVENT,
                    [ Item.DECOR, newId, oldId ]);
            }
            newId = attrsUpdate.audioData.itemId;
            oldId = _scene.getAudioData().itemId;
            if (newId != oldId) {
                _wdctx.getWorldClient().dispatchEventToGWT(BACKGROUND_CHANGED_EVENT,
                    [ Item.AUDIO, newId, oldId ]);
            }
        } else if (update is ModifyFurniUpdate) {
            var args :Array = [ [], [] ];
            var updates :Array = [ (update as ModifyFurniUpdate).furniAdded,
                (update as ModifyFurniUpdate).furniRemoved ];
            for (var ii :int = 0; ii < updates.length; ii++) {
                for each (var furni :FurniData in updates[ii]) {
                    args[ii].push([ furni.itemType, furni.itemId ]);
                }
            }
            _wdctx.getWorldClient().dispatchEventToGWT(FURNI_CHANGED_EVENT, args);
        }

        super.sceneUpdated(update);
        _roomView.processUpdate(update);
        _editor.processUpdate(update);
    }

    /**
     * Called from user code to show a custom popup.
     */
    internal function showEntityPopup (
        sprite :MsoySprite, title :String, panel :DisplayObject, w :Number, h :Number,
        color :uint = 0xFFFFFF, alpha :Number = 1.0, mask :Boolean = true) :Boolean
    {
//        if (_entityAllowedToPop != sprite) {
//            return false;
//        }

        // other people's avatars cannot put a popup on our screen...
        if ((sprite is MemberSprite) && (sprite != _roomView.getMyAvatar())) {
            return false;
        }

        if (isNaN(w) || isNaN(h) || w <= 0 || h <= 0 || title == null || panel == null) {
            return false;
        }

        // close any existing popup
        if (_entityPopup != null) {
            _entityPopup.close();
        }

        _entityPopup = new EntityPopup(_wdctx, sprite, this, title, panel, w, h, color, alpha, mask);
        _entityPopup.open();
        return true;
    }

    /**
     * Clear any popup belonging to the specified sprite.
     */
    internal function clearEntityPopup (sprite :MsoySprite) :void
    {
        if (_entityPopup != null && _entityPopup.getOwningEntity() == sprite) {
            _entityPopup.close(); // will trigger callback that clears _entityPopup
        }
    }

    /**
     * A callback from the EntityPopup to let us know that it's been closed.
     */
    internal function entityPopupClosed () :void
    {
        _entityPopup = null;
    }

    /** The number of pixels we scroll the room on a keypress. */
    protected static const ROOM_SCROLL_INCREMENT :int = 20;

    /** The event to send to GWT when a background property has changed. */
    protected static const BACKGROUND_CHANGED_EVENT :String = "backgroundChanged";

    /** The event to send to GWT when furni has been added or removed. */
    protected static const FURNI_CHANGED_EVENT :String = "furniChanged";

    /** The life-force of the client. */
    protected var _wdctx :WorldContext;

    /** The room view that we're controlling. */
    protected var _roomView :RoomView;

    protected var _roomObj :RoomObject;

    /** Our general-purpose room listener. */
    protected var _roomListener :ChangeListener;

    /** The currently hovered sprite, or null. */
    protected var _hoverSprite :MsoySprite;

    /** All currently displayed sprite tips. */
    protected var _hoverTips :Array = [];

    /** True if the shift key is currently being held down, false if not. */
    protected var _shiftDown :Boolean;

    /** If shift is being held down, the coordinates at which it was pressed. */
    protected var _shiftDownSpot :Point;

    /** The music currently playing in the scene, which may or may not be
     * background music. */
    protected var _music :SoundPlayer;

    /** True if _music is the room's background music. Otherwise
     * The music playing is from some other source. */
    protected var _musicIsBackground :Boolean = true;

    /** Holds loading alternate music. Once triggered to play,
     * it's shifted to _music. */
    protected var _loadingMusic :SoundPlayer;

    /** The current scene we're viewing. */
    protected var _scene :MsoyScene;

    /** The "cursor" used to display that a location is walkable. */
    protected var _walkTarget :WalkTarget = new WalkTarget();

    protected var _flyTarget :WalkTarget = new WalkTarget(true);

    protected var _entityPopup :EntityPopup;

    protected var _entityAllowedToPop :MsoySprite;

    /** Controller for in-room furni editing. */
    protected var _editor :RoomEditorController;

    /** Controller for room snapshots. */
    protected var _snap :SnapshotController;

    /** Stack that stores the sequence of room updates. */
    protected var _updates :UpdateStack = new UpdateStack(updateRoom);

    /** A flag to indicate that the room editor should be opened when the view is un-minimized */
    protected var _openEditor :Boolean = false;
}
}

import flash.display.DisplayObject;
import flash.display.Sprite;

import com.threerings.msoy.world.client.RoomElement;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.RoomCodes;

class WalkTarget extends Sprite
    implements RoomElement
{
    public function WalkTarget (fly :Boolean = false)
    {
        var targ :DisplayObject = (fly ? new FLYTARGET() : new WALKTARGET() as DisplayObject);
        targ.x = -targ.width/2;
        targ.y = -targ.height/2;
        addChild(targ);
    }

    // from RoomElement
    public function setLocation (newLoc :Object) :void
    {
        _loc.set(newLoc);
    }

    // from RoomElement
    public function getLocation () :MsoyLocation
    {
        return _loc;
    }

    // from RoomElement
    public function getLayoutType () :int
    {
        return RoomCodes.LAYOUT_NORMAL;
    }

    // from RoomElement
    public function getRoomLayer () :int
    {
        return RoomCodes.FURNITURE_LAYER;
    }

    // from RoomElement
    public function setScreenLocation (x :Number, y :Number, scale :Number) :void
    {
        this.x = x
        this.y = y

        // don't let the target shrink too much - 0.25 of original size at most
        var clampedScale :Number = Math.max(0.25, scale);
        this.scaleX = clampedScale;
        this.scaleY = clampedScale;
    }

    /** Our logical location. */
    protected const _loc :MsoyLocation = new MsoyLocation();

    [Embed(source="../../../../../../../rsrc/media/walkable.swf")]
    protected static const WALKTARGET :Class;

    [Embed(source="../../../../../../../rsrc/media/flyable.swf")]
    protected static const FLYTARGET :Class;
}
