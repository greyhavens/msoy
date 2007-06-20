//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.msoy.world.client.FurniSprite;
import com.threerings.msoy.world.client.MsoySprite;
import com.threerings.msoy.world.client.RoomView;
import com.threerings.msoy.world.data.MsoyLocation;


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
        _border = new Shape();
        _controller.roomView.addChild(_border);
    }
    
    public function end () :void
    {
        _controller.roomView.removeChild(_border);
        _border = null;
    }

    public function get target () :MsoySprite
    {
        return _target;
    }
    
    /** Displays or hides a hover rectangle around the specified sprite. */
    public function set target (sprite :MsoySprite) :void
    {
        _target = sprite as FurniSprite;
        updateBorder();
    }

    /** Updates the UI displayed over the tracked sprite */
    protected function updateBorder () :void
    {
        _border.graphics.clear();

        if (_target != null) {
            _border.x = target.x;
            _border.y = target.y;
            repaintBorder();
        }
    }

    /** Assuming a clear border shape, draws the border details. */
    protected function repaintBorder () :void
    {
        var g :Graphics = _border.graphics;
        var w :Number = target.getActualWidth();
        var h :Number = target.getActualHeight();

        // draw outer and inner outlines
        g.lineStyle(0, 0x000000, 0.5, true);
        g.drawRect(0, 0, w, h);
        g.drawRect(-2, -2, w + 4, h + 4);

        // draw center lines
        g.lineStyle(0, 0xffffff, 1, true);
        g.drawRect(-1, -1, w + 2, h + 2);

    }

    /** Pointer back to the controller. */
    protected var _controller :RoomEditorController;

    /** MsoySprite which the user is targeting. */
    protected var _target :FurniSprite;
    
    /** Sprite that contains a UI to display over the target. */
    protected var _border :Shape;

}
}
