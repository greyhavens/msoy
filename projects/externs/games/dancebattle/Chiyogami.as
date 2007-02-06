package {

import flash.display.DisplayObject;
import flash.display.Sprite;

import flash.text.TextField;

import flash.events.Event;

import flash.media.Sound;
import flash.media.SoundChannel;

import com.threerings.util.MediaContainer;

[SWF(width="400", height="400")]
public class Chiyogami extends Sprite
{
    public function Chiyogami ()
    {
        addEventListener(Event.ADDED_TO_STAGE, setup);
        addEventListener(Event.REMOVED_FROM_STAGE, shutdown);

//        var spr :Sprite = new Sprite();
//        spr.graphics.beginFill(0x34FF67);
//        spr.graphics.drawRect(0, 0, 100, 100);
//        spr.graphics.endFill();
//        _media = spr;

        _media = DisplayObject(new AVATAR());
    }

    /**
     * Called when we're added to the stage.
     */
    protected function setup (evt :Object = null) :void
    {
        _sound = Sound(new MUSIC());
        _channel = _sound.play(0, int.MAX_VALUE);

        // make sure we're kosh
        if (_channel == null) {
            var tf :TextField = new TextField();
            tf.text = "No sound for you. So Sorry!";
            tf.width = tf.textWidth + 5;
            addChild(tf);

        } else {
            addChild(_media);
            addEventListener(Event.ENTER_FRAME, handleFrame);
        }
    }

    /**
     * Called when we're remove from the stage.
     */
    protected function shutdown (evt :Object = null) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleFrame);
        _channel.stop();
        _channel = null;
        _sound = null;

        removeChild(_media);
    }

    protected function handleFrame (evt :Object = null) :void
    {
        var left :Number = _channel.leftPeak * DANCE_AMPLITUDE;
        var right :Number = _channel.rightPeak * DANCE_AMPLITUDE;

        _media.y = left;

        var radians :Number = Math.atan2(right - left, _media.width);
        _media.rotation = radians * 180 / Math.PI;

//        trace("peaks: " + left + "  " + right);
//        trace("radians: " + radians + "  "+ _media.rotation);

        // maybe do some squishing?
        var avg :Number = (left + right) / (2 * DANCE_AMPLITUDE);
        if (avg > .75) {
            _media.scaleY = .75 + (1 - avg);
        } else {
            _media.scaleY = 1;
        }
    }

    protected var _media :DisplayObject;
    protected var _sound :Sound;
    protected var _channel :SoundChannel;

    [Embed(source="kawaii_pike.swf")]
    protected var AVATAR :Class;

    //[Embed(source="meg lee chin - heavy scene.mp3")]
    [Embed(source="5-fetts_vette.mp3")]
    protected var MUSIC :Class;

    //protected static const BPM :Number = 115.4;

    protected static const DANCE_AMPLITUDE :int = 50;
}
}
