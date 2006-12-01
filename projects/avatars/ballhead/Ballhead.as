package {

import flash.display.Graphics;
import flash.display.Sprite;

import flash.media.Sound;

import com.threerings.msoy.export.AvatarInterface;

[SWF(width="50", height="50")]
public class Ballhead extends Sprite
{
    public function Ballhead ()
    {
        _speakSound = Sound(new SPEAK_SOUND());

        _iface = new AvatarInterface(this);
        _iface.avatarChanged = setupVisual;
        _iface.avatarSpoke = spoke;
        _iface.addAction("start blushing", startBlushing);
        _iface.addAction("stop blushing", stopBlushing);

        setupVisual();
    }

    protected function setupVisual () :void
    {
        var orient :Number = _iface.getOrientation();
        var walking :Boolean = _iface.isWalking();

        graphics.clear();

        var color :uint = _blushing ? (walking ? 0xFF9933 : 0x993333)
                                    : (walking ? 0x33FF99 : 0x339933);
        graphics.beginFill(color);
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

    protected function spoke () :void
    {
        _speakSound.play();
    }

    protected function startBlushing () :void
    {
        _blushing = true;
        setupVisual();
    }

    protected function stopBlushing () :void
    {
        _blushing = false;
        setupVisual();
    }

    protected var _speakSound :Sound;

    protected var _iface :AvatarInterface;

    protected var _blushing :Boolean;

    [Embed(source="talk.mp3")]
    protected static const SPEAK_SOUND :Class;
}
}
