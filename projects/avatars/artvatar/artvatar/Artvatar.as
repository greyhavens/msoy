//
// $Id$
//
// Avatar with built-in doodling functionality

package artvatar {

import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;
import flash.events.TimerEvent;

import com.whirled.AvatarControl;
import com.whirled.ControlEvent;

[SWF(width="400", height="450")]
public class Artvatar extends Sprite
{
    public static const DRAW_ACTION :String = "Drawl!";

    public function Artvatar ()
    {
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        addEventListener(Event.ENTER_FRAME, handleEnterFrame);
        handleEnterFrame();

        // create a temporary visualization
        var box :Shape = new Shape();
        box.graphics.beginFill(uint(0xFFCC99));
        box.graphics.drawRect(-50, 0, 50, 100);
        box.graphics.endFill();
        box.x = 200;
        box.y = (450-100);
        addChild(box);

        // wire up our distributed business
        _control = new AvatarControl(this);
        _control.addEventListener(ControlEvent.ACTION_TRIGGERED, handleActionTriggered);
        // only the "Draw!" action is a menu item, done is dispatched programmatically
        _control.registerActions(DRAW_ACTION);

        if (!_control.isConnected()) {
            startDrawing();
        }
    }

    /**
     * This is called when the user selects an action from the menu.
     */
    protected function handleActionTriggered (event :ControlEvent) :void
    {
        switch (event.name) {
        case DRAW_ACTION:
            startDrawing();
            break;
        }
    }

    protected function handleEnterFrame (evt :Event = null) :void
    {
        // TODO
    }

    protected function handleUnload (event :Event) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleEnterFrame);
    }

    protected function startDrawing () :void
    {
        if (_canvas == null) {
            // wire up our canvas widget which will capture drawing commands and display them
            addChild(_canvas = new Canvas());
            // wire up our control panel which will configure the canvas
            addChild(_cpanel = new ControlPanel(_canvas, finishDrawing));
            // position our control panel below our canvas
            _cpanel.x = _canvas.x;
            _cpanel.y = (_canvas.height + 10);
        }
    }

    protected function finishDrawing (sendResults :Boolean) :void
    {
        // TODO: have the canvas summarize the drawing and then dispatch it as an event

        if (_canvas != null) {
            removeChild(_canvas);
            removeChild(_cpanel);
            _canvas = null;
            _cpanel = null;
        }
    }

    protected var _control :AvatarControl;
    protected var _canvas :Canvas;
    protected var _cpanel :ControlPanel;
}
}
