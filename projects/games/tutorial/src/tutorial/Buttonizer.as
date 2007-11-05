//
// $Id$

package tutorial {

import flash.display.InteractiveObject;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.ColorTransform;
import flash.geom.Matrix;

/**
 * This class turns any existing display object and turns it into an ez-button by adjusting
 * transformations as the user mouse-overs and mouse-clicks. TODO: custom 'hand' pointer but
 * alas this looks annoyingly complicated.
 */
public class Buttonizer extends Sprite
{
    public function Buttonizer (content :InteractiveObject)
    {
        _pane = new Sprite();
        _pane.addChild(content);
        this.addChild(_pane);

        _downMatrix = new Matrix(1, 0, 0, 1, 3, 3);
        _overColor = new ColorTransform(1.1, 1.1, 1.1);
        this.addEventListener(Event.ADDED_TO_STAGE, start);
    }

    protected function start (event :Event) :void
    {
        _pane.removeEventListener(Event.ADDED_TO_STAGE, start);
        _pane.addEventListener(Event.REMOVED_FROM_STAGE, end);

        _pane.addEventListener(MouseEvent.MOUSE_DOWN, handleMouseDown);
        _pane.addEventListener(MouseEvent.MOUSE_UP, handleMouseUp);
        _pane.addEventListener(MouseEvent.ROLL_OVER, handleRollOver);
        _pane.addEventListener(MouseEvent.ROLL_OUT, handleRollOut);
    }

    protected function end (event :Event) :void
    {
        _pane.removeEventListener(MouseEvent.MOUSE_DOWN, handleMouseDown);
        _pane.removeEventListener(MouseEvent.MOUSE_UP, handleMouseUp);
        _pane.removeEventListener(MouseEvent.ROLL_OVER, handleRollOver);
        _pane.removeEventListener(MouseEvent.ROLL_OUT, handleRollOut);
    }

    protected function handleMouseDown (event :Event) :void
    {
        _down = true;
        update();
    }

    protected function handleMouseUp (event :Event) :void
    {
        _down = false;
        update();
    }

    protected function handleRollOver (event :Event) :void
    {
        _over = true;
        update();
    }

    protected function handleRollOut (event :Event) :void
    {
        _over = false;
        update();
    }

    protected function update () :void
    {
        _pane.transform.matrix = _down ? _downMatrix : new Matrix();
        _pane.transform.colorTransform = _over ? _overColor : new ColorTransform();
    }

    protected var _pane :Sprite;

    protected var _down :Boolean = false;
    protected var _over :Boolean = false;

    protected var _downMatrix :Matrix;
    protected var _overColor :ColorTransform;
}
}
