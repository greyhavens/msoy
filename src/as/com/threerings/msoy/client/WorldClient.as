//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Stage;
import flash.display.StageQuality;
import flash.events.ContextMenuEvent;
import flash.external.ExternalInterface;
import flash.geom.Point;
import flash.system.Capabilities;
import flash.ui.ContextMenu;

import mx.core.Application;
import mx.resources.ResourceBundle;

import com.threerings.util.CommandEvent;
import com.threerings.util.Name;
import com.threerings.util.ResultAdapter;
import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.flash.MenuUtil;

import com.threerings.whirled.data.SceneMarshaller;
import com.threerings.whirled.spot.data.SpotMarshaller;
import com.threerings.whirled.spot.data.SpotSceneObject;

import com.threerings.parlor.data.ParlorMarshaller;
import com.threerings.toybox.data.ToyBoxMarshaller;

import com.threerings.presents.data.ClientObject;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.item.data.ItemMarshaller;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Document;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemList;
import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;
import com.threerings.msoy.item.data.all.Pet;
import com.threerings.msoy.item.data.all.Photo;

import com.threerings.msoy.world.client.RoomController;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.client.PetService;
import com.threerings.msoy.world.client.PetSprite;

import com.threerings.msoy.world.data.MsoySceneMarshaller;
import com.threerings.msoy.world.data.PetMarshaller;
import com.threerings.msoy.world.data.RoomConfig;

import com.threerings.msoy.game.data.AVRGameMarshaller;
import com.threerings.msoy.game.data.AVRMarshaller;

import com.threerings.msoy.chat.client.ReportingListener;

import com.threerings.msoy.notify.data.GuestInviteNotification;
import com.threerings.msoy.notify.data.LevelUpNotification;
import com.threerings.msoy.notify.data.ReleaseNotesNotification;

/**
 * Dispatched when the client is minimized or unminimized.
 * 
 * @eventType com.threerings.msoy.client.WorldClient.MINI_WILL_CHANGE
 */
[Event(name="miniWillChange", type="com.threerings.util.ValueEvent")]

/**
 * Dispatched when the client is known to be either embedded or not. This happens shortly after the
 * client is initialized.
 * 
 * @eventType com.threerings.msoy.client.WorldClient.EMBEDDED_STATE_KNOWN
 */
[Event(name="embeddedStateKnown", type="com.threerings.util.ValueEvent")]

/**
 * An event dispatched for tutorial-specific purposes.
 * 
 * @eventType com.threerings.msoy.client.WorldClient.TUTORIAL_EVENT
 */
[Event(name="tutorial", type="com.threerings.util.ValueEvent")]

/**
 * Handles the main services for the world and game clients.
 */
public class WorldClient extends BaseClient
{
    /**
     * An event dispatched when the client is minimized or unminimized.
     *
     * @eventType miniWillChange
     */
    public static const MINI_WILL_CHANGE :String = "miniWillChange";

    /**
     * An event dispatched when we learn whether or not the client is embedded.
     *
     * @eventType clientEmbedded
     */
    public static const EMBEDDED_STATE_KNOWN :String = "clientEmbedded";

    /**
     * An event dispatched for tutorial-specific purposes.
     *
     * @eventType tutorial
     */
    public static const TUTORIAL_EVENT :String = "tutorial";

    public static const log :Log = Log.getLog(WorldClient);

    public function WorldClient (stage :Stage)
    {
        super(stage);

        // TODO: allow users to choose? I think it's a decision that we should make for them.
        // Jon speculates that maybe we can monitor the frame rate and automatically shift it,
        // but noticable jiggles occur when it's switched and I wouldn't want the entire
        // world to jiggle when someone starts walking, then jiggle again when they stop.
        // So: for now we just peg it to MEDIUM.
        stage.quality = StageQuality.MEDIUM;

        // set up a context menu that blocks funnybiz on the stage
        var menu :ContextMenu = new ContextMenu();
        menu.hideBuiltInItems();
        Application.application.contextMenu = menu;
        menu.addEventListener(ContextMenuEvent.MENU_SELECT, contextMenuWillPopUp);

        // make sure we're running a sufficiently new version of Flash
        if (_wctx.getTopPanel().verifyFlashVersion()) {
            logon(); // now logon
        }
    }

    // from BaseClient
    override public function fuckingCompiler () :void
    {
        super.fuckingCompiler();
        var c :Class;
        c = AVRMarshaller;
        c = AVRGameMarshaller;
        c = Document;
        c = Furniture;
        c = Game;
        c = GuestInviteNotification;
        c = ItemList;
        c = ItemMarshaller;
        c = ItemPack;
        c = LevelPack;
        c = LevelUpNotification;
        c = MemberLocation;
        c = MsoySceneMarshaller;
        c = ParlorMarshaller;
        c = PetMarshaller;
        c = Photo;
        c = ReleaseNotesNotification;
        c = RoomConfig;
        c = SceneMarshaller;
        c = SpotMarshaller;
        c = SpotSceneObject;
        c = ToyBoxMarshaller;

        // these cause bundles to be compiled in.
        [ResourceBundle("general")]
        [ResourceBundle("chat")]
        [ResourceBundle("game")]
        [ResourceBundle("editing")]
        [ResourceBundle("item")]
        [ResourceBundle("notify")]
        [ResourceBundle("prefs")]
        var rb :ResourceBundle;
    }

    /**
     * Find out if we're currently working in mini-land or not.  Other components should be able to
     * check this value after they detect that the flash player's size has changed, to discover our
     * status in this regard.
     */
    public function isMinimized () :Boolean
    {
        return _minimized;
    }

    /**
     * Notifies our JavaScript shell that the flash client should be made full size.
     */
    public function restoreClient () :void
    {
        try {
            if (ExternalInterface.available && !_embedded && !_featuredPlaceView) {
                ExternalInterface.call("restoreClient");
            }
        } catch (err :Error) {
            log.warning("ExternalInterface.call('restoreClient') failed: " + err);
        }
    }

    /**
     * Notifies our JavaScript shell that the flash client should be cleared out.
     */
    public function closeClient () :void
    {
        try {
            if (ExternalInterface.available && !_embedded && !_featuredPlaceView) {
                ExternalInterface.call("clearClient");
            }
        } catch (err :Error) {
            log.warning("ExternalInterface.call('clearClient') failed: " + err);
        }
    }

    /**
     * Notifies javascript that we need it to create a little black divider at the given position.
     */
    public function setSeparator (x :int) :void
    {
        try {
            if (ExternalInterface.available && !_embedded && !_featuredPlaceView) {
                ExternalInterface.call("setSeparator", x);
            }
        } catch (err :Error) {
            log.warning("ExternalInteface.call('setSeparator') failed: " + err);
        }
    }

    /**
     * Notifies javascript that we no longer need the little black divider.
     */
    public function clearSeparator () :void
    {
        try {
            if (ExternalInterface.available && !_embedded && !_featuredPlaceView) {
                ExternalInterface.call("clearSeparator");
            }
        } catch (err :Error) {
            log.warning("ExternalInterface.call('clearSeparator') failed: " + err);
        }
    }

    /**
     * Requests that GWT set the window title.
     */
    public function setWindowTitle (title :String) :void
    {
        try {
            if (ExternalInterface.available && !_embedded && !_featuredPlaceView) {
                ExternalInterface.call("setWindowTitle", title);
            }
        } catch (err :Error) {
            Log.getLog(this).warning("setWindowTitle failed: " + err);
        }
    }

    // from Client
    override public function gotClientObject (clobj :ClientObject) :void
    {
        super.gotClientObject(clobj);
        if (clobj is MemberObject && !_embedded && !_featuredPlaceView) {
            var member :MemberObject = clobj as MemberObject;
            member.addListener(new AvatarUpdateNotifier());
        }
    }

    // from BaseClient
    override protected function createContext () :BaseContext
    {
        return (_wctx = new WorldContext(this));
    }

    // from BaseClient
    override protected function configureExternalFunctions () :void
    {
        super.configureExternalFunctions();

        ExternalInterface.addCallback("clientLogon", externalClientLogon);
        ExternalInterface.addCallback("clientGo", externalClientGo);
        ExternalInterface.addCallback("clientLogoff", externalClientLogoff);
        ExternalInterface.addCallback("setMinimized", externalSetMinimized);
        ExternalInterface.addCallback("inRoom", externalInRoom);
        ExternalInterface.addCallback("useAvatar", externalUseAvatar);
        ExternalInterface.addCallback("getAvatarId", externalGetAvatarId);
        ExternalInterface.addCallback("updateAvatarScale", externalUpdateAvatarScale);
        ExternalInterface.addCallback("useItem", externalUseItem);
        ExternalInterface.addCallback("removeFurni", externalRemoveFurni);
        ExternalInterface.addCallback("getSceneItemId", externalGetSceneItemId);
        ExternalInterface.addCallback("getFurniList", externalGetFurniList);
        ExternalInterface.addCallback("usePet", externalUsePet);
        ExternalInterface.addCallback("removePet", externalRemovePet);
        ExternalInterface.addCallback("getPets", externalGetPets);
        ExternalInterface.addCallback("tutorialEvent", externalTutorialEvent);

        try {
            _embedded = !(ExternalInterface.call("helloWhirled") as Boolean);
        } catch (err :Error) {
            _embedded = true;
        }
        dispatchEvent(new ValueEvent(WorldClient.EMBEDDED_STATE_KNOWN, _embedded));
    }

    /**
     * Called to process ContextMenuEvent.MENU_SELECT.
     */
    protected function contextMenuWillPopUp (event :ContextMenuEvent) :void
    {
        var menu :ContextMenu = (event.target as ContextMenu);
        var custom :Array = menu.customItems;
        custom.length = 0;

//        custom.push(MenuUtil.createControllerMenuItem(
//                        Msgs.GENERAL.get("b.toggle_fullscreen"),
//                        MsoyController.TOGGLE_FULLSCREEN, null, false,
//                        _wctx.getMsoyController().supportsFullScreen()));

        try {
            var allObjects :Array =
                _stage.getObjectsUnderPoint(new Point(_stage.mouseX, _stage.mouseY));
            var seenObjects :Array = [];
            for each (var disp :DisplayObject in allObjects) {
                do {
                    seenObjects.push(disp);
                    if (disp is ContextMenuProvider) {
                        (disp as ContextMenuProvider).populateContextMenu(_wctx, custom);
                    }
                    disp = disp.parent;

                } while (disp != null && (seenObjects.indexOf(disp) == -1));
            }
        } catch (e :Error) {
            Log.getLog(this).logStackTrace(e);
        }

        // HACK: putting the separator in the menu causes the item to not
        // work in linux, so we don't do it in linux.
        var useSep :Boolean = (-1 == Capabilities.os.indexOf("Linux"));

        // add the About menu item
        custom.push(MenuUtil.createControllerMenuItem(
                        Msgs.GENERAL.get("b.about"),
                        MsoyController.ABOUT, null, useSep));

        // then, the menu will pop up
    }

    /**
     * Exposed to javascript so that it may notify us to logon.
     */
    protected function externalClientLogon (memberId :int, token :String) :void
    {
        if (token == null) {
            return;
        }

        log.info("Logging on via external request [id=" + memberId + ", token=" + token + "].");
        var co :MemberObject = _wctx.getMemberObject();
        if (co == null || co.getMemberId() != memberId) {
            _wctx.getMsoyController().handleLogon(createStartupCreds(_wctx.getStage(), token));
        }
    }

    /**
     * Exposed to javascript so that it may notify us to move to a new location.
     */
    protected function externalClientGo (where :String) :void
    {
        var eidx :int = where.indexOf("=");
        if (eidx == -1) {
            log.warning("Dropping malformed go request [where=" + where + "].");
        } else {
            log.info("Changing scenes per external request [where=" + where + "].");
            var params :Object = new Object();
            params[where.substring(0, eidx)] = where.substring(eidx+1);
            _wctx.getMsoyController().goToPlace(params);
        }
    }

    /**
     * Exposed to javascript so that it may notify us to logoff.
     */
    protected function externalClientLogoff (backAsGuest :Boolean = true) :void
    {
        log.info("Logging off via external request [backAsGuest=" + backAsGuest + "].");

        if (backAsGuest) {
            // have the controller handle it it will logoff, then back as a guest
            _wctx.getMsoyController().handleLogon(null);
        } else {
            logoff(false);
        }
    }

    /**
     * Exposed to javascript so that it may let us know when we've been pushed out of the way.
     */
    protected function externalSetMinimized (minimized :Boolean) :void   
    {
        dispatchEvent(new ValueEvent(MINI_WILL_CHANGE, _minimized = minimized));
    }

    /**
     * Exposed to javascript so that the it may determine if the current scene is a room.
     */
    protected function externalInRoom () :Boolean
    {
        return _wctx.getTopPanel().getPlaceView() is RoomView;
    }

    /**
     * Exposed to javascript so that it may tell us to use this avatar.  If the avatarId of 0 is
     * passed in, the current avatar is simply cleared away, leaving them with the default.
     */
    protected function externalUseAvatar (avatarId :int, scale :Number) :void
    {
        _wctx.getWorldDirector().setAvatar(avatarId, scale);
    }

    /**
     * Exposed to javascript so that it can check the avatar that its showing in the inventory 
     * browser agains the avatar that the user is currently wearing.
     */
    protected function externalGetAvatarId () :int
    {
        var avatar :Avatar = _wctx.getMemberObject().avatar;
        return avatar == null ? 0 : avatar.itemId;
    }

    /**
     * Exposed to javascript so that the avatarviewer may update the scale of an avatar
     * in real-time.
     */
    protected function externalUpdateAvatarScale (avatarId :int, newScale :Number) :void
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            view.updateAvatarScale(avatarId, newScale);
        }
    }

    /**
     * Exposed to javascript so that it may tell us to use items in the current room, either as
     * background items, or as furni as apporpriate.
     */ 
    protected function externalUseItem (itemId :int, itemType :int) :void
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            view.getRoomController().useItem(itemId, itemType);
        }
    }

    /**
     * Exposed to javascript so that it may tell us to remove furni from the current room.
     */
    protected function externalRemoveFurni (itemId :int, itemType :int) :void
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            view.getRoomController().removeFurni(itemId, itemType);
        }
    }

    /**
     * Exposed to javascript so that it may find out the id of some specific item types for the
     * current room.
     */
    protected function externalGetSceneItemId (itemType :int) :int
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            return view.getRoomController().getItemId(itemType);
        } else {
            return 0;
        }
    }

    protected function externalGetFurniList () :Array 
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            return view.getRoomController().getFurniList();
        } else {
            return [];
        }
    }

    protected function externalUsePet (petId :int) :void
    {
        var svc :PetService = _ctx.getClient().requireService(PetService) as PetService;
        svc.callPet(_wctx.getClient(), petId, 
            new ReportingListener(_wctx, MsoyCodes.GENERAL_MSGS, null, "m.pet_called"));
    }

    protected function externalRemovePet (petId :int) :void
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            // ensure this pet really is in this room
            for each (var pet :PetSprite in view.getPets()) {
                if (pet.getItemIdent().itemId == petId) {
                    CommandEvent.dispatch(view, RoomController.ORDER_PET, [petId, Pet.ORDER_SLEEP]);
                    break;
                }
            }
        }
    }

    protected function externalGetPets () :Array
    {
        var view :RoomView = _wctx.getTopPanel().getPlaceView() as RoomView;
        if (view != null) {
            var petIds :Array = [];
            for each (var pet :PetSprite in view.getPets()) {
                petIds.push(pet.getItemIdent().itemId);
            }
            return petIds;
        } else {
            return [];
        }
    }

    protected function externalTutorialEvent (eventName :String) :void
    {
        _wctx.getGameDirector().tutorialEvent(eventName);
    }

    protected var _wctx :WorldContext;
    protected var _minimized :Boolean;
}
}

import flash.external.ExternalInterface;

import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;

import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.data.all.Avatar;

class AvatarUpdateNotifier implements AttributeChangeListener
{
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (MemberObject.AVATAR == event.getName()) {
            try {
                if (ExternalInterface.available) {
                    var newId :int = 0;
                    var oldId :int = 0;
                    var value :Object = event.getValue();
                    if (value is Avatar) {
                        newId = (value as Avatar).itemId;
                    }
                    value = event.getOldValue();
                    if (value is Avatar) {
                        oldId = (value as Avatar).itemId;
                    }
                    ExternalInterface.call("triggerFlashEvent", "avatarChanged", 
                        [ newId, oldId ]);
                }
            } catch (err :Error) {
                Log.getLog(this).warning("triggerFlashEvent failed: " + err);
            }
        }
    }
}
