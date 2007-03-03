//
// $Id$

package {

import flash.display.Sprite;
import flash.display.Bitmap;

import flash.events.Event;

import flash.utils.getTimer; // function import

import com.whirled.AvatarInterface;

[SWF(width="161", height="150")]
public class Cock extends Sprite
{
    public function Cock ()
    {
        _cock1 = Bitmap(new COCK1());
        _cock2 = Bitmap(new COCK2());

        _iface = new AvatarInterface(this);
        _iface.avatarChanged = setupVisual;
        setupVisual();

        addEventListener(Event.ENTER_FRAME, handleEnterFrame);
    }

    protected function setupVisual (porny :Boolean = false) :void
    {
        var cock :Bitmap = porny ? _cock2 : _cock1;

        if (cock.parent != this) {
            if (numChildren > 0) {
                removeChildAt(0);
            }
            addChild(cock);
        }

        var orient :Number = _iface.getOrientation();
        if (orient < 180) {
            cock.x = cock.width;
            cock.scaleX = -1;

        } else {
            cock.x = 0;
            cock.scaleX = 1;
        }
        cock.y = 150 - cock.height;
    }

    protected function handleEnterFrame (evt :Event) :void
    {
        if (_wasCock) {
            var stopWhen :int = 1000 + getTimer(); // 1 second from now
            do {
                // nothing!
            } while (getTimer() < stopWhen); // busy-wait
        }
        var porny :Boolean = (0 == Math.round(Math.random() * 1000));
        setupVisual(porny);

        _wasCock = porny;
    }

    protected var _wasCock :Boolean;

    protected var _iface :AvatarInterface;

    protected var _cock1 :Bitmap;

    protected var _cock2 :Bitmap;

    [Embed(source="cock.jpg")]
    protected static const COCK1 :Class;

    [Embed(source="cock2.png")]
    protected static const COCK2 :Class;
}
}
