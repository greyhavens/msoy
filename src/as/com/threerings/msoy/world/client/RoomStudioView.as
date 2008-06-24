//
// $Id$

package com.threerings.msoy.world.client {

import flash.external.ExternalInterface;

import flash.utils.ByteArray;

import mx.binding.utils.BindingUtils;
import mx.containers.HBox;
import mx.controls.HSlider;

import com.threerings.util.ValueEvent;

import com.threerings.flash.MediaContainer;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.data.UberClientModes;

import com.threerings.msoy.client.Msgs;

/**
 * A non-network RoomView for testing avatars and other room entities.
 */
public class RoomStudioView extends RoomView
{
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
        _avatar.setMediaBytes(bytes);
    }

    public function initForViewing (params :Object, uberMode :int) :void
    {
        (_ctrl as RoomStudioController).studioOnStage();

        if (uberMode == UberClientModes.AVATAR_VIEWER) {

            var scale :Number = Number(params["scale"]);
            if (isNaN(scale) || scale == 0) {
                scale = 1;
            }

            var avatar :String = params["avatar"];
            var info :StudioMemberInfo = new StudioMemberInfo(_sctx, avatar);
            info.setScale(scale);
            _avatar = new MemberSprite(_ctx, info);
            addSprite(_avatar);

            if ("true" == String(params["scaling"])) {
                createScaleControls(scale);
                _avatar.addEventListener(MediaContainer.SIZE_KNOWN, handleSizeKnown);
            }
        }
    }

    override public function getMyAvatar () :MemberSprite
    {
        return _avatar;
    }

    public function setAvatarState (state :String) :void
    {
        var studioInfo :StudioMemberInfo = _avatar.getActorInfo().clone() as StudioMemberInfo;
        studioInfo.setState(state);
        _avatar.setOccupantInfo(studioInfo);
    }

    public function setAvatarScale (scale :Number) :void
    {
        var studioInfo :StudioMemberInfo = _avatar.getActorInfo().clone() as StudioMemberInfo;
        studioInfo.setScale(scale);
        _avatar.setOccupantInfo(studioInfo);
    }

    protected function createScaleControls (scale :Number) :void
    {
        _scaleReset = new CommandButton(Msgs.GENERAL.get("b.resetScale"), function () :void {
            _scaleSlider.value = 1;
        });

        _scaleSlider = new HSlider();
        _scaleSlider.width = 40; // TODO: This is tiny! The scale slider mayhap needs a new UI?
        _scaleSlider.liveDragging = true;
        _scaleSlider.minimum = 0;
        _scaleSlider.maximum = int.MAX_VALUE;
        _scaleSlider.value = scale;
        _scaleSlider.enabled = false;
        _scaleSlider.tickValues = [ 1 ];
        BindingUtils.bindSetter(scaleUpdated, _scaleSlider, "value");

        var box :HBox = new HBox();
        box.percentHeight = 100;
        box.styleName = "controlBarSpacer";
        box.addChild(_scaleSlider);
        box.addChild(_scaleReset);
        _ctx.getTopPanel().getControlBar().addCustomComponent(box);
    }

    /**
     * Handles the event when our viewer avatar's size is known.
     */
    protected function handleSizeKnown (event :ValueEvent) :void
    {
        var width :int = int(event.value[0]);
        var height :int = int(event.value[1]);

        // the minimum scale makes things 10 pixels in a dimension
        var minScale :Number = Math.max(10 / width, 10 / height);
        // the maximum bumps us up against the overall maximums
        var maxScale :Number = Math.min(OccupantSprite.MAX_WIDTH / width,
                                        OccupantSprite.MAX_HEIGHT / height);

        // but we always ensure that scale 1.0 is selectable, even if it seems it shouldn't be.
        _scaleSlider.minimum = Math.min(1, minScale);
        _scaleSlider.maximum = Math.max(1, maxScale);

        // enable everything
        _scaleSlider.enabled = true;
        scaleUpdated();
    }

    /**
     * Callback when the scale is updated in some way.
     */
    protected function scaleUpdated (... ignored) :void
    {
        var scale :Number = _scaleSlider.value;
        _scaleReset.enabled = (scale != 1);

        setAvatarScale(scale);

        if (ExternalInterface.available) {
            try {
                ExternalInterface.call("updateAvatarScale", scale);
            } catch (e :Error) {
                trace(e);
            }
        }
    }

    // much TODO

    protected var _sctx :StudioContext;

    protected var _avatar :MemberSprite;

    protected var _scaleReset :CommandButton;

    protected var _scaleSlider :HSlider;
}
}
