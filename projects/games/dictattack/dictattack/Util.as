//
// $Id$

package dictattack {

import flash.events.TimerEvent;
import flash.utils.Timer;

/**
 * Would you believe, utility functions?
 */
public class Util
{
    public static function invokeLater (delay :int, func :Function) :void
    {
        var timer :Timer = new Timer(delay, 1);
        timer.addEventListener(TimerEvent.TIMER, func);
        timer.start();
    }
}

}
