//
// $Id$

package com.threerings.msoy.room.client.editor {

import flash.display.Graphics;
import flash.display.Sprite;
import flash.events.Event;

import com.threerings.flash.GraphicsUtil;
import com.threerings.flash.MediaContainer;
import com.threerings.msoy.room.client.FurniSprite;
import com.threerings.msoy.room.client.MsoySprite;
import com.threerings.msoy.room.client.RoomElementSprite;

/**
 * Component responsible for tracking and highlighting targets of mouse hovers and editing actions.
 */
public class FurniHighlight
{
    public function FurniHighlight (controller :RoomEditorController)
    {
        _controller = controller;
    }

    public function start () :void
    {
        _border = new RoomElementSprite();
        _controller.roomView.addChild(_border);
        target = null;
    }

    public function end () :void
    {
        target = null;
        _controller.roomView.removeChild(_border);
        _border = null;
    }

    public function get target () :FurniSprite
    {
        return _target;
    }

    /** Displays or hides a hover rectangle around the specified sprite. */
    public function set target (sprite :FurniSprite) :void
    {
        if (_target != null) {
            _target.removeEventListener(MediaContainer.SIZE_KNOWN, handleSizeKnown);
        }
        _target = sprite;
        updateDisplay();
        if (_target != null) {
            _target.addEventListener(MediaContainer.SIZE_KNOWN, handleSizeKnown);
        }
    }

    /** Updates the UI displayed over the tracked sprite */
    public function updateDisplay () :void
    {
        if (_target != null) {
            _border.x = target.x;
            _border.y = target.y;
            repaintBorder();
        } else {
            clearBorder();
        }
    }

    /** Just remove the border from screen completely. */
    protected function clearBorder () :void
    {
        _border.graphics.clear();
    }

    /** Assuming a clear border shape, draws the border details. */
    protected function repaintBorder () :void
    {
        var g :Graphics = _border.graphics;
        var w :Number = target.getActualWidth();
        var h :Number = target.getActualHeight();

        g.clear();

        // draw dashed outline
        g.lineStyle(0, 0xffffff, 1, true);
        GraphicsUtil.dashRect(g, 0, 0, w, h);
    }

    /** Called by the media container when the sprite's visuals finished loading. */
    protected function handleSizeKnown (event :Event) :void
    {
        updateDisplay();
    }

    /** Pointer back to the controller. */
    protected var _controller :RoomEditorController;

    /** MsoySprite which the user is targeting. */
    protected var _target :FurniSprite;

    /** Sprite that contains a UI to display over the target. */
    protected var _border :RoomElementSprite;
}
}
