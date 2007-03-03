package {

import flash.display.Bitmap;
import flash.display.Sprite;
import flash.display.Graphics;

import com.whirled.PetControl;

/**
 * An extremely simple pet that moves randomly around the room.
 */
[SWF(width="83", height="47")]
public class Roomba extends Sprite
{
    public function Roomba ()
    {
        var name :Array = [];
        for (var ii :int = 0; ii < 6; ii++) {
            name.push(int(Math.random() * 26 + 97)); // lowercase letters
        }
        _name = String.fromCharCode.apply(null, name);

        addChild(Bitmap(new ROOMBA()));
        scaleX = .25;
        scaleY = .25;

        _ctrl = new PetControl(this);
        _ctrl.tick = tick;
        _ctrl.setTickInterval(1000);
    }

    protected function tick () :void
    {
        trace(_name + " ticking");
        _ctrl.setLocation(Math.random(), 0, Math.random(), Math.random());
    }

    protected var _name :String;
    protected var _ctrl :PetControl;

    [Embed(source="roomba.png")]
    protected static const ROOMBA :Class;

//    [Embed(source="schade01.wav")]
//    protected static const BUMP :Class;
}
}
