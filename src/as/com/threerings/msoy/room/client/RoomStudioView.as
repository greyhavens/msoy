//
// $Id$

package com.threerings.msoy.room.client {

import flash.external.ExternalInterface;

import flash.system.Capabilities;
import flash.system.Security;

import flash.utils.ByteArray;

import mx.binding.utils.BindingUtils;

import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.flash.MediaContainer;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.data.UberClientModes;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.UberClient;

import com.threerings.msoy.ui.SliderPopup;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Decor;
import com.threerings.msoy.item.data.all.Furniture;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.room.data.ActorInfo;
import com.threerings.msoy.room.data.FurniData;
import com.threerings.msoy.room.data.MsoyLocation;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;

/**
 * A non-network RoomView for testing avatars and other room entities.
 */
public class RoomStudioView extends RoomView
{
    /** Used to construct ItemIdents for the things we're testing. */
    public static const MEMBER_ID :int = 1;
    public static const PET_ID :int = 2;

    public function RoomStudioView (ctx :StudioContext, ctrl :RoomStudioController)
    {
        super(ctx, ctrl);

        _sctx = ctx;
    }

    /**
     * This method is needed for anything registered as a "Viewer" in world.mxml.
     */
    public function loadBytes (bytes :ByteArray) :void
    {
        _testingSprite.setZippedMediaBytes(bytes);
    }

    public function initForViewing (params :Object, uberMode :int) :void
    {
        (_ctrl as RoomStudioController).studioOnStage(uberMode);

        switch (uberMode) {
        case UberClientModes.AVATAR_VIEWER:
            initViewAvatar(params);
            break;

        case UberClientModes.PET_VIEWER:
            initViewPet(params);
            break;

        case UberClientModes.DECOR_VIEWER:
            initViewDecor(params);
            break;

        case UberClientModes.DECOR_EDITOR:
            initEditDecor(params);
            break;

        case UberClientModes.FURNI_VIEWER:
        case UberClientModes.TOY_VIEWER:
            initViewFurni(params);
            break;
        }
    }

    /**
     * Provide access to the pet we're previewing.
     */
    public function getPet () :PetSprite
    {
        return _pet;
    }

    override public function getMyAvatar () :MemberSprite
    {
        return _avatar;
    }

    // from RoomView
    override public function getZoom () :Number
    {
        return _zoom;
    }

    public function setZoom (newZoom :Number) :void
    {
        _zoom = newZoom;
        relayout();
    }

    public function doEntityMove (ident :ItemIdent, newLoc :MsoyLocation) :void
    {
        var sprite :OccupantSprite = _entities.get(ident) as OccupantSprite;
        if (sprite != null) {
            sprite.moveTo(newLoc, _scene);
        }
    }

    public function doAvatarMove (newLoc :MsoyLocation) :void
    {
        emulateIdle(false);
        _avatar.moveTo(newLoc, _scene);
    }

    public function setActorState (ident :ItemIdent, state :String) :void
    {
        var actor :ActorSprite = _entities.get(ident) as ActorSprite;
        if (actor == null) {
            Log.dumpStack();
            return;
        }
        var info :ActorInfo = actor.getActorInfo().clone() as ActorInfo;
        info.setState(state);
        info.status = OccupantInfo.ACTIVE; // un-idle, if needed
        actor.setOccupantInfo(info, {});
    }

    override public function dispatchSpriteMessage (
        item :ItemIdent, name :String, arg :ByteArray, isAction :Boolean) :void
    {
        // un-idle our avatar
        if (isAction && (_avatar == _entities.get(item))) {
            emulateIdle(false);
        }
        super.dispatchSpriteMessage(item, name, arg, isAction);
    }

    public function setSpriteScale (scale :Number) :void
    {
        if (_testingSprite is MemberSprite) {
            var avatar :MemberSprite = (_testingSprite as MemberSprite);
            var info :StudioMemberInfo = avatar.getActorInfo().clone() as StudioMemberInfo;
            info.setScale(scale);
            info.status = OccupantInfo.ACTIVE; // while we're at it, un-idle
            avatar.setOccupantInfo(info, {});

        } else if (_testingSprite is FurniSprite) {
            var furni :FurniSprite = (_testingSprite as FurniSprite);
            furni.setMediaScaleX(scale);
            furni.setMediaScaleY(scale);

        } else {
            throw new Error("waka!");
        }
    }

    override public function setBackground (decor :Decor) :void
    {
        super.setBackground(decor);
        checkDrawRoom();
    }

    override public function updateBackground () :void
    {
        super.updateBackground();
        checkDrawRoom();
    }

    override protected function relayout () :void
    {
        super.relayout();

        if (_avatar != null) {
            relayoutSprite(_avatar);
        }
    }

    protected function checkDrawRoom () :void
    {
        var decor :Decor = _scene.getDecor();

        // if we're not specifically viewing a decor, show a wireframe decor
        switch (UberClient.getMode()) {
        case UberClientModes.DECOR_VIEWER:
            // nothing!
            break;

        case UberClientModes.DECOR_EDITOR:
            if (_backdropOverlay != null) {
                _backdrop.drawRoom(_backdropOverlay.graphics, decor.width, decor.height,
                    true, false, 0.5);
            }
            break;

        default:
            _backdrop.drawRoom(_bg.graphics, decor.width, decor.height, true, true);
            break;
        }
    }

    protected function initViewAvatar (params :Object) :void
    {
        // newstyle is that everything comes in on the "media" param, but let's still fall
        // back to "avatar" for a bit.
        var avatar :String = params["media"] || params["avatar"];
        var info :StudioMemberInfo = new StudioMemberInfo(_sctx, avatar);
        info.setScale(getScaleFromParams(params));
        _avatar = new MemberSprite(_ctx, info, {});
        addSprite(_avatar);
        _avatar.setEntering(new MsoyLocation(.1, 0, .25));
        _avatar.roomScaleUpdated();
        setCenterSprite(_avatar);
        _testingSprite = _avatar;

        addTalkControl();
        var idle :CommandButton = new CommandButton(null, emulateIdle);
        idle.styleName = "controlBarIdleButton";
        idle.toolTip = Msgs.STUDIO.get("i.idle")
        const bar :ControlBar = _ctx.getControlBar();
        bar.addCustomButton(idle);

        _saveScaling = ("true" == String(params["scaling"]));
        createSpriteScaleControls();
        _avatar.addEventListener(MediaContainer.SIZE_KNOWN, handleAvatarSizeKnown);
    }

    protected function initViewPet (params :Object) :void
    {
        var pet :String = params["media"];
        var name :String = params["name"] || "Pet";
        var info :StudioPetInfo = new StudioPetInfo(name, pet);
        _pet = new PetSprite(_ctx, info, {});
        _pet.setEntering(new MsoyLocation(.1, 0, .25));
        addSprite(_pet);
        setCenterSprite(_pet);
        _testingSprite = _pet;

        addDefaultAvatar();
    }

    protected function initViewDecor (params :Object) :void
    {
        // the Backdrop media will be set all up in RoomStudioController
        _testingSprite = _bg;

        addDefaultAvatar();
    }

    protected function initEditDecor (params :Object) :void
    {
        initViewDecor(params);
        addDefaultFurni();

        showBackdropOverlay(true);
        _layout.updateScreenLocation(_backdropOverlay);

        // open the panel
        new DecorEditPanel(_ctx, this);
    }

    protected function initViewFurni (params :Object) :void
    {
        var furni :FurniData = new FurniData();
        furni.id = _scene.getNextFurniId(0);
        furni.itemType = Item.FURNITURE;
        furni.itemId = 150;
        furni.media = new StudioMediaDesc(params["media"] as String);
        furni.loc = new MsoyLocation(0.5, 0, 0);

        _testingSprite = addFurni(furni);

        createSpriteScaleControls();
        initScaleProperties(.1, 4);
        addDefaultAvatar();
    }

    protected function addDefaultAvatar () :void
    {
        var avatarPath :String;
        if (Security.sandboxType != Security.LOCAL_WITH_FILE) {
            avatarPath = Avatar.getDefaultMemberAvatarMedia().getMediaPath();
        } else {
            avatarPath = findSDKMediaPath() + "default-avatar.swf";
        }
        _avatar = new MemberSprite(_ctx, new StudioMemberInfo(_sctx, avatarPath), {});
        _avatar.setEntering(new MsoyLocation(.1, 0, .25));
        addSprite(_avatar);
        setCenterSprite(_avatar);
        addTalkControl();
    }

    protected function addDefaultFurni () :void
    {
        var furniPath :String;
        if (Security.sandboxType != Security.LOCAL_WITH_FILE) {
            furniPath = Furniture.getTestingFurniMedia().getMediaPath();
        } else {
            furniPath = findSDKMediaPath() + "testing-furni.swf";
        }

        var furni :FurniData = new FurniData();
        furni.id = _scene.getNextFurniId(0);
        furni.itemType = Item.FURNITURE;
        furni.itemId = 300;
        furni.media = new StudioMediaDesc(furniPath);
        furni.loc = new MsoyLocation(.2, 0, .8);
        addFurni(furni);
    }

    protected function findSDKMediaPath () :String
    {
        var url :String = this.root.loaderInfo.url;
        var fileSep :String = (-1 != Capabilities.os.indexOf("Windows")) ? "\\" : "/";
        var dex :int = url.lastIndexOf(fileSep);
        if (dex == -1) {
            return "file:";
        } else {
            return url.substring(0, dex + 1);
        }
    }

    protected function addTalkControl () :void
    {
        var talk :CommandButton = new CommandButton(null, emulateChat);
        talk.styleName = "controlBarTalkButton";
        talk.toolTip = Msgs.STUDIO.get("i.talk");

        const bar :ControlBar = _ctx.getControlBar();
        bar.addCustomButton(talk);
    }

    protected function createSpriteScaleControls () :void
    {
        _scaleButton = new CommandButton(null, showSpriteScaler);
        _scaleButton.styleName = "controlBarScaleButton";
        _scaleButton.toolTip = Msgs.STUDIO.get("i.sprite_scale");
        _scaleButton.enabled = false;

        _ctx.getControlBar().addCustomButton(_scaleButton);
    }

    protected function showSpriteScaler () :void
    {
        SliderPopup.toggle(_scaleButton, (_avatar.getActorInfo() as StudioMemberInfo).getScale(),
            updateSpriteScale, _scaleProperties);
    }

    protected function getSpriteScale () :Number
    {
        if (_testingSprite is MemberSprite) {
            return ((_testingSprite as MemberSprite).getActorInfo() as StudioMemberInfo).getScale();

        } else if (_testingSprite is FurniSprite) {
            return (_testingSprite as FurniSprite).getMediaScaleX();

        } else {
            throw new Error("waka!");
        }
    }

    /**
     * Callback when the scale is updated in some way.
     */
    protected function updateSpriteScale (newScale :Number) :void
    {
        setSpriteScale(newScale);

        if (_saveScaling && ExternalInterface.available) {
            try {
                ExternalInterface.call("updateAvatarScale", newScale);
            } catch (e :Error) {
                trace(e);
            }
        }
    }

    /**
     * Tell the avatar that it chatted.
     */
    protected function emulateChat () :void
    {
        emulateIdle(false);
        _avatar.performAvatarSpoke();

        var ident :String = _avatar.getItemIdent().toString();
        var name :String = _avatar.getActorInfo().username.toString();
        for each (var entity :MsoySprite in _entities.values()) {
            entity.processChatMessage(ident, name, "bla bla bla");
        }
    }

    /**
     * Tell the avatar that it is idle.
     */
    protected function emulateIdle (idle :Boolean = true) :void
    {
        var info :StudioMemberInfo = _avatar.getActorInfo() as StudioMemberInfo;
        var newStatus :int = idle ? OccupantInfo.IDLE : OccupantInfo.ACTIVE;
        if (info.status == newStatus) {
            return; // our work here is done
        }
        info = info.clone() as StudioMemberInfo;
        info.status = newStatus;
        _avatar.setOccupantInfo(info, {});
    }

    /**
     * Handles the event when our viewer avatar's size is known.
     */
    protected function handleAvatarSizeKnown (event :ValueEvent) :void
    {
        var width :int = int(event.value[0]);
        var height :int = int(event.value[1]);

        // the minimum scale makes things 10 pixels in a dimension
        var minScale :Number = Math.max(10 / width, 10 / height);
        // the maximum bumps us up against the overall maximums
        var maxScale :Number = Math.min(OccupantSprite.MAX_WIDTH / width,
                                        OccupantSprite.MAX_HEIGHT / height);
        initScaleProperties(minScale, maxScale);
    }

    protected function initScaleProperties (minScale :Number, maxScale :Number) :void
    {
        // but we always ensure that scale 1.0 is selectable, even if it seems it shouldn't be.
        _scaleProperties.minimum = Math.min(1, minScale);
        _scaleProperties.maximum = Math.max(1, maxScale);

        // enable everything
        _scaleButton.enabled = true;
    }

    protected function getScaleFromParams (params :Object) :Number
    {
        var scale :Number = Number(params["scale"]);
        return (isNaN(scale) || scale == 0) ? 1 : scale;
    }

    protected var _sctx :StudioContext;

    protected var _zoom :Number = 1;

    protected var _testingSprite :MsoySprite;
    protected var _avatar :MemberSprite;
    protected var _pet :PetSprite;

    /** Used for sizing our own avatar. */
    protected var _scaleButton :CommandButton;
    protected var _saveScaling :Boolean;

    protected var _scaleProperties :Object = { tickValues: [ 1 ] };
}
}
