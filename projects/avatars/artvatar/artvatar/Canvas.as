//
// $Id$
//
// Avatar with built-in doodling functionality

package artvatar {

import flash.display.Shape;
import flash.display.SimpleButton;
import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.utils.ByteArray;

/**
 * Captures and records drawing commands (mouse presses and drags), displaying them as the user
 * draws and summarizing them for broadcast to other users.
 */
public class Canvas extends Sprite
{
    public static const MOVE_TO :int = 0;
    public static const LINE_TO :int = 1;
    public static const SET_COLOR :int = 2;
    public static const SET_STYLE :int = 3;

    public function Canvas ()
    {
        // listen for the requisite mouse events
        addEventListener(MouseEvent.MOUSE_DOWN, mousePressed);
        addEventListener(MouseEvent.MOUSE_UP, mouseReleased);
        addEventListener(MouseEvent.MOUSE_MOVE, mouseMoved);
        addEventListener(MouseEvent.MOUSE_OUT, mouseOut);
        addEventListener(MouseEvent.MOUSE_OVER, mouseOver);

        // fill our pixels so that we get mouse events
        _drawing.graphics.beginFill(uint(0xFFFFFF), 0);
        _drawing.graphics.drawRect(0, 0, 250, 250);
        _drawing.graphics.endFill();

        // draw an outline so that the user knows where to draw
        _drawing.graphics.lineStyle(1, uint(0x000000));
        _drawing.graphics.drawRect(0, 0, 250, 250);

        addChild(_drawing);
    }

    protected function mousePressed (event :MouseEvent) :void
    {
        // update the local display
        _mouseDown = true;
        _drawing.graphics.moveTo(event.localX, event.localY);

        // if someone runs this on a screen larger than 32k across or down, antics will ensue
        _commands.writeByte(MOVE_TO);
        _commands.writeShort(_mx = event.localX);
        _commands.writeShort(_my = event.localY);
    }

    protected function mouseReleased (event :MouseEvent) :void
    {
        // update the local display
        _mouseDown = false;
        _drawing.graphics.lineTo(event.localX, event.localY);

        // compute the necessary business
        generateLineCommands(event.localX, event.localY);

        // clear out our position marker
        _mx = _my = 0;
    }

    protected function mouseMoved (event :MouseEvent) :void
    {
        if (_mouseDown) {
            _drawing.graphics.lineTo(event.localX, event.localY);
        }
    }

    protected function mouseOut (event :MouseEvent) :void
    {
        if (_mouseDown) {
            _drawing.graphics.lineTo(event.localX, event.localY);
        }
        _mouseDown = false;
    }

    protected function mouseOver (event :MouseEvent) :void
    {
        _mouseDown = event.buttonDown;
        if (_mouseDown) {
            _drawing.graphics.moveTo(event.localX, event.localY);
        }
    }

    protected function generateLineCommands (nx :int, ny :int) :void
    {
        // determine whether we need to split this into multiple commands
        var dx :int = (nx - _mx);
        var dy :int = (ny - _my);
        if (dx < 128 || dx > 127 || dy < 128 || dy > 127) {
        }
    }

    protected var _drawing :Shape = new Shape();
    protected var _done :SimpleButton = new SimpleButton();

    protected var _mouseDown :Boolean = false;
    protected var _mx :int, _my :int;

    protected var _commands :ByteArray = new ByteArray();
}
}
