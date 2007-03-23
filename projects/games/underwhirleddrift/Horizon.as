package {

import flash.display.Sprite;
import flash.display.DisplayObject;

import flash.events.Event;

public class Horizon extends Sprite
{
    public function Horizon (image :Class, camera :Camera) 
    {
        addChild(_image = new Sprite());
        var child :Sprite = new image();
        _image.addChild(child);
        child = new image();
        child.x -= UnderwhirledDrift.DISPLAY_WIDTH;
        _image.addChild(child);
        child = new image();
        child.x -= UnderwhirledDrift.DISPLAY_WIDTH * 2;
        _image.addChild(child);

        _camera = camera;

        UnderwhirledDrift.registerEventListener(this, Event.ENTER_FRAME, enterFrame);
    }

    protected function enterFrame (evt :Event) :void 
    {
        var angle :Number = (Math.PI * 2 - _camera.angle) % (Math.PI * 2);
        var percent :Number = (angle % ANGULAR_DISTANCE) / ANGULAR_DISTANCE;
        _image.x = percent * UnderwhirledDrift.DISPLAY_WIDTH;
    }

    // make the image (which should be the same width as the DISPLAY_WIDTH fit into the horizon
    // 4 times in a full circle
    protected static const ANGULAR_DISTANCE :Number = (Math.PI * 2) / 4;

    protected var _camera :Camera;
    protected var _image :Sprite;
}
}
