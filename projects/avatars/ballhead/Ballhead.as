package {

import flash.display.Graphics;
import flash.display.Sprite;

import com.threerings.msoy.export.AvatarInterface;

[SWF(width="50", height="50")]
public class Ballhead extends Sprite
{
    public function Ballhead ()
    {
        _iface = new AvatarInterface(this);
        _iface.avatarChanged = setupVisual;
        setupVisual();
    }

    protected function setupVisual () :void
    {
        var orient :Number = _iface.getOrientation();
        var walking :Boolean = _iface.isWalking();

        graphics.clear();
        graphics.beginFill(walking ? 0x33FF99 : 0x339933);
        graphics.drawCircle(25, 25, 25);
        graphics.endFill();

        // convert the msoy orient into the right radians
        var radians :Number = (orient - 90) * Math.PI / 180;

        // draw a little line indicating direction.
        graphics.lineStyle(2.2, 0x000000);
        graphics.moveTo(25, 25);
        graphics.lineTo(Math.cos(radians) * 25 + 25,
            Math.sin(radians) * -25 + 25);
    }

    protected var _iface :AvatarInterface;
}
}
