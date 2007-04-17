package com.threerings.msoy.world.client.editor {

import mx.binding.utils.BindingUtils;

import mx.containers.Canvas;

import mx.controls.HSlider;
import mx.controls.VSlider;

import mx.core.ScrollPolicy;

import com.threerings.util.CommandEvent;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.data.MsoyLocation;

public class LocationEditor extends Canvas
{
    public function LocationEditor (ctx :WorldContext)
    {
        _ctx = ctx;

        // jesus, flex bites my ass
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    public function setSprite (sprite :MsoySprite) :void
    {
        _sprite = sprite;

        var loc :MsoyLocation = sprite.getLocation();
        _x.value = loc.x;
        _y.value = loc.y;
        _z.value = loc.z;

        if (!_didBind) {
            BindingUtils.bindSetter(function (val :Number) :void {
                _sprite.setLocation([ val, loc.y, loc.z ]);
                wasEdited();
            }, _x, "value");
            BindingUtils.bindSetter(function (val :Number) :void {
                _sprite.setLocation([ loc.x, val, loc.z ]);
                wasEdited();
            }, _y, "value");
            BindingUtils.bindSetter(function (val :Number) :void {
                _sprite.setLocation([ loc.x, loc.y, val ]);
                wasEdited();
            }, _z, "value");

            _didBind = true;
        }
    }

    override protected function measure () :void
    {
        // TODO: can flex's layout be any worse? Do we need to hard-code
        // every size?
        maxWidth = 200;
        maxHeight = 200;
        measuredWidth = 200;
        measuredHeight = 200;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _x = new HSlider();
        _x.tickInterval = .1;
        _x.liveDragging = true;
        _x.minimum = 0;
        _x.maximum = 1;
        _x.x = 0;
        _x.y = 180;
        _x.width = 180;
        addChild(_x);

        _y = new VSlider();
        _y.tickInterval = .1;
        _y.liveDragging = true;
        _y.minimum = 0;
        _y.maximum = 1;
        _y.x = 175;
        _y.y = 0;
        _y.height = 180;
        addChild(_y);

        _z = new HSlider();
        _z.tickInterval = .1;
        _y.liveDragging = true;
        _z.liveDragging = true;
        _z.minimum = 0;
        _z.maximum = 1;
        _z.x = 175;
        _z.y = 180;
        _z.width = 180;
        // TODO: make the Z slider angle match that of the room?
        _z.rotation = 215;
        addChild(_z);
    }

    protected function wasEdited () :void
    {
        CommandEvent.dispatch(
            this, EditorController.SPRITE_PROPS_UPDATED, _sprite);
    }

    /** The giver of life. */
    protected var _ctx :WorldContext;

    /** The sprite we're editing. */
    protected var _sprite :MsoySprite;

    protected var _didBind :Boolean = false;

    protected var _x :HSlider;
    protected var _y :VSlider;
    protected var _z :HSlider;
}
}
