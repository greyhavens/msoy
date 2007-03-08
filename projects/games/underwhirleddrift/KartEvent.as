package {
import flash.events.Event;

public class KartEvent extends Event
{
    public static const CROSSED_FINISH_LINE :String = "crossedFinishLine";
    public static const BONUS :String = "bonus";
    public static const REMOVE_BONUS :String = "removeBonus";
    public static const SHIELD :String = "shield";

    public function KartEvent (type :String, value :Object = null) 
    {
        super(type);
        _value = value;
    }

    public function get value () :Object
    {
        return _value;
    }

    override public function clone () :Event
    {
        return new KartEvent(type, _value);
    }

    protected var _value :Object;
}
}
