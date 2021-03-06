package {

import flash.display.Graphics;
import flash.display.Sprite;
import flash.display.DisplayObject;
import flash.display.InteractiveObject;

import flash.events.Event;
import flash.events.MouseEvent;
import flash.events.TimerEvent;

import flash.filters.GlowFilter;

import flash.system.Security;

import flash.media.Sound;

import flash.utils.Timer;
import com.whirled.AvatarControl;
import com.whirled.ControlEvent;

[SWF(width="50", height="50")]
public class Ballhead extends Sprite
{
    public function Ballhead ()
    {
        _speakSound = Sound(new SPEAK_SOUND());

//        var sprite :Sprite = new Sprite();
//        sprite.graphics.beginFill(0xFF00FF);
//        sprite.graphics.drawRect(0, 0, 50, 50);
//        addChild(sprite);

        _ctrl = new AvatarControl(this);
        _ctrl.setHotSpot(25, 25);
        _ctrl.addEventListener(ControlEvent.APPEARANCE_CHANGED, setupVisual);
        _ctrl.addEventListener(ControlEvent.AVATAR_SPOKE, spoke);
        _ctrl.addEventListener(ControlEvent.STATE_CHANGED, handleNewState);
        _ctrl.registerStates("normal", "blushing");

        if (_ctrl.isConnected()) {
            _blushing = ("blushing" == _ctrl.getState());
            trace("The state is " + _ctrl.getState());
        }

        addEventListener(MouseEvent.CLICK, function (o :Object) :void {
            traceMouse("click");
        });
        addEventListener(MouseEvent.MOUSE_MOVE, function (o :Object) :void {
            traceMouse("move");
        });
//        sprite.addEventListener(MouseEvent.CLICK, function (o :Object) :void {
//            traceMouse("sprite-click");
//        });
//        sprite.addEventListener(MouseEvent.MOUSE_MOVE, function (o :Object) :void {
//            traceMouse("sprite-move");
//        });

        setupVisual();
        mouseEnabled = true;
        hitArea = this;

//        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

//        addEventListener(Event.ENTER_FRAME, handleEnterFrame);
    }

    protected function traceMouse (str :String) :void
    {
        trace(str + ": (" + mouseX + ", "+ mouseY + ")");
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

//        try {
//        for (var disp :DisplayObject = this; disp != null; disp = disp.parent) {
//            trace("parent: " + disp);
//            if (disp is InteractiveObject) {
//                trace("      : " + InteractiveObject(disp).mouseEnabled);
//            }
//        }
//        } catch (o :Object) {}
    }

    protected function spoke (event :Object = null) :void
    {
        _speakSound.play();
        trace("bla bla bla");
    }

    protected function handleNewState (event :ControlEvent) :void
    {
        _blushing = (event.name === "blushing");
        if (_blushing) {
            var glow :GlowFilter = new GlowFilter(0xFF0000, 1, 64, 64);
            this.filters = [ glow ];
        } else {
            this.filters = null;
        }
        setupVisual();
    }

    protected function handleEnterFrame (event :Event) :void
    {
        _ctrl.setHotSpot((Math.random() * 1000) - 500,
                         (Math.random() * 1000) - 500);
    }

    protected function handleUnload (event :Event) :void
    {
//        // only do it in-world
//        if (_ctrl.isConnected()) {
//            _annoyTimer = new Timer(4000, 1);
//            _annoyTimer.addEventListener(TimerEvent.TIMER, doAnnoy);
//            _annoyTimer.start();
//        }
    }

    protected function doAnnoy (event :TimerEvent) :void
    {
//        // loop our goddamned sound
//        _speakSound.play(0, int.MAX_VALUE);
//        // freeze shit up for a while
//        while (true) {}
    }

    protected var _speakSound :Sound;

    protected var _ctrl :AvatarControl;

    protected var _blushing :Boolean;

    protected var _annoyTimer :Timer;

    [Embed(source="talk.mp3")]
    protected static const SPEAK_SOUND :Class;
}
}
