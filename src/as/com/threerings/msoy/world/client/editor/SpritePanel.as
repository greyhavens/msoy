package com.threerings.msoy.world.client.editor {

import mx.binding.utils.BindingUtils;

import mx.core.UIComponent;

import mx.containers.VBox;

import mx.controls.TextInput;

import com.threerings.mx.events.CommandEvent;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.Grid;
import com.threerings.msoy.ui.MsoyUI;

import com.threerings.msoy.world.client.MsoySprite;

/**
 * A base class for editing sprites.
 */
public class SpritePanel extends Grid
{
    public function SpritePanel (ctx :MsoyContext)
    {
        _ctx = ctx;
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
        // nada
    }

    /**
     * Called to bind any controls created.
     */
    protected function bind () :void
    {
        updateInputFields();

        // do your binding here
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

    protected var _sprite :MsoySprite;
}
}
