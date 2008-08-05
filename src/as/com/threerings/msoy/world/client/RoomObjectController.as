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

import com.threerings.util.ArrayUtil;
import com.threerings.util.ClassUtil;
import com.threerings.util.Integer;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.ObjectMarshaller;
import com.threerings.util.ValueEvent;

import com.threerings.flash.MenuUtil;
import com.threerings.flex.CommandMenu;
import com.threerings.flex.PopUpUtil;

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

import com.threerings.msoy.client.BootablePlaceController;
import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.LogonPanel;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.Audio;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.Pet;

import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.msoy.world.client.editor.DoorTargetEditController;
import com.threerings.msoy.world.client.editor.ItemUsedDialog;
import com.threerings.msoy.world.client.editor.RoomEditorController;
import com.threerings.msoy.world.client.snapshot.SnapshotController;
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
import com.threerings.msoy.world.data.EntityMemoryEntry;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.FurniUpdate_Add;
import com.threerings.msoy.world.data.FurniUpdate_Remove;
import com.threerings.msoy.world.data.MemberInfo;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.MsoySceneModel;
import com.threerings.msoy.world.data.PetInfo;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.RoomPropertyEntry;
import com.threerings.msoy.world.data.SceneAttrsUpdate;

import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.RadialMenu;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.chat.client.ReportingListener;

/**
 * Manages the various interactions that take place in a room scene.
 */
public class RoomObjectController extends RoomController
    implements BootablePlaceController
{
    /** Some commands */
    public static const EDIT_DOOR :String = "EditDoor";

    // documentation inherited
    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);

        if (UberClient.isFeaturedPlaceView()) {
            _suppressNormalHovering = true;
            // show the pointer cursor
            _roomView.buttonMode = true;
            _roomView.mouseChildren = false;
            _roomView.useHandCursor = true;
        }
    }

    // documentation inherited
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _roomObjectView = new RoomObjectView(_wdctx, this);
        _roomView = _roomObjectView;
        return _roomObjectView;
    }

    // from interface BootablePlaceController
    public function canBoot () :Boolean
    {
        return canManageRoom();
    }

    /**
     * Set the specified name hovered or unhovered.
     */
    public function setHoverName (name :MemberName, hovered :Boolean) :void
    {
        setHoverSprite(hovered ? _roomObjectView.getOccupantByName(name) : null);
        _suppressNormalHovering = hovered;
    }

    override public function getViewerName (instanceId :int = 0) :String
    {
        var name :String = super.getViewerName(instanceId);
        if (name == null) {
            // look for the name in the OccupantInfos
            for each (var obj :Object in _roomObj.occupantInfo.toArray()) {
                var memInfo :MemberInfo = obj as MemberInfo;
                if (memInfo != null && memInfo.getMemberId() == instanceId) {
                    name = memInfo.username.toString();
                    break;
                }
            }
        }
        return name;
    }

    /**
     * Requests that this client be given control of the specified item.
     */
    override public function requestControl (ident :ItemIdent) :void
    {
        var result :Object = hasEntityControl(ident);
        // side-effect of calling hasEntityControl: the sprite will be notified (possibly again)
        // that it has control if it does
        if (result == null) {
            // only if nobody currently has control do we issue the request
            _roomObj.roomService.requestControl(_wdctx.getClient(), ident);
        }
    }

    /**
     * Handles a request by an actor to change its location. Returns true if the request was
     * dispatched, false if funny business prevented it.
     */
    override public function requestMove (ident :ItemIdent, newloc :MsoyLocation) :Boolean
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
     * Returns true if we are in edit mode, false if not.
     */
    public function isRoomEditing () :Boolean
    {
        return (_editor != null) && _editor.isEditing();
    }

    /**
     * Toggle the display of the snapshot panel.
     */
    public function toggleSnapshotPanel () :void
    {
        const ctrl :SnapshotController = getSnapshotController();
        if (ctrl.isShowing()) {
            ctrl.destroyPanel();
        } else {
            ctrl.showPanel();
        }
    }

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
    override public function handleFurniClicked (furni :FurniData) :void
    {
        switch (furni.actionType) {
        case FurniData.ACTION_URL:
            _wdctx.getMsoyController().showExternalURL(furni.splitActionData()[0] as String);
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
    override public function handleAvatarClicked (avatar :MemberSprite) :void
    {
        var occInfo :MemberInfo = (avatar.getActorInfo() as MemberInfo);
        if (occInfo == null) {
            log.info("Clicked on non-MemberInfo sprite [info=" + avatar.getActorInfo() + "].");
            return;
        }

        var us :MemberObject = _wdctx.getMemberObject();
        var menuItems :Array = [];

        // add the standard menu items
        _wdctx.getMsoyController().addMemberMenuItems(occInfo.username as MemberName, menuItems);

        // then add our custom menu items
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

            // if we're not a guest add a menu for changing avatars
            if (!us.isGuest()) {
                menuItems.push(createChangeAvatarMenu(us, canControl));
            }

            // add our custom menu items (avatar actions and states)
            addSelfMenuItems(avatar, menuItems, canControl);

            if (_wdctx.getWorldClient().isEmbedded()) {
                if (us.isGuest()) {
                    menuItems.push({ label: Msgs.GENERAL.get("b.logon"),
                        callback: function () :void {
                            (new LogonPanel(_wdctx)).open();
                        }});
                } else {
                    var creds :MsoyCredentials = new MsoyCredentials(null, null);
                    creds.ident = "";
                    menuItems.push({ label: Msgs.GENERAL.get("b.logout"), 
                        command: MsoyController.LOGON, arg: creds });
                }
            }

        } else {
            // create a menu for clicking on someone else
            var memId :int = occInfo.getMemberId();

            if (!MemberName.isGuest(memId)) {
                // TODO: move up when we can forward MemberObjects between servers for guests
                menuItems.push({ label: Msgs.GENERAL.get("l.invite_follow"),
                                 callback: inviteFollow, arg: occInfo.username });
                menuItems.push({ label: Msgs.GENERAL.get("l.visit_home"),
                                 command: WorldController.GO_MEMBER_HOME, arg: memId });

                var kind :String = Msgs.GENERAL.get(avatar.getDesc());
                var flagItems :Array = [];

                if (avatar.isBlockable()) {
                    var key :String = avatar.isBlocked() ? "b.unbleep_item" : "b.bleep_item";
                    flagItems.push({ label: Msgs.GENERAL.get(key, kind),
                                     callback: avatar.toggleBlocked, arg: _wdctx });
                }

                var ident :ItemIdent = avatar.getItemIdent();
                if (ident != null && ident.type >= 0) { // -1 is used for the default avatar, etc
                    flagItems.push({ label: Msgs.GENERAL.get("b.view_item", kind),
                                     command: MsoyController.VIEW_ITEM, arg: ident });
                }

                if (flagItems.length > 0) {
                    menuItems.push({ type: "separator"},
                                   { label: Msgs.GENERAL.get("l.item_menu", kind),
                                     children: flagItems });
                }
            }
        }

        popActorMenu(avatar, menuItems);
    }

    /** 
     * Create the menu item that allows a user to change their own avatar.
     */     
    protected function createChangeAvatarMenu (us :MemberObject, canControl :Boolean) :Object
    {       
        var avItems :Array = [];
        var avatars :Array = (us.avatarCache != null) ? us.avatarCache.toArray() : [];
        ArrayUtil.sort(avatars);
        for (var ii :int = 0; ii < avatars.length; ii++) {
            var av :Avatar = avatars[ii] as Avatar;
            avItems.push({ label: av.name, enabled: !av.equals(us.avatar),
                iconObject: MediaWrapper.createView(
                    av.getThumbnailMedia(), MediaDesc.QUARTER_THUMBNAIL_SIZE),
                callback: _wdctx.getWorldDirector().setAvatar, arg: av.itemId });
        }
        // add defaults
        avItems.push({ label: Msgs.ITEM.get("m.default"), enabled: (us.avatar != null),
            iconObject: MediaWrapper.createView(
                Item.getDefaultThumbnailMediaFor(Item.AVATAR), MediaDesc.QUARTER_THUMBNAIL_SIZE),
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
     * Handles PET_CLICKED.
     */
    override public function handlePetClicked (pet :ActorSprite) :void
    {
        var occInfo :PetInfo = (pet.getActorInfo() as PetInfo);
        if (occInfo == null) {
            log.warning("Pet has unexpected ActorInfo [info=" + pet.getActorInfo() + "].");
            return;
        }

        var memObj :MemberObject = _wdctx.getMemberObject();
        var isPetOwner :Boolean = (occInfo.getOwnerId() == memObj.getMemberId());
        if (!isPetOwner && !canManageRoom()) {
            return;
        }

        var petId :int = occInfo.getItemIdent().itemId;

        var menuItems :Array = [];
        if (isPetOwner) {
            var isWalking :Boolean = (memObj.walkingId != 0);
            menuItems.push(
            { label: Msgs.GENERAL.get("b.order_pet_stay"),
              command: ORDER_PET, arg: [ petId, Pet.ORDER_STAY ], enabled: canManageRoom() },
            { label: Msgs.GENERAL.get("b.order_pet_follow"),
              command: ORDER_PET, arg: [ petId, Pet.ORDER_FOLLOW ], enabled: !isWalking },
            { label: Msgs.GENERAL.get("b.order_pet_go_home"),
              command: ORDER_PET, arg: [ petId, Pet.ORDER_GO_HOME ] });
        }
        // and any old room manager can put the pet to sleep
        menuItems.push({ label: Msgs.GENERAL.get("b.order_pet_sleep"),
            command: ORDER_PET, arg: [ petId, Pet.ORDER_SLEEP ] });

        popActorMenu(pet, menuItems);
    }

    /**
     * Handles ORDER_PET.
     */
    override public function handleOrderPet (petId :int, command :int) :void
    {
        var svc :PetService = (_wdctx.getClient().requireService(PetService) as PetService);
        svc.orderPet(_wdctx.getClient(), petId, command,
            new ReportingListener(_wdctx, MsoyCodes.GENERAL_MSGS, null, "m.pet_ordered" + command));
    }

    override public function getMemories (ident :ItemIdent) :Object
    {
        var mems :Object = {};
        for each (var entry :EntityMemoryEntry in _roomObj.memories.toArray()) {
            // filter out memories with null as the value, those will not be persisted
            if (entry.value != null && entry.item.equals(ident)) {
                mems[entry.key] = ObjectMarshaller.decode(entry.value);
            }
        }
        return mems;
    }

    override public function lookupMemory (ident :ItemIdent, key :String) :Object
    {
        var mkey :EntityMemoryEntry = new EntityMemoryEntry(ident, key, null);
        var entry :EntityMemoryEntry = _roomObj.memories.get(mkey) as EntityMemoryEntry;
        return (entry == null) ? null : ObjectMarshaller.decode(entry.value);
    }

    override public function getRoomProperties () :Object
    {
        var props :Object = {};
        for each (var entry :RoomPropertyEntry in _roomObj.roomProperties.toArray()) {
            props[entry.key] = ObjectMarshaller.decode(entry.value);
        }
        return props;
    }

    override public function getRoomProperty (key :String) :Object
    {
        var entry :RoomPropertyEntry = _roomObj.roomProperties.get(key) as RoomPropertyEntry;
        return (entry == null) ? null : ObjectMarshaller.decode(entry.value);
    }

    override public function canManageRoom () :Boolean
    {
        return (_scene != null && _scene.canManage(_wdctx.getMemberObject()));
    }

    override public function deleteItem (ident :ItemIdent) :void
    {
        var svc :ItemService = _wdctx.getClient().requireService(ItemService) as ItemService;

        svc.deleteItem(_wdctx.getClient(), ident, new ConfirmAdapter(
            function (cause :String) :void {
                Log.getLog(this).debug(
                    "Failed to delete item " +
                    "[type=" + ident.type + ", itemId=" + ident.itemId +
                    ", cause=" + cause + "]");
            }));
    }

    /**
     * End editing the room.
     */
    public function cancelRoomEditing () :void
    {
        _editor.endEditing();
        _editor = null;
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
     * This is called from JavaScript to select this room's decor, audio, or add a piece of furni.
     */
    public function useItem (itemType :int, itemId :int) :void
    {
        if (itemType == Item.AVATAR) {
            // NOTE: this is not used. GWT calls WorldClient.externalUseAvatar, but
            // we may as well handle this case
            _wdctx.getWorldDirector().setAvatar(itemId);
            return;
        }

        if (itemType == Item.PET) {
            var svc :PetService = _ctx.getClient().requireService(PetService) as PetService;
            svc.callPet(_wdctx.getClient(), itemId,
                        new ReportingListener(_wdctx, MsoyCodes.GENERAL_MSGS, null,
                                              "m.pet_called"));
            return;
        }

        if (!canManageRoom()) {
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
            // NOTE: this is not used. GWT calls WorldClient.externalUseAvatar, but
            // we may as well handle this case
            _wdctx.getWorldDirector().setAvatar(0);

        } else if (itemType == Item.PET) {
            // ensure this pet really is in this room
            for each (var pet :PetSprite in _roomObjectView.getPets()) {
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
     * Set the hover for all furniture on or off.
     */
    public function hoverAllFurni (on :Boolean) :void
    {
        var sprite :FurniSprite;

        if (on) {
            for each (sprite in _roomObjectView.getFurniSprites().values()) {
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
            for each (sprite in _roomObjectView.getFurniSprites().values()) {
                sprite.setHovered(false);
            }

            removeHoverTips();
        }
    }

    public function setBackgroundMusic (data :AudioData) :void
    {
        if (!UberClient.isRegularClient()) {
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
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _roomObj = (plobj as RoomObject);
        _roomListener = new MessageAdapter(msgReceivedOnRoomObj);
        _roomObj.addListener(_roomListener);

        // get a copy of the scene
        _scene = (_wdctx.getSceneDirector().getScene() as MsoyScene);

        _walkTarget.visible = false;
        _flyTarget.visible = false;
        _roomView.addChildAt(_flyTarget, _roomView.numChildren);
        _roomView.addChildAt(_walkTarget, _roomView.numChildren);

        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.addEventListener(Event.ENTER_FRAME, checkMouse, false, int.MIN_VALUE);
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
        _roomView.removeEventListener(Event.ENTER_FRAME, checkMouse);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyEvent);

        _roomView.removeChild(_walkTarget);
        _roomView.removeChild(_flyTarget);
        hoverAllFurni(false);
        setHoverSprite(null);

        _roomObj.removeListener(_roomListener);

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

        if (_editor == null) { // should be..
            _editor = new RoomEditorController(_wdctx, _roomObjectView);
        }
        _editor.startEditing(wrapupFn);
        _editor.updateUndoStatus(_updates.length != 0);
    }

    /**
     * Sends a room update to the server.
     */
    protected function updateRoom (update :SceneUpdate) :void
    {
        _roomObj.roomService.updateRoom(_wdctx.getClient(), update, new ReportingListener(_wdctx));
    }

    override protected function checkMouse2 (
        grabAll :Boolean, allowMovement :Boolean, setHitter :Function) :void
    {
        grabAll = isRoomEditing();
        if (grabAll) {
            allowMovement = _editor.isMovementEnabled();
            setHitter = _editor.mouseOverSprite;
        }

        super.checkMouse2(grabAll, allowMovement, setHitter);
    }

    override protected function mouseClicked (event :MouseEvent) :void
    {
        // if we're in a featured place view, any click should take the member to this room.
        if (UberClient.isFeaturedPlaceView()) {
            _wdctx.getWorldController().handleGoScene(_scene.getId());
            return;
        }

        super.mouseClicked(event);
    }

    override protected function requestAvatarMove (newLoc :MsoyLocation) :void
    {
        _wdctx.getSpotSceneDirector().changeLocation(newLoc, null);
    }

    // documentation inherited
    override protected function setActorState2 (
        ident :ItemIdent, actorOid :int, state :String) :void
    {
        _roomObj.roomService.setActorState(_wdctx.getClient(), ident, actorOid, state);
    }

    // documentation inherited
    override protected function sendSpriteMessage2 (
        ident :ItemIdent, name :String, data :ByteArray, isAction :Boolean) :void
    {
        _roomObj.roomService.sendSpriteMessage(_wdctx.getClient(), ident, name, data, isAction);
    }

    // documentation inherited
    override protected function sendSpriteSignal2 (name :String, data :ByteArray) :void
    {
        _roomObj.roomService.sendSpriteSignal(_wdctx.getClient(), name, data);
    }

    // documentation inherited
    override protected function sendPetChatMessage2 (msg :String, info :ActorInfo) :void
    {
        var svc :PetService = (_wdctx.getClient().requireService(PetService) as PetService);
        svc.sendChat(_wdctx.getClient(), info.bodyOid, _scene.getId(), msg,
            new ReportingListener(_wdctx));
    }

    // documentation inherited
    override protected function updateMemory2 (ident :ItemIdent, key :String, data: ByteArray) :void
    {
        // ship the update request off to the server
        _roomObj.roomService.updateMemory(_wdctx.getClient(),
            new EntityMemoryEntry(ident, key, data));
    }

    // documentation inherited
    override protected function setRoomProperty2 (key :String, data: ByteArray) :void
    {
        // ship the update request off to the server
        _roomObj.roomService.setRoomProperty(_wdctx.getClient(), 
            new RoomPropertyEntry(key, data));
    }

    // documentation inherited
    override protected function keyEvent (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.F6) {
            var overlay :ChatOverlay = _wdctx.getTopPanel().getChatOverlay();
            if (overlay != null) {
                overlay.setClickableGlyphs(event.type == KeyboardEvent.KEY_DOWN);
            }
            event.updateAfterEvent();
            return;
        }

        super.keyEvent(event);
    }

    /**
     * Called when a message is received on the room object.
     */
    protected function msgReceivedOnRoomObj (event :MessageEvent) :void
    {
        var args :Array = event.getArgs();
        switch (event.getName()) {
        case RoomObject.LOAD_MUSIC:
            if (UberClient.isFeaturedPlaceView()) {
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
        var actor :OccupantSprite = _roomObjectView.getOccupant(bodyOid);
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
    override protected function checkCanRequest (ident :ItemIdent, from :String) :Boolean
    {
        if (_roomObj == null) {
            log.warning("Cannot issue request for lack of room object [from=" + from +
                        ", ident=" + ident + "].");
            return false;
        }

        return super.checkCanRequest(ident, from);
    }

    /**
     * Does this client have control over the specified entity?
     *
     * Side-effect: The gotControl() will always be re-dispatched to the entity if it does.
     * The newest EntityControl will suppress repeats.
     *
     * @returns true, false, or null if nobody currently has control.
     */
    override protected function hasEntityControl (ident :ItemIdent) :Object
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
        var data :FurniData;
        if (update is SceneAttrsUpdate) {
            var attrsUpdate :SceneAttrsUpdate = update as SceneAttrsUpdate;
            var newId :int = attrsUpdate.decor.itemId;
            var oldId :int = _scene.getDecor().itemId;
            if (newId != oldId) {
                if (newId != 0) {
                    _wdctx.getWorldClient().itemUsageChangedToGWT(
                        Item.DECOR, newId, Item.USED_AS_BACKGROUND, _scene.getId());
                }
                if (oldId != 0) {
                    _wdctx.getWorldClient().itemUsageChangedToGWT(
                        Item.DECOR, oldId, Item.UNUSED, 0);
                }
            }
            newId = attrsUpdate.audioData.itemId;
            oldId = _scene.getAudioData().itemId;
            if (newId != oldId) {
                if (newId != 0) {
                    _wdctx.getWorldClient().itemUsageChangedToGWT(
                        Item.AUDIO, newId, Item.USED_AS_BACKGROUND, _scene.getId());
                }
                if (oldId != 0) {
                    _wdctx.getWorldClient().itemUsageChangedToGWT(
                        Item.AUDIO, oldId, Item.UNUSED, 0);
                }
            }

            var newName :String = attrsUpdate.name;
            var oldName :String = _scene.getName();
            if (newName != oldName) {
                _wdctx.getMsoyClient().setWindowTitle(newName);
            }

        } else if (update is FurniUpdate_Add) {
            data = (update as FurniUpdate_Add).data;
            _wdctx.getWorldClient().itemUsageChangedToGWT(
                data.itemType, data.itemId, Item.USED_AS_FURNITURE, _scene.getId());

        } else if (update is FurniUpdate_Remove) {
            data = (update as FurniUpdate_Remove).data;
            _wdctx.getWorldClient().itemUsageChangedToGWT(
                data.itemType, data.itemId, Item.UNUSED, 0);
        }

        super.sceneUpdated(update);
        _roomObjectView.processUpdate(update);
        if (_editor != null) {
            _editor.processUpdate(update);
        }
    }
    
    /**
     * Return a SnapshotController for this room, creating a new one if necessary.
     */
    protected function getSnapshotController () :SnapshotController 
    {
        if (!_snapshotController) {
            _snapshotController = new SnapshotController(_wdctx, _roomView);            
        }
        return _snapshotController;        
    }

    /** A casted version of _roomView. */
    protected var _roomObjectView :RoomObjectView;

    /** The room object. */
    protected var _roomObj :RoomObject;

    /** Our general-purpose room listener. */
    protected var _roomListener :ChangeListener;

    /** The current scene we're viewing. */
    protected var _scene :MsoyScene;

    /** The music currently playing in the scene, which may or may not be
     * background music. */
    protected var _music :SoundPlayer;

    /** True if _music is the room's background music. Otherwise
     * The music playing is from some other source. */
    protected var _musicIsBackground :Boolean = true;

    /** Holds loading alternate music. Once triggered to play,
     * it's shifted to _music. */
    protected var _loadingMusic :SoundPlayer;

    /** Controller for in-room furni editing. */
    protected var _editor :RoomEditorController;
    
    /** Stack that stores the sequence of room updates. */
    protected var _updates :UpdateStack = new UpdateStack(updateRoom);

    /** A flag to indicate that the room editor should be opened when the view is un-minimized */
    protected var _openEditor :Boolean = false;
    
    /** Controller for taking snapshots of the room **/
    protected var _snapshotController :SnapshotController;
}
}
