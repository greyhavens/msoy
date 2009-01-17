//
// $Id$

package com.threerings.msoy.room.client {

import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.ui.Keyboard;
import flash.utils.ByteArray;

import mx.controls.Button;

import com.threerings.util.ArrayUtil;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.ObjectMarshaller;
import com.threerings.util.ValueEvent;

import com.threerings.presents.dobj.ChangeListener;
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.client.BootablePlaceController;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.UberClient;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
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

import com.threerings.msoy.world.client.WorldControlBar;
import com.threerings.msoy.world.client.WorldController;

import com.threerings.msoy.room.client.editor.DoorTargetEditController;
import com.threerings.msoy.room.client.editor.ItemUsedDialog;
import com.threerings.msoy.room.client.editor.RoomEditorController;
import com.threerings.msoy.room.client.updates.FurniUpdateAction;
import com.threerings.msoy.room.client.updates.SceneUpdateAction;
import com.threerings.msoy.room.client.updates.UpdateAction;
import com.threerings.msoy.room.client.updates.UpdateStack;

import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.AudioData;
import com.threerings.msoy.room.data.ControllableAVRGame;
import com.threerings.msoy.room.data.ControllableEntity;
import com.threerings.msoy.room.data.EntityControl;
import com.threerings.msoy.room.data.EntityMemoryEntry;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.FurniUpdate_Add;
import com.threerings.msoy.room.data.FurniUpdate_Remove;
import com.threerings.msoy.room.data.MemberInfo;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.PetInfo;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.room.data.SceneAttrsUpdate;

import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.chat.client.ChatOverlay;

/**
 * Manages the various interactions that take place in a room scene.
 */
public class RoomObjectController extends RoomController
    implements BootablePlaceController
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
            // see if we can control our own avatar right now...
            const canControl :Boolean = _wdctx.worldProps.userControlsAvatar;

            // if we have followers, add a menu item for clearing them
            if (us.followers.size() > 0) {
                menuItems.push({ label: Msgs.GENERAL.get("l.clear_followers"),
                                 callback: ditchFollower});
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
            if (avatar != null) {
                addSelfMenuItems(avatar, menuItems, canControl);
            }

        } else {
            // create a menu for clicking on someone else
            const memId :int = occInfo.getMemberId();
            if (!MemberName.isGuest(memId)) {
                // TODO: move up when we can forward MemberObjects between servers for guests
                menuItems.push({ label: Msgs.GENERAL.get("b.follow_other"),
                                 callback: followOther, arg: occInfo.username });

                // If they are following us
                if (us.followers.containsKey(memId)) {
                    menuItems.push({ label: Msgs.GENERAL.get("b.ditch_follower"),
                                     callback: ditchFollower, arg: occInfo.username });
                } else {
                    menuItems.push({ label: Msgs.GENERAL.get("b.invite_follow"),
                                     callback: inviteFollow, arg: occInfo.username });
                }

                if (avatar != null) {
                    var kind :String = Msgs.GENERAL.get(avatar.getDesc());
                    var flagItems :Array = [];

                    if (avatar.isBlockable()) {
                        var key :String = avatar.isBlocked() ? "b.unbleep_item" : "b.bleep_item";
                        flagItems.push({ label: Msgs.GENERAL.get(key, kind),
                                         callback: avatar.toggleBlocked, arg: _wdctx });
                    }

                    var ident :ItemIdent = avatar.getItemIdent();
                    if (ident != null && ident.type >= 0) { // -1 is the default avatar, etc
                        flagItems.push({ label: Msgs.GENERAL.get("b.view_item", kind),
                                         command: MsoyController.VIEW_ITEM, arg: ident });
//                        flagItems.push({ label: Msgs.GENERAL.get("b.flag_item", kind),
//                                         command: MsoyController.FLAG_ITEM, arg: ident });
                    }

                    if (flagItems.length > 0) {
                        menuItems.push({ type: "separator"},
                                       { label: Msgs.GENERAL.get("l.item_menu", kind),
                                         children: flagItems });
                    }
                }
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
        throttle(ident, _roomObj.roomService.changeLocation, _wdctx.getClient(), ident, newloc);
        return true;
    }

    public function followOther (member :MemberName) :void
    {
        postAction(WorldController.RESPOND_FOLLOW, member.getMemberId());
    }

    /**
     * Sends an invitation to the specified member to follow us.
     */
    public function inviteFollow (member :MemberName) :void
    {
        var msvc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        msvc.inviteToFollow(_ctx.getClient(), member.getMemberId(), _wdctx.listener());
    }

    /**
     * Tells the server we no longer want to be following anyone.
     */
    public function clearFollow () :void
    {
        var msvc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        msvc.followMember(_ctx.getClient(), 0, _wdctx.listener());
    }

    /**
     * Tells the server we no longer want someone following us. If target member is null, all
     * our followers are ditched.
     */
    public function ditchFollower (member :MemberName = null) :void
    {
        var msvc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        msvc.ditchFollower(_ctx.getClient(), (member != null) ? member.getMemberId() : 0,
            _wdctx.listener());
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
        _roomObj.roomService.editRoom(_wdctx.getClient(), _wdctx.resultListener(handleResult));
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
        _roomObj.roomService.editRoom(_wdctx.getClient(), _wdctx.resultListener(handleResult));
    }

    /**
     * Handle the ROOM_RATE command.
     */
    public function handleRoomRate (rating :Number) :void
    {
        _roomObj.roomService.rateRoom(_wdctx.getClient(), rating, _wdctx.listener());
    }

    /**
     * Handles PUBLISH_ROOM.
     */
    public function handlePublishRoom () :void
    {
        _roomObj.roomService.publishRoom(_wdctx.getClient(), _wdctx.listener());
        // TODO: remove when A/B test is finished
        _wdctx.getMsoyClient().getABTestGroup("2008 12 share hint", true,
            _wdctx.resultListener(function (group :int) :void {
                // group 1: no help, group 2: help
                if (group == 2) {
                    WorldControlBar(_wdctx.getControlBar()).showShareButtonBubble();
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
            _wdctx.getMsoyController().handleViewUrl(furni.splitActionData()[0] as String);
            return;

        case FurniData.ACTION_WORLD_GAME:
            postAction(WorldController.JOIN_AVR_GAME, int(furni.splitActionData()[0]));
            return;

        case FurniData.ACTION_LOBBY_GAME:
            postAction(WorldController.VIEW_GAME, int(furni.splitActionData()[0]));
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

        var menuItems :Array = [];
        _wdctx.getMsoyController().addMemberMenuItems(
            occInfo.username as MemberName, menuItems, true, true);
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

        avItems.push({ label: Msgs.GENERAL.get("b.avatars_full"),
            command: WorldController.VIEW_STUFF, arg: Item.AVATAR });
        avItems.push({ type: "separator" });

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

        const memObj :MemberObject = _wdctx.getMemberObject();
        const isPetOwner :Boolean = (occInfo.getOwnerId() == memObj.getMemberId());
        const petId :int = occInfo.getItemIdent().itemId;
        const isMuted :Boolean = _wdctx.getMuteDirector().isMuted(occInfo.username);

        var menuItems :Array = [];

        menuItems.push({ label: Msgs.GENERAL.get(isMuted ? "b.unmute_pet" : "b.mute_pet"),
            callback: _wdctx.getMuteDirector().setMuted, arg: [ occInfo.username, !isMuted ] });

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
        if (isPetOwner || canManageRoom()) {
            // and any old room manager can put the pet to sleep
            menuItems.push({ label: Msgs.GENERAL.get("b.order_pet_sleep"),
                command: ORDER_PET, arg: [ petId, Pet.ORDER_SLEEP ] });
        }

        popActorMenu(pet, menuItems);
    }

    /**
     * Handles ORDER_PET.
     */
    override public function handleOrderPet (petId :int, command :int) :void
    {
        var svc :PetService = (_wdctx.getClient().requireService(PetService) as PetService);
        svc.orderPet(_wdctx.getClient(), petId, command,
            _wdctx.confirmListener("m.pet_ordered" + command));
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

    override public function canManageRoom (memberId :int = 0) :Boolean
    {
        var me :MemberObject = _wdctx.getMemberObject();
        if (memberId == 0 || (memberId == me.getMemberId())) { // self
            return (_scene != null && _scene.canManage(me));

        } else { // others
            var info :MemberInfo = findOccupantById(memberId);
            return (info != null) && info.isManager();
        }
    }

    override public function deleteItem (ident :ItemIdent) :void
    {
        var svc :ItemService = _wdctx.getClient().requireService(ItemService) as ItemService;

        svc.deleteItem(_wdctx.getClient(), ident, _wdctx.confirmListener());
    }

    override public function rateRoom (rating :Number, onSuccess :Function) :void
    {
        _roomObj.roomService.rateRoom(_wdctx.getClient(), rating, _wdctx.resultListener(onSuccess));
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
            svc.callPet(_wdctx.getClient(), itemId, _wdctx.confirmListener("m.pet_called"));
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
                    var newSceneModel :MsoySceneModel = MsoySceneModel(newScene.getSceneModel());
                    newSceneModel.decor = item as Decor;
                    applyUpdate(new SceneUpdateAction(_wdctx, _scene, newScene));

                } else if (item.getType() == Item.AUDIO) {
                    var audio :Audio = item as Audio;
                    newScene = _scene.clone() as MsoyScene;
                    (newScene.getSceneModel() as MsoySceneModel).audioData =
                        new AudioData(audio.itemId, audio.audioMedia);
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
                }
            };

            if (item.isUsed()) {
                var msg :String = Item.getTypeKey(itemType);
                (new ItemUsedDialog(_wdctx, Msgs.ITEM.get(msg), function () :void {
                    isvc.reclaimItem(_wdctx.getClient(), ident,
                        _wdctx.confirmListener(useNewItem, MsoyCodes.EDITING_MSGS,
                            "e.failed_to_remove", null, "Failed to reclaim item", "item", ident));
                })).open(true);
            } else {
                useNewItem();
            }
        };

        isvc.peepItem(_wdctx.getClient(), ident,
            _wdctx.resultListener(gotItem, MsoyCodes.EDITING_MSGS));
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
                (newScene.getSceneModel() as MsoySceneModel).audioData = null;
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

    // documentation inherited
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _roomObj = (plobj as RoomObject);

        // get a copy of the scene
        _scene = (_wdctx.getSceneDirector().getScene() as MsoyScene);

        _wdctx.getMsoyController().addGoMenuProvider(populateGoMenu);

        var bar :WorldControlBar = WorldControlBar(_wdctx.getControlBar());
        _roomEditBtn = bar.roomEditBtn;
        _roomEditBtn.enabled = canManageRoom();

        // deactivate any hot zoneiness
        if (bar.hotZoneBtn.selected) {
            bar.hotZoneBtn.activate();
        }

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

        _wdctx.getMsoyController().removeGoMenuProvider(populateGoMenu);

        _ctx.getClient().removeEventListener(MsoyClient.MINI_WILL_CHANGE, miniWillChange);

        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.removeEventListener(Event.ENTER_FRAME, checkMouse);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyEvent);

        _roomView.removeChild(_walkTarget);
        _roomView.removeChild(_flyTarget);
        setHoverSprite(null);

        _scene = null;
        _roomObj = null;

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

        // put the room edit button in the selected state
        _roomEditBtn.selected = true;

        // this function will be called when the edit panel is closing
        var wrapupFn :Function = function () :void {
            _roomEditBtn.selected = false;
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
        _roomObj.roomService.updateRoom(_wdctx.getClient(), update, _wdctx.listener());
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
        throttle(ident, _roomObj.roomService.setActorState,
            _wdctx.getClient(), ident, actorOid, state);
    }

    // documentation inherited
    override protected function sendSpriteMessage2 (
        ident :ItemIdent, name :String, data :ByteArray, isAction :Boolean) :void
    {
        throttle(ident, _roomObj.roomService.sendSpriteMessage,
            _wdctx.getClient(), ident, name, data, isAction);
    }

    // documentation inherited
    override protected function sendSpriteSignal2 (
        ident :ItemIdent, name :String, data :ByteArray) :void
    {
        throttle(ident, _roomObj.roomService.sendSpriteSignal, _wdctx.getClient(), name, data);
    }

    // documentation inherited
    override protected function sendPetChatMessage2 (msg :String, info :ActorInfo) :void
    {
        var svc :PetService = (_wdctx.getClient().requireService(PetService) as PetService);
        throttle(info.getItemIdent(), svc.sendChat,
            _wdctx.getClient(), info.bodyOid, _scene.getId(), msg, _wdctx.confirmListener());
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
            _wdctx.getClient(), new EntityMemoryEntry(ident, key, data),
            _wdctx.resultListener(resultHandler));
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
            newId = (attrsUpdate.audioData == null) ? 0 : attrsUpdate.audioData.itemId;
            oldId = (_scene.getAudioData() == null) ? 0 : _scene.getAudioData().itemId;
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
     * Populates any room-specific entries on the client's "go" menu.
     */
    protected function populateGoMenu () :Array
    {
        const model :MsoySceneModel = _scene.getSceneModel() as MsoySceneModel;

        const stuff :Array = [];
        if (model.ownerType == MsoySceneModel.OWNER_TYPE_GROUP) {
            stuff.push({ label: Msgs.GENERAL.get("b.group_page"),
                command: MsoyController.VIEW_GROUP, arg: model.ownerId });
        }
        if (model.gameId != 0) {
            stuff.push({ label: Msgs.GENERAL.get("b.group_game"),
                command: WorldController.JOIN_GAME_LOBBY, arg: model.gameId });
        }
        return stuff;
    }

    /** A casted version of _roomView. */
    protected var _roomObjectView :RoomObjectView;

    /** The room object. */
    protected var _roomObj :RoomObject;

    /** The room edit button, on the control bar (imported). */
    protected var _roomEditBtn :Button;

    /** The current scene we're viewing. */
    protected var _scene :MsoyScene;

    /** Controller for in-room furni editing. */
    protected var _editor :RoomEditorController;

    /** Stack that stores the sequence of room updates. */
    protected var _updates :UpdateStack = new UpdateStack(updateRoom);

    /** A flag to indicate that the room editor should be opened when the view is un-minimized */
    protected var _openEditor :Boolean = false;
}
}
