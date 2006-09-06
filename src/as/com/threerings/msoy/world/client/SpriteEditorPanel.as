package com.threerings.msoy.world.client {

import mx.binding.utils.BindingUtils;

import mx.core.UIComponent;

import mx.containers.Grid;
import mx.containers.GridItem;
import mx.containers.GridRow;
import mx.containers.VBox;

import mx.controls.TextInput;

import com.threerings.mx.events.CommandEvent;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.MsoyUI;

public class SpriteEditorPanel extends Grid
{
    public function SpriteEditorPanel (ctx :MsoyContext)
    {
        _ctx = ctx;
    }

    /**
     * Set the sprite to be edited.
     */
    public function setSprite (sprite :MsoySprite) :void
    {
        _sprite = sprite;

        if (_x != null) {
            bind();
        }
    }

    /**
     * Called when we set up and when the sprite is changed
     * by the mousing editor controls.
     */
    public function updateInputFields () :void
    {
        _x.text = String(_sprite.loc.x);
        _y.text = String(_sprite.loc.y);
        _z.text = String(_sprite.loc.z);

        _xScale.text = String(_sprite.getMediaScaleX());
        _yScale.text = String(_sprite.getMediaScaleY());
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.x")),
            _x = new TextInput());
        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.y")),
            _y = new TextInput());
        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.z")),
            _z = new TextInput());
        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.xscale")),
            _xScale = new TextInput());
        addRow(
            MsoyUI.createLabel(_ctx.xlate("editing", "l.yscale")),
            _yScale = new TextInput());
    }

    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        if (_sprite != null) {
            bind();
        }
    }

    protected function addRow (... comps) :void
    {
        var row :GridRow = new GridRow();
        for each (var comp :UIComponent in comps) {
            addToRow(row, comp);
        }
        addChild(row);
    }

    protected function addToRow (row :GridRow, comp :UIComponent) :void
    {
        var item :GridItem = new GridItem();
        item.addChild(comp);
        row.addChild(item);
    }

    protected function bind () :void
    {
        updateInputFields();

        // we can set up bindings pretty easily on all these text fields
        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sprite.setLocation([ val, _sprite.loc.y, _sprite.loc.z ]);
                spriteWasTextuallyEdited();
            }
        }, _x, "text");
        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sprite.setLocation([ _sprite.loc.x, val, _sprite.loc.z ]);
                spriteWasTextuallyEdited();
            }
        }, _y, "text");
        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sprite.setLocation([ _sprite.loc.x, _sprite.loc.y, val ]);
                spriteWasTextuallyEdited();
            }
        }, _z, "text");

        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sprite.setMediaScaleX(val);
                spriteWasTextuallyEdited();
            }
        }, _xScale, "text");
        BindingUtils.bindSetter(function (o :Object) :void {
            var val :Number = Number(o);
            if (!isNaN(val)) {
                _sprite.setMediaScaleY(val);
                spriteWasTextuallyEdited();
            }
        }, _yScale, "text");
    }

    protected function spriteWasTextuallyEdited () :void
    {
        CommandEvent.dispatch(this, EditRoomHelper.PROPERTIES_TYPED, _sprite);
    }

    protected var _ctx :MsoyContext;

    protected var _sprite :MsoySprite;

    protected var _xScale :TextInput;
    protected var _yScale :TextInput;

    protected var _x :TextInput;
    protected var _y :TextInput;
    protected var _z :TextInput;
}
}
