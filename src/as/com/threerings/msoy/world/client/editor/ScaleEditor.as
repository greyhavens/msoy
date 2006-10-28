package com.threerings.msoy.world.client.editor {

import mx.binding.utils.BindingUtils;

import mx.controls.CheckBox;
import mx.controls.HSlider;

import com.threerings.mx.events.CommandEvent;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.world.client.MsoySprite;

/**
 * A user interface for editing the scale of a sprite.
 */
public class ScaleEditor extends Grid
{
    public function ScaleEditor (ctx :MsoyContext)
    {
        _ctx = ctx;
    }

    public function setSprite (sprite :MsoySprite) :void
    {
        var newSprite :Boolean = (sprite != _sprite);
        _sprite = sprite;

        _x.value = sprite.getMediaScaleX();
        _y.value = sprite.getMediaScaleY();

        if (newSprite) {
            _adjustedLast = null;
            // only guess a value for locked on new sprites
            _locked.selected = (_x.value == _y.value);
        }

        if (!_didBind) {
            BindingUtils.bindSetter(function (val :Number) :void {
                _sprite.setMediaScaleX(val);
                if (_locked.selected) {
                    setSlider(_y, val);
                }
                _adjustedLast = _x;
                wasEdited();
            }, _x, "value");
            BindingUtils.bindSetter(function (val :Number) :void {
                _sprite.setMediaScaleY(val);
                if (_locked.selected) {
                    setSlider(_x, val);
                }
                _adjustedLast = _y;
                wasEdited();
            }, _y, "value");
            BindingUtils.bindSetter(function (val :Boolean) :void {
                if (!val) {
                    return;
                }
                // when locking gets turned on, we snap the least-recently
                // adjusted scale to the most-recently adjusted scale.
                // if neither have been adjusted, we snap to the one closer
                // to 1
                var arbiter :HSlider = _adjustedLast;
                if (arbiter == null) {
                    arbiter = (Math.abs(1 - _x.value) < Math.abs(1 - _y.value))
                        ? _x : _y;
                }
                setSlider(_x, arbiter.value);
                setSlider(_y, arbiter.value);

            }, _locked, "selected");

            _didBind = true;
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _x = new HSlider();
        _x.liveDragging = true;
        _x.minimum = -2;
        _x.maximum = 2;
        _x.tickInterval = 1;

        _y = new HSlider();
        _y.liveDragging = true;
        _y.minimum = -2;
        _y.maximum = 2;
        _y.tickInterval = 1;

        _locked = new CheckBox();
        _locked.percentHeight = 100;
        _locked.label = Msgs.EDITING.get("l.scale_locked");

        addRow(MsoyUI.createLabel(Msgs.EDITING.get("l.xscale")), _x,
            _locked, [ 1, 2]);
        addRow(
            MsoyUI.createLabel(Msgs.EDITING.get("l.yscale")), _y);
    }

    /**
     * Sliders always fire a value event, even if set to the same value
     * that they already contain. To prevent our binding stuff from
     * overflowing the stack we have to externalize the logic of not
     * firing the event when the value is the same as it already is.
     */
    protected function setSlider (slider :HSlider, val :Number) :void
    {
        if (slider.value != val) {
            slider.value = val;
        }
    }

    protected function wasEdited () :void
    {
        CommandEvent.dispatch(
            this, EditorController.SPRITE_PROPS_UPDATED, _sprite);
    }

    protected var _ctx :MsoyContext;

    protected var _sprite :MsoySprite;

    protected var _x :HSlider;

    protected var _y :HSlider;

    protected var _locked :CheckBox;

    protected var _didBind :Boolean = false;

    /** The most recently adjusted slider. */
    protected var _adjustedLast :HSlider;
}
}
