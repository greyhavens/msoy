//
// $Id$

package com.threerings.msoy.client {

import mx.binding.utils.BindingUtils;

import mx.containers.HBox;

import mx.controls.HSlider;

import com.threerings.util.ValueEvent;

import com.threerings.flash.MediaContainer;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.item.data.all.Avatar;

import com.threerings.msoy.world.client.ActorSprite;

public class AvatarScalingDialog extends FloatingPanel
{
    public function AvatarScalingDialog (ctx :WorldContext, avatar :Avatar)
    {
        super(ctx, Msgs.GENERAL.get("t.avatar_scale"));
        _avatar = avatar;

        open(true);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var container :MediaContainer = new MediaContainer();
        container.addEventListener(MediaContainer.SIZE_KNOWN, handleSizeKnown);
        container.setMedia(_avatar.avatarMedia.getMediaPath());

        var wrapper :MediaWrapper = new MediaWrapper(container,
            ActorSprite.MAX_WIDTH, ActorSprite.MAX_HEIGHT, true);
        addChild(wrapper);

        var hbox :HBox = new HBox();

        _reset = new CommandButton();
        _reset.enabled = false;
        _reset.label = Msgs.GENERAL.get("b.reset_scale");
        _reset.setFunction(function () :void {
            _slider.value = 1;
        });

        _slider = new HSlider();
        _slider.liveDragging = true;
        _slider.minimum = 0;
        _slider.maximum = int.MAX_VALUE;
        _slider.enabled = false;
        _slider.tickValues = [ 1 ];
        BindingUtils.bindSetter(function (newScale :Number) :void {
            container.scaleX = newScale;
            container.scaleY = newScale;
            _reset.enabled = (newScale != 1);
        }, _slider, "value");
        _slider.value = _avatar.scale;
        hbox.addChild(_slider);

        hbox.addChild(_reset);

        addChild(hbox);

        addButtons(OK_BUTTON, CANCEL_BUTTON);

        _buttons[OK_BUTTON].enabled = false;
    }

    /**
     * When the size of the avatar is known we can enable the scaling.
     */
    protected function handleSizeKnown (event :ValueEvent) :void
    {
        var width :int = int(event.value[0]);
        var height :int = int(event.value[1]);

        // the minimum scale makes things 10 pixels in a dimension
        var minScale :Number = Math.max(10 / width, 10 / height);
        // the maximum bumps us up against
        var maxScale :Number = Math.min(ActorSprite.MAX_WIDTH / width,
            ActorSprite.MAX_HEIGHT / height);

        // but we always ensure that scale 1.0 is selectable, even if it seems it shouldn't be.
        _slider.minimum = Math.min(1, minScale);
        _slider.maximum = Math.max(1, maxScale);

        // enable everything
        _slider.enabled = true;
        _reset.enabled = true;
        _buttons[OK_BUTTON].enabled = true;
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == OK_BUTTON && _slider.enabled) {
            _ctx.getWorldDirector().setAvatar(_avatar.itemId, _slider.value);
        }

        super.buttonClicked(buttonId);
    }

    /** The avatar we're scaling. */
    protected var _avatar :Avatar;

    protected var _reset :CommandButton;

    protected var _slider :HSlider;
}
}
