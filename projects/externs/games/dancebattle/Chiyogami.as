package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.text.TextField;

import flash.events.Event;

import flash.media.Sound;
import flash.media.SoundChannel;

import flash.utils.getTimer; // function import

import com.threerings.util.MediaContainer;
import com.threerings.util.FPSDisplay;

[SWF(width="400", height="400")]
public class Chiyogami extends Sprite
{
    public function Chiyogami ()
    {
        addEventListener(Event.ADDED_TO_STAGE, setup);
        addEventListener(Event.REMOVED_FROM_STAGE, shutdown);

        addChild(new DiscoMoire(400, 400));

        var fps :FPSDisplay = new FPSDisplay();
        fps.y = 40;
        addChild(fps);

//        var spr :Sprite = new Sprite();
//        spr.graphics.beginFill(0x34FF67);
//        spr.graphics.drawRect(0, 0, 100, 100);
//        spr.graphics.endFill();
//        _media = spr;

        for (var ii :int = 0; ii < 1; ii++) {
            var c :Class = (Math.random() > .5) ? AVATAR_1 : AVATAR_2;
            _media[ii] = DisplayObject(new c());
        }
    }

    /**
     * Called when we're added to the stage.
     */
    protected function setup (evt :Object = null) :void
    {
//        _sound = Sound(new MUSIC());
//        _channel = _sound.play(0, int.MAX_VALUE);

        // make sure we're kosh
        if (_channel == null) {
            var tf :TextField = new TextField();
            tf.text = "No sound for you. So Sorry!";
            tf.width = tf.textWidth + 5;
            addChild(tf);
        }

        for (var ii :int = 0; ii < _media.length; ii++) {
            var disp :DisplayObject = DisplayObject(_media[ii]);
            disp.x = (400 - disp.width) / 2;
            disp.y = (400 - disp.height) / 2;
            addChild(disp);
        }

        _timeBase = getTimer();
        _phase = 0;
//        addEventListener(Event.ENTER_FRAME, handleFrame);
    }

    /**
     * Called when we're remove from the stage.
     */
    protected function shutdown (evt :Object = null) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleFrame);
        if (_channel != null) {
            _channel.stop();
            _channel = null;
        }
        _sound = null;

        // remove all children
        for (var ii :int = numChildren - 1; ii >= 0; ii--) {
            removeChildAt(ii);
        }
    }

    protected function handleFrame (evt :Object = null) :void
    {
        var media :DisplayObject;

        var curTime :Number = getTimer();
        var curDur :Number = curTime - _timeBase;
        while (curDur > _millisPerBeat) {
            curDur -= _millisPerBeat;
            _timeBase += _millisPerBeat;
            _phase = (_phase + 1) % 4;
        }

        /** Scale the time duration into radians */
        var piece :Number = curDur * 2 * Math.PI / _millisPerBeat;

        var left :Number;
        var right :Number;

        switch (_phase) {
        case 0:
        case 1:
        case 2:
            left = Math.sin(piece);
            break;

        case 3:
            left = 0;
            break;
        }

        switch (_phase) {
        case 0:
        case 2:
        case 3:
            right = Math.sin(piece);
            break;

        case 1:
            right = 0;
            break;
        }

        if (_channel != null) {
            left *= Math.min(1, _channel.leftPeak / .75);
            right *= Math.min(1, _channel.rightPeak / .75);
        }

        var leftDip :Number = left * DANCE_AMPLITUDE;
        var rightDip :Number = right * DANCE_AMPLITUDE;

        for each (media in _media) {
            var radians :Number = Math.atan2(rightDip - leftDip, media.width);
            var rot :Number = radians * 180 / Math.PI;
            media.y = leftDip;
            media.rotation = rot;
        }

//        trace("peaks: " + left + "  " + right);
//        trace("radians: " + radians + "  "+ _media.rotation);

        // maybe do some squishing?
        var avg :Number = (left + right) / 2;
        if (avg > .75) {
            for each (media in _media) {
                media.scaleY = .75 + (1 - avg);
            }
        } else {
            for each (media in _media) {
                media.scaleY = 1;
            }
        }
    }

    protected var _media :Array = [];
    protected var _sound :Sound;
    protected var _channel :SoundChannel;

    //protected static const BPM :Number = 115.4; // for fetts-vette
    //protected static const BPM :Number = 73.0; // for BollWeevil
    protected static const BPM :Number = 140.2; // for tarzan

    protected var _millisPerBeat :Number = 60 * 1000 / BPM;

    protected var _timeBase :Number;

    /** 0 - 3 for the 4 types of bounce. */
    protected var _phase :int;

    [Embed(source="guest.swf")]
    protected var AVATAR_1 :Class;

    //[Embed(source="kawaii_sword.swf")]
    protected var AVATAR_2 :Class = AVATAR_1;

    //[Embed(source="meg lee chin - heavy scene.mp3")]
    //[Embed(source="5-fetts_vette.mp3")]
    //[Embed(source="BollWeevil.mp3")]
    [Embed(source="tarzan.mp3")]
    protected var MUSIC :Class;

    protected static const DANCE_AMPLITUDE :int = 50;
}
}
