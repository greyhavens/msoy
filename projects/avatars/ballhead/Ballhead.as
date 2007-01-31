package {

import flash.display.Graphics;
import flash.display.Sprite;

import flash.media.Sound;

import com.threerings.msoy.export.AvatarControl;
import com.threerings.msoy.export.ControlEvent;

[SWF(width="50", height="50")]
public class Ballhead extends Sprite
{
    public function Ballhead ()
    {
        _speakSound = Sound(new SPEAK_SOUND());

        _ctrl = new AvatarControl(this);
        _ctrl.addEventListener(ControlEvent.APPEARANCE_CHANGED, setupVisual);
        _ctrl.addEventListener(ControlEvent.AVATAR_SPOKE, spoke);
        _ctrl.addEventListener(ControlEvent.ACTION_TRIGGERED, handleAction);
        _ctrl.setActions("start blushing", "stop blushing");

        setupVisual();
    }

    protected function setupVisual (event :Object = null) :void
    {
        var orient :Number = _ctrl.getOrientation();
        var walking :Boolean = _ctrl.isMoving();

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

    protected function spoke (event :Object = null) :void
    {
        _speakSound.play();
    }

    protected function handleAction (event :ControlEvent) :void
    {
        _blushing = (event.name === "start blushing");
        setupVisual();
    }

    protected var _speakSound :Sound;

    protected var _ctrl :AvatarControl;

    protected var _blushing :Boolean;

    [Embed(source="talk.mp3")]
    protected static const SPEAK_SOUND :Class;
}
}
