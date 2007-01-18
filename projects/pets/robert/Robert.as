//
// $Id$

package {

import flash.display.Bitmap;
import flash.display.Sprite;

import com.threerings.msoy.export.FurniControl;

/**
 * Robert is a plant. He likes sunshine and water.
 */
[SWF(width="303", height="351")]
public class Robert extends Sprite
{
    public static const NORMAL :int = 0;
    public static const SMALL :int = 1;
    public static const TINY :int = 2;

    public static const WIDTH :int = 303;
    public static const HEIGHT :int = 351;

    public function Robert ()
    {
        // instantiate and wire up our control
        _ctrl = new FurniControl(this);
        _ctrl.eventTriggered = eventTriggered;
        _ctrl.memoryChanged = memoryChanged;
        _ctrl.tick = tick;
        _ctrl.setTickInterval(1000);

        // start out in the appropriate state
        updateState(_ctrl.lookupMemory("state", SMALL));
    }

    protected function updateState (value :Object) :void
    {
        var state :int = int(value);
        if (_image != null) {
            removeChild(_image);
            _image = null;
        }
        switch (state) {
        case NORMAL:
            _image = Bitmap(new NORMAL_IMAGE());
            break;
        case SMALL:
            _image = Bitmap(new SMALL_IMAGE());
            break;
        case TINY:
            _image = Bitmap(new TINY_IMAGE());
            break;
        }
        if (_image != null) {
            _image.x = (WIDTH - _image.width)/2;
            _image.y = (HEIGHT - _image.height);
            addChild(_image);
        }
    }

    protected function tick () :void
    {
        _state = (_state + 1) % 3;
        _ctrl.updateMemory("state", _state);
    }

    protected function eventTriggered (event :String) :void
    {
        trace("event triggered: " + event);
    }

    protected function memoryChanged (key :String, value :Object) :void
    {
        trace("memory changed: " + key + " -> " + value);
        if (key == "state") {
            updateState(value);
        }
    }

    protected var _ctrl :FurniControl;
    protected var _image :Bitmap;
    protected var _state :int;

    [Embed(source="normal.png")]
    protected static const NORMAL_IMAGE :Class;

    [Embed(source="small.png")]
    protected static const SMALL_IMAGE :Class;

    [Embed(source="tiny.png")]
    protected static const TINY_IMAGE :Class;
}
}
