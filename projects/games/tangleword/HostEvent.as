package {

import flash.events.Event;

import com.threerings.ezgame.EZEvent;
import com.threerings.ezgame.EZGameControl;

/**
 * Dispatched by the host coordinator, to signal a change
 * in host status.
 */
public class HostEvent extends EZEvent
{
    /** Indicates that the current host changed. */
    public static const CHANGED :String = "Host Changed";

    /** Indicates that the host used to be unknown, but it became claimed
        by one of the clients. */
    public static const CLAIMED :String = "Host Claimed";

    public function HostEvent (type :String, ezgame :EZGameControl)
    {
        super (type, ezgame);
    }

    override public function toString () :String
    {
        return "[HostEvent type=" + type + "]";
    }

    override public function clone () :Event
    {
        return new HostEvent (type, _ezgame);
    }
}
}
