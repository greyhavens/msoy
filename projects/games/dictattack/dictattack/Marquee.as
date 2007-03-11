//
// $Id$

package dictattack {

import flash.events.TimerEvent;
import flash.utils.Timer;

import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

/**
 * Displays a centered text message for a specified period of time. Messages are queued for display
 * if other messages are still displaying.
 */
public class Marquee extends TextField
{
    public function Marquee (textFormat :TextFormat, cx :int, cy :int)
    {
        autoSize = TextFieldAutoSize.CENTER;
        selectable = false;
        defaultTextFormat = textFormat;
        _cx = cx;
        y = cy;
    }

    public function display (message :String, clearMillis :int) :void
    {
        if (text != "") {
            _tqueue.push(message);
            _dqueue.push(clearMillis);
            return;
        }

        text = message;
        x = _cx - getLineMetrics(0).width/2;

        // we don't use Util here because we want this class to be easily resuable
        var timer :Timer = new Timer(clearMillis, 1);
        timer.addEventListener(TimerEvent.TIMER, function () :void {
            clear();
        });
        timer.start();
    }

    public function clear () :void
    {
        text = "";
        if (_tqueue.length > 0) {
            display(_tqueue.pop() as String, _dqueue.pop() as int);
        }
    }

    protected var _tqueue :Array = new Array();
    protected var _dqueue :Array = new Array();
    protected var _cx :int;
}

}
