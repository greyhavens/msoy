//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.ui.Keyboard;
import flash.utils.ByteArray;

import com.threerings.util.Arrays;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;
import com.threerings.util.ObjectMarshaller;
import com.threerings.util.Predicates;
import com.threerings.util.ValueEvent;

import com.threerings.presents.dobj.AttributeChangeAdapter;
import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.crowd.chat.client.MuteObserver;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.orth.data.MediaDescSize;
import com.threerings.orth.notify.data.Notification;
import com.threerings.orth.room.client.PetService;
import com.threerings.orth.ui.MediaWrapper;

import com.threerings.flex.CommandMenu;

import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.client.BootablePlaceController;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.Item_UsedAs;
import com.threerings.msoy.item.data.all.Launcher;
import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.room.client.editor.DoorTargetEditController;
import com.threerings.msoy.room.client.editor.ItemUsedDialog;
import com.threerings.msoy.room.client.editor.RoomEditorController;
import com.threerings.msoy.room.client.updates.FurniUpdateAction;
import com.threerings.msoy.room.client.updates.SceneUpdateAction;
import com.threerings.msoy.room.client.updates.UpdateAction;
import com.threerings.msoy.room.client.updates.UpdateStack;
import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.ControllableEntity;
import com.threerings.msoy.room.data.EntityControl;
import com.threerings.msoy.room.data.EntityMemories;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.FurniUpdate_Add;
import com.threerings.msoy.room.data.FurniUpdate_Remove;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.PetInfo;
import com.threerings.msoy.room.data.PetName;
import com.threerings.msoy.room.data.PuppetName;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.SceneAttrsUpdate;
import com.threerings.msoy.ui.BubblePopup;
import com.threerings.msoy.world.client.WorldControlBar;
import com.threerings.msoy.world.client.WorldController;

/**
 * Manages the various interactions that take place in a room scene.
 */
public class RoomObjectController extends RoomController
    implements BootablePlaceController, MuteObserver
{
    /** Some commands */
    public static const EDIT_DOOR :String = "EditDoor";
    public static const PUBLISH_ROOM :String = "PublishRoom";
    public static const SEND_POSTCARD :String = "SendPostcard";

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

    // from interface MuteObserver
    public function muteChanged (name :Name, nowMuted :Boolean) :void
    {
        // when s omeone becomes muted, it may affect the visibility of their sprite
        var occ :OccupantSprite = _roomObjectView.getOccupantByName(name);
        if (occ != null) {
            occ.muteChanged();
        }
        if (name is MemberName) {
            var memberId :int = MemberName(name).getId();
            for each (var pet :PetSprite in _roomObjectView.getPets()) {
                if (pet.getOwnerId() == memberId) {
                    pet.ownedMuteChanged();
                    // keep going: there could be more than one matching pet
                }
            }
        }
    }

    /**
     * Is the specified player in this room?
     */
    public function containsPlayer (name :MemberName) :Boolean
    {
        var info :OccupantInfo = _roomObj.getOccupantInfo(name);
        return (info != null) && !(info.username is PuppetName);
    }

    /**
     * Add to the specified menu, any room/avatar related menu items.
     */
    public function addAvatarMenuItems (name :MemberName, menuItems :Array) :void
    {
        const occInfo :MemberInfo = _roomObj.getOccupantInfo(name) as MemberInfo;
        if (occInfo == null) {
            return;
        }

        const us :MemberObject = _wdctx.getMemberObject();
        const avatar :MemberSprite = _roomObjectView.getOccupant(occInfo.bodyOid) as MemberSprite;
        // avatar may be null if not yet loaded. We check below..

        // then add our custom menu items
        if (occInfo.bodyOid == us.getOid()) {
            // if we're not a guest add a menu for changing avatars
            menuItems.push(createChangeAvatarMenu(us, true));
            // add our custom menu items (avatar actions and states)
            if (avatar != null) {
                addSelfMenuItems(avatar, menuItems, true);
            }

        } else { // shown when clicking on someone else
            if (avatar == null) {
                return;
            }
            if (name is PuppetName) {
                addPuppetMenuItems(avatar, menuItems);
            }
            var kind :String = Msgs.GENERAL.get(avatar.getDesc());
            var flagItems :Array = [];
            var bleepItem :Object = null;
            if (avatar.viz.isBleepable()) {
                var key :String = avatar.viz.isBleeped() ? "b.unbleep_avatar" : "b.bleep_avatar";
                //var key :String = avatar.viz.isBleeped() ? "b.unbleep_item" : "b.bleep_item";
                bleepItem = { label: Msgs.GENERAL.get(key), icon: BLEEP_ICON,
                    callback: avatar.viz.toggleBleeped, arg: _wdctx };
            }

            var ident :ItemIdent = avatar.getItemIdent();
            if (ident != null && ident.type >= 0) { // -1 is the default avatar, etc
                flagItems.push({ label: Msgs.GENERAL.get("b.view_item", kind),
                    command: MsoyController.VIEW_ITEM, arg: ident });
                if (!us.isPermaguest()) {
                    flagItems.push({ label: Msgs.GENERAL.get("b.flag_item", kind),
                        command: MsoyController.FLAG_ITEM, arg: ident });
                }
            }

            // finally, add whatever makes sense
            if (bleepItem != null || flagItems.length != 0) {
                CommandMenu.addSeparator(menuItems);
            }
            if (bleepItem != null) {
                menuItems.push(bleepItem);
            }
            if (flagItems.length > 0) {
                menuItems.push({ label: Msgs.GENERAL.get("l.item_menu", kind), icon: AVATAR_ICON,
                    children: flagItems });
            }
        }
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
            var info :MemberInfo = findOccupantById(instanceId);
            if (info != null) {
                name = info.username.toString();
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
            _roomObj.roomService.requestControl(ident);
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
        throttle(ident, _roomObj.roomService.changeLocation, ident, newloc);
        return true;
    }

    /**
     * Returns true if we are in edit mode, false if not.
     */
    public function isRoomEditing () :Boolean
    {
        return (_editor != null) && _editor.isEditing();
    }

    /**
     * Handles EDIT_DOOR.
     */
    public function handleEditDoor (furniData :FurniData) :void
    {
        if (isRoomEditing()) {
            cancelRoomEditing();
        }

        var handleResult :Function = function (result :Object) :void {
            DoorTargetEditController.start(furniData, _wdctx);
        };
        _roomObj.roomService.editRoom(_wdctx.resultListener(handleResult, MsoyCodes.EDITING_MSGS));
    }

    /**
     * A callback from the RoomObjectView to let us know that we may want to take a
     * step with door editing.
     */
    public function backgroundFinishedLoading () :void
    {
        DoorTargetEditController.updateLocation();
    }

    /**
     * Handle the ROOM_EDIT command.
     */
    public function handleRoomEdit () :void
    {
        if (!canManageRoom()) {
            return;
        }

        // TODO: debounce the button, since we're round-trippin' to the server..
        if (isRoomEditing()) {
            cancelRoomEditing();
            return;
        }

        var handleResult :Function = function (result :Object) :void {
            beginRoomEditing();
        };
        _roomObj.roomService.editRoom(_wdctx.resultListener(handleResult, MsoyCodes.EDITING_MSGS));
    }

    /**
     * Handle the ROOM_RATE command.
     */
    public function handleRoomRate (rating :Number) :void
    {
        _roomObj.roomService.rateRoom(rating, _wdctx.listener());
    }

    /**
     * Handles PUBLISH_ROOM.
     */
    public function handlePublishRoom () :void
    {
        _roomObj.roomService.publishRoom(_wdctx.listener(MsoyCodes.EDITING_MSGS));

        // TODO: remove this bubbley hint someday?
        BubblePopup.showHelpBubble(_wdctx, _wdctx.getControlBar().shareBtn,
            Msgs.WORLD.get("h.room_share"), -7);
    }

    /**
     * Handles FURNI_CLICKED.
     */
    override public function handleFurniClicked (furni :FurniData) :void
    {
        switch (furni.actionType) {
        case FurniData.ACTION_URL:
            _wdctx.getMsoyController().handleViewUrl(furni.splitActionData()[0] as String);
            return;

        case FurniData.ACTION_WORLD_GAME:
            postAction(WorldController.JOIN_AVR_GAME, int(furni.splitActionData()[0]));
            return;

        case FurniData.ACTION_LOBBY_GAME:
            postAction(WorldController.PLAY_GAME, int(furni.splitActionData()[0]));
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
            log.warning("Clicked on unhandled furni action type",
               "actionType", furni.actionType, "actionData", furni.actionData);
            return;
        }
    }

    override public function squelchPlayer (player :MemberName, squelch :Boolean) :void
    {
        var avatar :OccupantSprite = _roomObjectView.getOccupantByName(player)
        if (avatar != null) {
            avatar.squelch(squelch);
        }

        // Process all their pets too
        for each (var pet :PetSprite in _roomObjectView.getPets()) {
            if (pet.getOwnerId() == player.getId()) {
                pet.squelch(squelch);
            }
        }
    }

    /**
     * Handles AVATAR_CLICKED.
     */
    override public function handleAvatarClicked (avatar :MemberSprite) :void
    {
        var occInfo :MemberInfo = (avatar.getActorInfo() as MemberInfo);
        if (occInfo == null) {
            log.info("Clicked on non-MemberInfo sprite", "info", avatar.getActorInfo());
            return;
        }

        var menuItems :Array = [];
        _wdctx.getMsoyController().addMemberMenuItems(occInfo.username as MemberName, menuItems);
        popActorMenu(avatar, menuItems);
    }

    /**
     * Create the menu item that allows a user to change their own avatar.
     */
    protected function createChangeAvatarMenu (us :MemberObject, canControl :Boolean) :Object
    {
        var avItems :Array = [];
        var avatars :Array = (us.avatarCache != null) ? us.avatarCache.toArray() : [];
        Arrays.sort(avatars);

        avItems.push({ label: Msgs.GENERAL.get("b.avatars_full"),
            command: WorldController.VIEW_STUFF, arg: Item.AVATAR });
        CommandMenu.addSeparator(avItems);

        for (var ii :int = 0; ii < avatars.length; ii++) {
            var av :Avatar = avatars[ii] as Avatar;
            avItems.push({ label: av.name, enabled: !av.equals(us.avatar),
                iconObject: MediaWrapper.createView(
                    av.getThumbnailMedia(), MediaDescSize.QUARTER_THUMBNAIL_SIZE),
                callback: _wdctx.getWorldDirector().setAvatar, arg: av.itemId });
        }
        // add defaults
        avItems.push({ label: Msgs.ITEM.get("m.default"), enabled: (us.avatar != null),
            iconObject: MediaWrapper.createView(
                Item.getDefaultThumbnailMediaFor(Item.AVATAR),
                MediaDescSize.QUARTER_THUMBNAIL_SIZE),
            callback: _wdctx.getWorldDirector().setAvatar, arg: 0 });

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
            log.warning("Pet has unexpected ActorInfo", "info", pet.getActorInfo());
            return;
        }

        const memObj :MemberObject = _wdctx.getMemberObject();
        const isPetOwner :Boolean = (PetSprite(pet).getOwnerId() == memObj.getMemberId());
        const petId :int = occInfo.getItemIdent().itemId;

        var menuItems :Array = [];

        _wdctx.getWorldController().addPetMenuItems(PetName(occInfo.username), menuItems);
        if (pet.viz.isBleepable()) {
            var key :String = pet.viz.isBleeped() ? "b.unbleep_pet" : "b.bleep_pet";
            menuItems.push({ label: Msgs.GENERAL.get(key), icon: BLEEP_ICON,
                             callback: pet.viz.toggleBleeped, arg: _wdctx });
        }

        if (isPetOwner) {
            CommandMenu.addSeparator(menuItems);
            var isWalking :Boolean = (memObj.walkingId != 0);
            menuItems.push(
            { label: Msgs.GENERAL.get("b.order_pet_stay"),
              command: ORDER_PET, arg: [ petId, Pet.ORDER_STAY ], enabled: canManageRoom() },
            { label: Msgs.GENERAL.get("b.order_pet_follow"),
              command: ORDER_PET, arg: [ petId, Pet.ORDER_FOLLOW ], enabled: !isWalking },
            { label: Msgs.GENERAL.get("b.order_pet_go_home"),
              command: ORDER_PET, arg: [ petId, Pet.ORDER_GO_HOME ] });
        }
        if (isPetOwner || canManageRoom()) {
            CommandMenu.addSeparator(menuItems);
            // and any old room manager can put the pet to sleep
            menuItems.push({ label: Msgs.GENERAL.get("b.order_pet_sleep"),
                command: ORDER_PET, arg: [ petId, Pet.ORDER_SLEEP ] });
        }
        if (isPetOwner && pet.hasCustomConfigPanel()) {
            menuItems.push({ label: Msgs.GENERAL.get("b.config_item", "pet"),
                callback: showConfigPopup, arg: pet, enabled: memoriesWillSave() });
        }

        popActorMenu(pet, menuItems);
    }

    /**
     * Handles ORDER_PET.
     */
    override public function handleOrderPet (petId :int, command :int) :void
    {
        var svc :PetService = (_wdctx.getClient().requireService(PetService) as PetService);
        svc.orderPet(petId, command, _wdctx.confirmListener("m.pet_ordered" + command));
    }

    override public function getEnvironment () :String
    {
        return "room"; // EntityControl.ENV_ROOM
    }

    override public function getMemories (ident :ItemIdent) :Object
    {
        var mems :Object = {};
        var entry :EntityMemories = _roomObj.memories.get(ident) as EntityMemories;
        if (entry != null) {
            entry.memories.forEach(function (key :String, data :ByteArray) :void {
                mems[key] = ObjectMarshaller.decode(data);
            });
        }
        return mems;
    }

    override public function lookupMemory (ident :ItemIdent, key :String) :Object
    {
        var entry :EntityMemories = _roomObj.memories.get(ident) as EntityMemories;
        return (entry == null) ? null
                               : ObjectMarshaller.decode(entry.memories.get(key) as ByteArray);
    }

    override public function canManageRoom (
        memberId :int = 0, allowSupport :Boolean = true) :Boolean
    {
        var me :MemberObject = _wdctx.getMemberObject();
        if (memberId == 0 || (memberId == me.getMemberId())) { // self
            return (_scene != null && _scene.canManage(me, allowSupport));

        } else { // others
            var info :MemberInfo = findOccupantById(memberId);
            return (info != null) && info.isManager();
        }
    }

    override public function deleteItem (ident :ItemIdent) :void
    {
        var svc :ItemService = _wdctx.getClient().requireService(ItemService) as ItemService;
        svc.deleteItem(ident, _wdctx.confirmListener(MsoyCodes.EDITING_MSGS));
    }

    override public function rateRoom (rating :Number, onSuccess :Function) :void
    {
        _roomObj.roomService.rateRoom(rating, _wdctx.resultListener(onSuccess));
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
            svc.callPet(itemId, _wdctx.confirmListener("m.pet_called"));
            return;
        }

        if (itemType == Item.AUDIO) {
            if (_scene.getPlaylistControl() != MsoySceneModel.ACCESS_EVERYONE && !canManageRoom()) {
                _wdctx.displayFeedback(MsoyCodes.WORLD_MSGS, "e.no_playlist_perm");
                return;
            }

        } else {
            if (!canManageRoom()) {
                _wdctx.displayFeedback(MsoyCodes.EDITING_MSGS, "e.no_permission");
                return;
            }
        }

        if (itemType != Item.DECOR && itemType != Item.AUDIO) {
            _openEditor = true;
        }

        var isvc :ItemService = _wdctx.getClient().requireService(ItemService) as ItemService;
        var ident :ItemIdent = new ItemIdent(itemType, itemId);

        var gotItem :Function = function (item :Item) :void {
            // a function we'll invoke when we're ready to use the item
            var useNewItem :Function = function () :void {
                if (item.getType() == Item.DECOR) {
                    var newScene :MsoyScene = _scene.clone() as MsoyScene;
                    var newSceneModel :MsoySceneModel = MsoySceneModel(newScene.getSceneModel());
                    newSceneModel.decor = item as Decor;
                    applyUpdate(new SceneUpdateAction(_wdctx, _scene, newScene));

                } else if (item.getType() == Item.AUDIO) {
                    // audio is different
                    var rsp :String = "m.music_added" + (canManageRoom(0, false) ? "" : "_temp");
                    _roomObj.roomService.addOrRemoveSong(item.itemId, true,
                        _wdctx.confirmListener(rsp, MsoyCodes.WORLD_MSGS));

                } else {
                    // create a generic furniture descriptor
                    var furni :FurniData = new FurniData();
                    furni.id = _scene.getNextFurniId(0);
                    furni.itemType = item.getType();
                    furni.itemId = item.itemId;
                    furni.media = item.getFurniMedia();
                    if (item is Furniture) {
                        furni.hotSpotX = (item as Furniture).hotSpotX;
                        furni.hotSpotY = (item as Furniture).hotSpotY;
                    }
                    // create it at the front of the scene, centered on the floor
                    _roomView.setInitialFurniLocation(furni);
                    if (item is Launcher) {
                        var launcher :Launcher = (item as Launcher);
                        furni.actionType = launcher.isAVRG ?
                            FurniData.ACTION_WORLD_GAME : FurniData.ACTION_LOBBY_GAME;
                        furni.actionData = String(launcher.gameId) + ":" + launcher.name;
                    }
                    applyUpdate(new FurniUpdateAction(_wdctx, null, furni));
                }
            };

            if (item.isUsed()) {
                var msg :String = Item.getTypeKey(itemType);
                (new ItemUsedDialog(_wdctx, Msgs.ITEM.get(msg), function () :void {
                    isvc.reclaimItem(ident,
                        _wdctx.confirmListener(useNewItem, MsoyCodes.EDITING_MSGS,
                            "e.failed_to_remove", null, "Failed to reclaim item", "item", ident));
                })).open(true);
            } else {
                useNewItem();
            }
        };

        isvc.peepItem(ident, _wdctx.resultListener(gotItem, MsoyCodes.EDITING_MSGS));
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

        } else if (itemType == Item.AUDIO) {
            _roomObj.roomService.addOrRemoveSong(itemId, false,
                _wdctx.confirmListener("m.music_removed", MsoyCodes.WORLD_MSGS));

        } else if (itemType == Item.DECOR) {
            var newScene :MsoyScene = _scene.clone() as MsoyScene;
            var newSceneModel :MsoySceneModel = (newScene.getSceneModel() as MsoySceneModel);
            newSceneModel.decor = MsoySceneModel.defaultMsoySceneModelDecor();
            applyUpdate(new SceneUpdateAction(_wdctx, _scene, newScene));

        } else {
            for each (var furni :FurniData in _scene.getFurni()) {
                if (furni.itemType == itemType && furni.itemId == itemId) {
                    applyUpdate(new FurniUpdateAction(_wdctx, furni, null));
                    break;
                }
            }
        }
    }

    // documentation inherited
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _roomObj = (plobj as RoomObject);
        _roomObj.addListener(_roomAttrListener);

        // report our location name and owner to interested listeners
        reportLocationName();
        reportLocationOwner();

        // get a copy of the scene
        _scene = (_wdctx.getSceneDirector().getScene() as MsoyScene);

        // if we're not playing a game, invite the user to play this group's game
        if (!_wdctx.getGameDirector().isGaming()) {
            var model :MsoySceneModel = _scene.getSceneModel() as MsoySceneModel;
            if (model.gameId != 0) {
                _wdctx.getNotificationDirector().addGenericNotification(
                    MessageBundle.tcompose("m.group_game", model.gameId), Notification.INVITE);
            }
        }

        _wdctx.getChatDirector().registerCommandHandler(
            Msgs.CHAT, "action", new AvatarChatHandler(false));
        _wdctx.getChatDirector().registerCommandHandler(
            Msgs.CHAT, "state", new AvatarChatHandler(true));
        _wdctx.getMuteDirector().addMuteObserver(this);

        // deactivate any hot zoneiness
        var bar :WorldControlBar = WorldControlBar(_wdctx.getControlBar());
        if (bar.hotZoneBtn.selected) {
            bar.hotZoneBtn.activate();
        }

        _walkTarget.visible = false;
        _flyTarget.visible = false;
        _roomView.appendElement(_flyTarget);
        _roomView.appendElement(_walkTarget);

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

        _wdctx.getChatDirector().unregisterCommandHandler(Msgs.CHAT, "action");
        _wdctx.getChatDirector().unregisterCommandHandler(Msgs.CHAT, "state");
        _wdctx.getMuteDirector().removeMuteObserver(this);

        _ctx.getClient().removeEventListener(MsoyClient.MINI_WILL_CHANGE, miniWillChange);

        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.removeEventListener(Event.ENTER_FRAME, checkMouse);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyEvent);

        _roomView.removeElement(_walkTarget);
        _roomView.removeElement(_flyTarget);
        setHoverSprite(null);

        if (_roomObj != null) {
            _roomObj.removeListener(_roomAttrListener);
            _roomObj = null;
        }

        _scene = null;

        super.didLeavePlace(plobj);
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
     * Begins editing the room.
     */
    protected function beginRoomEditing () :void
    {
        _walkTarget.visible = false;
        _flyTarget.visible = false;

        // this function will be called when the edit panel is closing
        var wrapupFn :Function = function () :void {
            _editor = null;
        }

        _editor = new RoomEditorController(_wdctx, _roomObjectView);
        _editor.startEditing(wrapupFn);
        _editor.updateUndoStatus(_updates.length != 0);
    }

    /**
     * Sends a room update to the server.
     */
    protected function updateRoom (update :SceneUpdate) :void
    {
        _roomObj.roomService.updateRoom(update, _wdctx.listener(MsoyCodes.EDITING_MSGS));
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

    override protected function requestAvatarMove (newLoc :MsoyLocation) :void
    {
        _wdctx.getSpotSceneDirector().changeLocation(newLoc, null);
    }

    // documentation inherited
    override protected function setActorState2 (
        ident :ItemIdent, actorOid :int, state :String) :void
    {
        throttle(ident, _roomObj.roomService.setActorState, ident, actorOid, state);
    }

    // documentation inherited
    override protected function sendSpriteMessage2 (
        ident :ItemIdent, name :String, data :ByteArray, isAction :Boolean) :void
    {
        throttle(ident, _roomObj.roomService.sendSpriteMessage, ident, name, data, isAction);
    }

    // documentation inherited
    override protected function sendSpriteSignal2 (
        ident :ItemIdent, name :String, data :ByteArray) :void
    {
        throttle(ident, _roomObj.roomService.sendSpriteSignal, name, data);
    }

    // documentation inherited
    override protected function sendPetChatMessage2 (msg :String, info :ActorInfo) :void
    {
        var svc :PetService = (_wdctx.getClient().requireService(PetService) as PetService);
        throttle(info.getItemIdent(), svc.sendChat,
            info.bodyOid, _scene.getId(), msg, _wdctx.confirmListener());
    }

    // documentation inherited
    override protected function updateMemory2 (
        ident :ItemIdent, key :String, data: ByteArray, callback :Function) :void
    {
        var resultHandler :Function = function (success :Boolean) :void {
            if (callback != null) {
                try {
                    callback(success);
                } catch (error :*) {
                    // ignored- error in usercode
                }
            }
        };

        // ship the update request off to the server
        throttle(ident, _roomObj.roomService.updateMemory,
            ident, key, data, _wdctx.resultListener(resultHandler));
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
     * Ensures that we can issue a request to update the distributed state of the specified item,
     * returning true if so, false if we don't yet have a room object or are not in control of that
     * item.
     */
    override protected function checkCanRequest (ident :ItemIdent, from :String) :Boolean
    {
        if (_roomObj == null) {
            log.warning("Cannot issue request for lack of room object",
                "from", from, "ident", ident);
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

        // TODO: see if this can be made more efficient for avatars.
        // I would think that we could just check the itemId against the user's worn avatar,
        // but that might not work with certain edge cases (default avatar, for example).

        // first, let's check all the MemberInfos
        for each (var occInfo :Object in _roomObj.occupantInfo.toArray()) {
            if (occInfo is MemberInfo) {
                var winfo :MemberInfo = (occInfo as MemberInfo);
                if (ident.equals(winfo.getItemIdent())) {
                    if (winfo.bodyOid == ourOid) {
                        // dispatch got-control to the avatar, it should supress repeats
                        dispatchEntityGotControl(ident);
                        return true;

                    } else if (winfo.username is PuppetName) {
                        // Anyone can control the puppet, yet no instance is "in control".
                        // TODO: shit, shit, shit, shit. We might have to manage control or
                        // it could freak out avatars. I'm betting that most won't be affected...
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
            dispatchEntityGotControl(ident);
            return true;

        } else {
            return false;
        }
    }

    /**
     * Find a user's MemberInfo by their memberId.
     */
    protected function findOccupantById (memberId :int) :MemberInfo
    {
        for each (var obj :Object in _roomObj.occupantInfo.toArray()) {
            var info :MemberInfo = obj as MemberInfo;
            if (info != null && info.getMemberId() == memberId) {
                return info;
            }
        }
        return null;
    }

    /**
     * Add special menu items for puppets.
     */
    protected function addPuppetMenuItems (avatar :MemberSprite, menuItems :Array) :void
    {
        var states :Array = avatar.getAvatarStates();
        var curState :String = avatar.getState();
        var dance :String = locateAction(states, ["dance", "dancing"]);
        var items :Array = [];
        if (dance != null) {
            if (dance == curState) {
                items.push({ label: Msgs.NPC.get("b.stop_dance"),
                    // wrap the arg in an array in case its null
                    callback: avatar.setState, arg: [ states[0] ] });
            } else {
                items.push({ label: Msgs.NPC.get("b.dance"),
                    callback: avatar.setState, arg: dance });
            }
        }
        // TODO: should these make our own avatar dance as well??

        // TODO: other actions?

        if (items.length > 0) {
            CommandMenu.addSeparator(items);
            // add the items after the first separator (should be right after the title)
            var dex :int = Arrays.indexIf(menuItems,
                Predicates.createPropertyEquals("type", "separator"));
            Arrays.splice(menuItems, dex + 1, 0, items);
        }
    }

    /**
     * Locate an action that matches (case insensitively) the var-args search actions specified.
     */
    protected function locateAction (actions :Array, searches :Array) :String
    {
        searches = searches.map(function (s :String, ... _) :String {
            return s.toLowerCase();
        });
        for each (var action :String in actions) {
            if (action != null && searches.indexOf(action.toLowerCase()) >= 0) {
                return action;
            }
        }
        return null;
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
                        Item.DECOR, newId, Item_UsedAs.BACKGROUND, _scene.getId());
                }
                if (oldId != 0) {
                    _wdctx.getWorldClient().itemUsageChangedToGWT(
                        Item.DECOR, oldId, Item_UsedAs.NOTHING, 0);
                }
            }

        } else if (update is FurniUpdate_Add) {
            data = (update as FurniUpdate_Add).data;
            _wdctx.getWorldClient().itemUsageChangedToGWT(
                data.itemType, data.itemId, Item_UsedAs.FURNITURE, _scene.getId());

        } else if (update is FurniUpdate_Remove) {
            data = (update as FurniUpdate_Remove).data;
            _wdctx.getWorldClient().itemUsageChangedToGWT(
                data.itemType, data.itemId, Item_UsedAs.NOTHING, 0);
        }

        super.sceneUpdated(update);
        _roomObjectView.processUpdate(update);
        if (_editor != null) {
            _editor.processUpdate(update);
        }
    }

    protected function reportLocationName () :void
    {
        _wdctx.getTopPanel().dispatchEvent(
            new ValueEvent(TopPanel.LOCATION_NAME_CHANGED, _roomObj.name));
    }

    protected function reportLocationOwner () :void
    {
        _wdctx.getTopPanel().dispatchEvent(
            new ValueEvent(TopPanel.LOCATION_OWNER_CHANGED, _roomObj.owner));
    }

    protected function roomAttrChanged (event :AttributeChangedEvent) :void
    {
        if (event.getName() == RoomObject.NAME) {
            reportLocationName();
        } else if (event.getName() == RoomObject.OWNER) {
            reportLocationOwner();
        }
    }

    /** A casted version of _roomView. */
    protected var _roomObjectView :RoomObjectView;

    /** The room object. */
    protected var _roomObj :RoomObject;

    /** The current scene we're viewing. */
    protected var _scene :MsoyScene;

    /** Controller for in-room furni editing. */
    protected var _editor :RoomEditorController;

    /** Stack that stores the sequence of room updates. */
    protected var _updates :UpdateStack = new UpdateStack(updateRoom);

    /** A flag to indicate that the room editor should be opened when the view is un-minimized */
    protected var _openEditor :Boolean = false;

    /** Listens for room attribute changes. */
    protected var _roomAttrListener :AttributeChangeAdapter =
        new AttributeChangeAdapter(roomAttrChanged);

    [Embed(source="../../../../../../../rsrc/media/skins/menu/avatar.png")]
    protected static const AVATAR_ICON :Class;

    [Embed(source="../../../../../../../rsrc/media/skins/menu/bleep.png")]
    protected static const BLEEP_ICON :Class;
}
}
