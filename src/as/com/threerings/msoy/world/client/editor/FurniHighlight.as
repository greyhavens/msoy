//
// $Id$

package com.threerings.msoy.world.client.editor {

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;

import com.threerings.flash.MediaContainer;
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
        _border = new Sprite();
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
        dashRect(g, 0, 0, w, h);
    }

    protected static function dashRect (g :Graphics, x :Number, y :Number,
                                        width :Number, height :Number,
                                        dashLength :Number = 5, spaceLength :Number = 5) :void
    {
        dashTo(g, x, y, x + width, y);
        dashTo(g, x + width, y, x + width, y + height);
        dashTo(g, x + width, y + height, x, y + height);
        dashTo(g, x, y + height, x, y);
    }

    protected static function dashTo (g :Graphics, x1 :Number, y1 :Number, x2 :Number, y2 :Number,
                                      dashLength :Number = 5, spaceLength :Number = 5) :void
    {
        var dx :Number = (x2 - x1), dy :Number = (y2 - y1);
        var length :Number = Math.sqrt(dx*dx + dy*dy);
        var units :Number = length/(dashLength+spaceLength);
        var dashSpaceRatio :Number = dashLength/(dashLength+spaceLength);
        var dashX :Number = (dx/units)*dashSpaceRatio, dashY :Number = (dy/units)*dashSpaceRatio;
        var spaceX :Number = (dx/units)-dashX, spaceY :Number = (dy/units)-dashY;

        g.moveTo(x1, y1);
        while (length > 0) {
            x1 += dashX;
            y1 += dashY;
            length -= dashLength;
            if (length < 0) {
                x1 = x2;
                y1 = y2;
            }
            g.lineTo(x1, y1);
            x1 += spaceX;
            y1 += spaceY;
            g.moveTo(x1, y1);
            length -= spaceLength;
        }
        g.moveTo(x2, y2);
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
    protected var _border :Sprite;

}
}
