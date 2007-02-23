package {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.text.TextField;

import flash.events.Event;
import flash.events.TimerEvent;

import flash.media.Sound;
import flash.media.SoundChannel;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import flash.utils.getTimer; // function import
import flash.utils.ByteArray;
import flash.utils.Timer;

import com.threerings.util.MediaContainer;
import com.threerings.util.EmbeddedSwfLoader;
import com.threerings.util.FPSDisplay;

import minigames.keyjam.KeyJam;


[SWF(width="500", height="700")]
public class Chiyogami extends Sprite
{
    public function Chiyogami ()
    {
        addEventListener(Event.ADDED_TO_STAGE, checkSetup);
        addEventListener(Event.REMOVED_FROM_STAGE, shutdown);

        addChild(new Background(500, 500));

        var o :Object = new ControlBackend();

//        var fps :FPSDisplay = new FPSDisplay();
//        fps.y = 0;
//        addChild(fps);

        _loader = new Loader();
        _bossController.init(_loader);
        _loader.contentLoaderInfo.addEventListener(Event.COMPLETE, bossLoaded);

        var context :LoaderContext = new LoaderContext();
        context.applicationDomain = ApplicationDomain.currentDomain;
        var bytes :ByteArray = ByteArray(new BOSS());
        _loader.loadBytes(bytes, context);
    }

    protected function bossLoaded (evnt :Event) :void
    {
        _media = _loader.content;

        checkSetup();
    }

    /**
     * Called when we're added to the stage.
     */
    protected function checkSetup (evt :Object = null) :void
    {
        if (stage == null || _media == null) {
            return; // not yet
        }

        _sound = Sound(new MUSIC());

        _media.x = (500 - _media.width) / 2;
        _media.y = (500 - _media.height) / 2;
        addChild(_media);

        var minigame :KeyJam = new KeyJam();
        minigame.y = 500;
        addChild(minigame);

        _bossUpdateTimer = new Timer(4000, 1);
        _bossUpdateTimer.addEventListener(TimerEvent.TIMER, kickOffBattle);
        _bossUpdateTimer.start();
    }

    protected function kickOffBattle (evt :Object = null) :void
    {
        _bossController.setWalking(true);

        _channel = _sound.play(0, int.MAX_VALUE);

        // make sure we're kosh
        if (_channel == null) {
            var tf :TextField = new TextField();
            tf.text = "No sound for you. So Sorry!";
            tf.width = tf.textWidth + 5;
            addChild(tf);
        }

        _timeBase = getTimer();
        _phase = 0;
        addEventListener(Event.ENTER_FRAME, handleFrame);
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
        var curTime :Number = getTimer();
        var curDur :Number = curTime - _timeBase;
        while (curDur > _millisPerBeat) {
            curDur -= _millisPerBeat;
            _timeBase += _millisPerBeat;
            _phase = (_phase + 1) % 4;
            if (_phase == 0) {
                _bossController.toggleFacing();
            }
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

        var radians :Number = Math.atan2(rightDip - leftDip, _media.width);
        var rot :Number = radians * 180 / Math.PI;
        _media.y = leftDip;
        _media.rotation = rot;

//        trace("peaks: " + left + "  " + right);
//        trace("radians: " + radians + "  "+ _media.rotation);

        // maybe do some squishing?
        var avg :Number = (left + right) / 2;
        if (avg > .75) {
            _media.scaleY = .75 + (1 - avg);
        } else {
            _media.scaleY = 1;
        }
    }

    protected var _media :DisplayObject;
    protected var _sound :Sound;
    protected var _channel :SoundChannel;

    protected var _loader :Loader;

    protected var _bossController :BossController = new BossController();

    //protected static const BPM :Number = 115.4; // for fetts-vette
    //protected static const BPM :Number = 73.0; // for BollWeevil
    protected static const BPM :Number = 140.2; // for tarzan

    protected var _millisPerBeat :Number = 60 * 1000 / BPM;

    protected var _timeBase :Number;

    protected var _bossUpdateTimer :Timer;

    /** 0 - 3 for the 4 types of bounce. */
    protected var _phase :int;

    [Embed(source="bboy.swf", mimeType="application/octet-stream")]
    protected static var BOSS :Class;

    //[Embed(source="kawaii_sword.swf")]
//    protected var AVATAR_2 :Class = AVATAR_1;

    //[Embed(source="meg lee chin - heavy scene.mp3")]
    //[Embed(source="5-fetts_vette.mp3")]
    //[Embed(source="BollWeevil.mp3")]
    [Embed(source="tarzan.mp3")]
    protected var MUSIC :Class;

    protected static const DANCE_AMPLITUDE :int = 50;
}
}
