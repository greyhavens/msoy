package
{

import flash.events.Event;

import com.threerings.ezgame.EZEvent;
import com.threerings.ezgame.EZGameControl;

/**
 * Dispatched by the RoundProvider, these events happen after a round has started or ended,
 * and guarantee that the RoundProvider data for the given round will be up to date.
 */
public class RoundProviderEvent extends EZEvent
{
    /** Indicates that a new round had started. */
    public static const STARTED :String = "Round started";

    /** Indicates that a round had ended. */
    public static const ENDED :String = "Round ended";

    public function RoundProviderEvent (
        type :String, control :EZGameControl, lengthMs :int)
    {
        super (type, control);
        _lengthMs = lengthMs;
    }

    public function get lengthMs () :int
    {
        return _lengthMs;
    }

    override public function clone () :Event
    {
        return new RoundProviderEvent (type, _ezgame, _lengthMs);
    }

    /** Round or pause length, in milliseconds. */
    protected var _lengthMs :int;

}
}
