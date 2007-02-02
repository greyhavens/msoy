package com.threerings.msoy.world.client.editor {

import flash.events.MouseEvent;

import mx.binding.utils.BindingUtils;

import mx.containers.Grid;

import mx.controls.Button;
import mx.controls.CheckBox;
import mx.controls.HSlider;

import com.threerings.util.CommandEvent;

import com.threerings.flex.GridUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

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

        checkReset();

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
                checkReset();
                _adjustedLast = _x;
                wasEdited();
            }, _x, "value");
            BindingUtils.bindSetter(function (val :Number) :void {
                _sprite.setMediaScaleY(val);
                if (_locked.selected) {
                    setSlider(_x, val);
                }
                checkReset();
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
        _locked.label = Msgs.EDITING.get("l.scale_locked");

        _reset = new Button();
        _reset.label = Msgs.EDITING.get("l.scale_reset");
        _reset.addEventListener(MouseEvent.CLICK, handleReset);

        GridUtil.addRow(this, MsoyUI.createLabel(Msgs.EDITING.get("l.xscale")), _x, _locked);
        GridUtil.addRow(this, MsoyUI.createLabel(Msgs.EDITING.get("l.yscale")), _y, _reset);
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

    protected function checkReset () :void
    {
        _reset.enabled = (_sprite.getMediaScaleX() != 1) ||
            (_sprite.getMediaScaleY() != 1);
    }

    protected function wasEdited () :void
    {
        CommandEvent.dispatch(
            this, EditorController.SPRITE_PROPS_UPDATED, _sprite);
    }

    protected function handleReset (event :MouseEvent) :void
    {
        _x.value = 1;
        _y.value = 1;
    }

    protected var _ctx :MsoyContext;

    protected var _sprite :MsoySprite;

    protected var _x :HSlider;

    protected var _y :HSlider;

    protected var _locked :CheckBox;

    protected var _reset :Button;

    protected var _didBind :Boolean = false;

    /** The most recently adjusted slider. */
    protected var _adjustedLast :HSlider;
}
}
