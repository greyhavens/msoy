package {

import flash.external.ExternalInterface;

public class Logger
{
    public static function log (msg :String) :void
    {
        trace(msg);

        ExternalInterface.call("console.debug", msg);
    }
}
}
