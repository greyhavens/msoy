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

        _container = new MediaContainer();
        _container.addEventListener(MediaContainer.SIZE_KNOWN, handleSizeKnown);
        _container.setMedia(_avatar.avatarMedia.getMediaPath());

        var wrapper :MediaWrapper = new MediaWrapper(_container,
            ActorSprite.MAX_WIDTH, ActorSprite.MAX_HEIGHT, true);
        addChild(wrapper);

        var hbox :HBox = new HBox();

        _reset = new CommandButton();
        _reset.label = Msgs.GENERAL.get("b.reset_scale");
        _reset.setCallback(function () :void {
            _slider.value = 1;
        });

        _slider = new HSlider();
        _slider.liveDragging = true;
        _slider.minimum = 0;
        _slider.maximum = int.MAX_VALUE;
        _slider.enabled = false;
        _slider.tickValues = [ 1 ];
        BindingUtils.bindSetter(sliderUpdated, _slider, "value");
        _slider.value = _avatar.scale;
        hbox.addChild(_slider);
        _reset.enabled = false;

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
        // the maximum bumps us up against the overall maximums
        var maxScale :Number = Math.min(ActorSprite.MAX_WIDTH / width,
            ActorSprite.MAX_HEIGHT / height);

        // but we always ensure that scale 1.0 is selectable, even if it seems it shouldn't be.
        _slider.minimum = Math.min(1, minScale);
        _slider.maximum = Math.max(1, maxScale);

        // enable everything
        _slider.enabled = true;
        _buttons[OK_BUTTON].enabled = true;
        sliderUpdated();
    }

    protected function sliderUpdated (... ignored) :void
    {
        var val :Number = _slider.value;
        _container.scaleX = val;
        _container.scaleY = val;
        _reset.enabled = (val != 1);

        // and position the media..
        var w :Number = _container.getContentWidth() * val;
        var h :Number = _container.getContentHeight() * val;

        _container.x = (ActorSprite.MAX_WIDTH - w) / 2;
        _container.y = (ActorSprite.MAX_HEIGHT - h);
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

    protected var _container :MediaContainer;

    protected var _reset :CommandButton;

    protected var _slider :HSlider;
}
}
