package com.threerings.msoy.world.client.editor {

import mx.binding.utils.BindingUtils;

import mx.core.UIComponent;

import mx.containers.VBox;

import mx.controls.CheckBox;
import mx.controls.TextInput;

import com.threerings.util.CommandEvent;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.world.client.MsoySprite;

/**
 * A base class for editing sprites.
 */
public class SpritePanel extends Grid
{
    public function init (ctx :MsoyContext, ctrl :EditorController) :void
    {
        _ctx = ctx;
        _ctrl = ctrl;
    }

    /**
     * Set the sprite to be edited.
     */
    public function setSprite (sprite :MsoySprite) :void
    {
        _sprite = sprite;

        if (processedDescriptors) {
            bind();
        }
    }

    /**
     * Called when we set up and when the sprite is changed
     * by the mousing editor controls.
     */
    public function updateInputFields () :void
    {
        var centering :Boolean = _ctrl.getCentering();
        if (centering != _centering.selected) {
            _centering.selected = centering;
        }

        _locEditor.setSprite(_sprite);
    }

    /**
     * Called to bind any controls created.
     */
    protected function bind () :void
    {
        updateInputFields();

        BindingUtils.bindSetter(function (state :Boolean) :void {
            _ctrl.setCentering(state);
        }, _centering, "selected");
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addRow(
            MsoyUI.createLabel(Msgs.EDITING.get("l.center")),
            _centering = new CheckBox());

        // location: big controls
        addRow(
            MsoyUI.createLabel(Msgs.EDITING.get("l.loc")),
            _locEditor = new LocationEditor(_ctx));
    }

    override protected function childrenCreated () :void
    {
        super.childrenCreated();

        if (_sprite != null) {
            bind();
        }
    }

    /**
     * This should be called after any fields have been edited that
     * modify the sprite. It will dispatch an event that will cause the
     * controller to re-render the sprite.
     */
    protected function spritePropsUpdated () :void
    {
        CommandEvent.dispatch(
            this, EditorController.SPRITE_PROPS_UPDATED, _sprite);
    }

    protected var _ctx :MsoyContext;

    protected var _ctrl :EditorController;

    protected var _sprite :MsoySprite;

    /** A checkbox used to select whether we'd like to center on this sprite. */
    protected var _centering :CheckBox;

    /** The location editor control. */
    protected var _locEditor :LocationEditor;
}
}
