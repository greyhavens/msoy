//
// $Id$
//
// Disco Butterfly - an avatar for Whirled

package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.geom.Point;

import flash.media.Sound;
import flash.media.SoundChannel;

import flash.utils.getTimer;
import flash.utils.Timer;

import com.threerings.flash.Siner;

import com.whirled.AvatarControl;
import com.whirled.ControlEvent;

[SWF(width="400", height="450")]
public class Butterfly extends Sprite
{
    public function Butterfly ()
    {
        _control = new AvatarControl(this);

        _butterfly = new Sprite();
        var butterImg :DisplayObject = (new BUTTERFLY() as DisplayObject);
        butterImg.x = -20;
        butterImg.y = -20;
        _butterfly.addChild(butterImg);
        addChild(_butterfly);

        _chickenHolder = new Sprite();
        _chickenHolder.addChild(_chickImg);
        _chickenMask = new Sprite();
        _chickImg.mask = _chickenMask;
        _chickenHolder.addChild(_chickenMask);

        _chicken = new Sprite();
        _chickenHolder.x = CHICKEN_W / -2;
        _chicken.addChild(_chickenHolder);

        _butterfly.addChildAt(_chicken, 0);

        _control.addEventListener(ControlEvent.STATE_CHANGED, handleStateChange);
        _control.registerStates("Normal", "Disco");

        _sinerX = new Siner(10, 10, 30, 11);
        _sinerY = new Siner(5, 1, 15, 5, 10, 12);
        _sinerX.randomize();
        _sinerY.randomize();
        _chickenRot = new Siner(720, 4.9);
        addEventListener(Event.ENTER_FRAME, handleEnterFrame);

        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        if (_control.isConnected()) {
            if (_control.getState() == "Disco") {
                // make us already discoing
                _phase = PHASE_DISCO;
                updateChickenStickinOut(1);
                playMusic(true);
            }
        }

        handleEnterFrame();
        // Test harness
//        handleAction(new ControlEvent("foo", "Disco"));
//        var t :Timer = new Timer(10000, 1);
//        t.addEventListener(TimerEvent.TIMER, function (evt :Object) :void {
//            handleAction(new ControlEvent("foo", "Stop Disco"));
//        });
//        t.start();
    }

    /**
     * This is called when the user selects a different state.
     */
    protected function handleStateChange (event :ControlEvent) :void
    {
        var inDisco :Boolean = (_phase == PHASE_DISCO) ||
            (_phase == PHASE_UP) || (_phase == PHASE_PRE_DISCO);
        switch (event.name) {
        case "Disco":
            if (inDisco) {
                return;
            }
            _chicken.rotation = 0;
            _phase = PHASE_UP;
            break;

        default:
            if (!inDisco) {
                return;
            }
            _phase = PHASE_DOWN;
            playMusic(false);
            break;
        }

        // in both cases, start the sound and take a timestamp
        _effect.play();
        _phaseStamp = getTimer();
    }

    protected function handleEnterFrame (evt :Event = null) :void
    {
        switch (_phase) {
        case PHASE_UP:
        case PHASE_DOWN:
            var elapsed :Number = getTimer() - _phaseStamp;
            var perc :Number = Math.min(1, elapsed / (_effect.length - 500));
            if (_phase == PHASE_DOWN) {
                perc = 1 - perc;
            }

            // update the chicken
            updateChickenStickinOut(perc);

            // see if we should now go to a different state
            if (_phase == PHASE_UP) {
                if (perc == 1) {
                    playMusic(true); // the music has some ramp-up time?
                    _phase = PHASE_PRE_DISCO;
                    _phaseStamp = getTimer();
                }

            } else {
                if (perc == 0) {
                    _phase = PHASE_NORMAL;
                }
            }
            break;

        case PHASE_PRE_DISCO:
            if (getTimer() - _phaseStamp > PRE_DISCO_PAUSE) {
                _phase = PHASE_DISCO;
                _chickenRot.reset();
            }
            break;

        case PHASE_DISCO:
            _chicken.rotation = _chickenRot.value;
            _sparkleCounter = (_sparkleCounter + 1) % 2;
            if (_sparkleCounter == 0) {
                var p :Point = _chickImg.localToGlobal(new Point(CHICKEN_W/2, 5));
                p = this.globalToLocal(p);
                var spark :Sparkle = new Sparkle(p.x, p.y, 450);
                addChildAt(spark, 0);
            }
            break;
        }

        var minSparkY :Number = int.MAX_VALUE;
        var childIdx :int;
        var sparkle :Sparkle;
        for (childIdx = numChildren - 1; childIdx >= 0; childIdx--) {
            sparkle = (getChildAt(childIdx) as Sparkle);
            if (sparkle != null) {
                minSparkY = Math.min(minSparkY, sparkle.y);
            }
        }
        // addin' some paddin'
        minSparkY -= 15;
        var oldOffset :Number = _yOffset;
        _yOffset = Math.max(_chickenExtent, CHICKEN_H - Math.max(0, minSparkY));
        var offsetDelta :Number = _yOffset - oldOffset;
        // smoothing...
        if (offsetDelta < -1) {
            _yOffset += (-1 - offsetDelta);
            offsetDelta = -1;
        }
        if (offsetDelta != 0) {
            for (childIdx = numChildren - 1; childIdx >= 0; childIdx--) {
                sparkle = (getChildAt(childIdx) as Sparkle);
                if (sparkle != null) {
                    sparkle.adjustY(offsetDelta);
                }
            }
        }

        // the butterfly always moves
        _butterfly.x = 200 + _sinerX.value;
        _butterfly.y = 50 + _yOffset + _sinerY.value;
        _control.setHotSpot(200, 300 + _yOffset);
    }

    protected function updateChickenStickinOut (perc :Number) :void
    {
        _chickenExtent = CHICKEN_H * perc;
        with (_chickenMask.graphics) {
            clear();
            beginFill(0xFFFFFF);
            drawRect(0, 0, CHICKEN_W, _chickenExtent);
            endFill();
        }
        _chickenHolder.y = -1 * _chickenExtent;
    }

    protected function handleUnload (event :Event) :void
    {
        playMusic(false);
        removeEventListener(Event.ENTER_FRAME, handleEnterFrame);
    }

    protected function playMusic (play :Boolean) :void
    {
        // either way, stop any old music
        if (_musicCtrl != null) {
            _musicCtrl.stop();
            _musicCtrl = null;
        }

        if (play && _music != null) {
            _musicCtrl = _music.play(0, 1000);
        }
    }

    protected var _control :AvatarControl;

    protected var _butterfly :Sprite;

    protected var _chicken :Sprite;

    protected var _chickImg :DisplayObject = (new CHICKEN() as DisplayObject);

    protected var _effect :Sound = (new EFFECT() as Sound);
    protected var _music :Sound = (new MUSIC() as Sound);
    protected var _musicCtrl :SoundChannel;

    protected var _chickenHolder :Sprite;
    protected var _chickenMask :Sprite;

    protected static const PHASE_NORMAL :int = 0;
    protected static const PHASE_UP :int = 1;
    protected static const PHASE_PRE_DISCO :int = 2;
    protected static const PHASE_DISCO :int = 3;
    protected static const PHASE_DOWN :int = 4;
    protected var _phase :int = PHASE_NORMAL;

    protected var _phaseStamp :Number;

    protected var _sparkleCounter :int = 0;

    protected var _sinerX :Siner;
    protected var _sinerY :Siner;
    protected var _chickenRot :Siner;

    // how much is our chicken stickin' out?
    protected var _chickenExtent :Number = 0;

    /** What's our y offset? This will be at least as big as _chickenExtent
     * and more during retraction. */
    protected var _yOffset :Number = 0;

    protected static const CHICKEN_W :int = 28;
    protected static const CHICKEN_H :int = 150;

    protected static const PRE_DISCO_PAUSE :int = 500;

    [Embed(source="radiostar.mp3")]
    protected static const MUSIC :Class;

    [Embed(source="hydraulic_motor.mp3")]
    protected static const EFFECT :Class;

    [Embed(source="butterfly.png")]
    protected static const BUTTERFLY :Class;

    [Embed(source="rubber_chicken.png")]
    protected static const CHICKEN :Class;
}
}
