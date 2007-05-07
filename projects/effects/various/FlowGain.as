package {

import flash.display.Sprite;

import flash.events.Event;

import flash.filters.GlowFilter;

import flash.geom.Point;

import flash.media.Sound;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import flash.utils.getTimer; // function import

import com.whirled.EffectControl;

/**
 * A simple effect that displays a flow gain.
 */
[SWF(width="100", height="200")]
public class FlowGain extends Sprite
{
    public function FlowGain ()
    {
        _ctrl = new EffectControl(this);

        addEventListener(Event.ADDED_TO_STAGE, handleAdded);
        addEventListener(Event.REMOVED_FROM_STAGE, handleRemoved);

        var message :String = _ctrl.getParameters();
        if (message == null) {
            trace("No parameters from EffectControl, showing generic message.");
            message = "+Flow!";
        }

        var format :TextFormat = new TextFormat();
        format.size = 32;
        format.font = "Arial";
        format.italic = true;
        format.color = 0x000000;

        var tf :TextField = new TextField();
        tf.defaultTextFormat = format;
        tf.autoSize = TextFieldAutoSize.LEFT;
        tf.text = message;
        tf.width = tf.textWidth + 5;
        tf.height = tf.textHeight + 4;

        tf.filters = [ new GlowFilter(0xFFFFFF, 1, 2, 2, 255) ];

        addChild(tf);
        // start out centered
        _hotSpot = new Point(tf.width / 2, tf.height / 2);

        _ctrl.setHotSpot(_hotSpot.x, _hotSpot.y);
    }

    protected function handleAdded (... ignored) :void
    {
        _stamp = getTimer();
        addEventListener(Event.ENTER_FRAME, handleFrame);
    }

    protected function handleRemoved (... ignored) :void
    {
        removeEventListener(Event.ENTER_FRAME, handleFrame);
    }

    protected function handleFrame (... ignored) :void
    {
        var elapsed :Number = getTimer() - _stamp;

        _ctrl.setHotSpot(_hotSpot.x, _hotSpot.y + (Y_VEL * elapsed));

        if (elapsed > 1000) {
            if (elapsed > 2000) {
                _ctrl.effectFinished();

            } else {
                this.alpha = (2000 - elapsed) / 1000;
            }
        }
    }

    protected var _ctrl :EffectControl;

    protected var _stamp :Number;

    protected var _hotSpot :Point;

    protected static const Y_VEL :Number = .02;
}
}
