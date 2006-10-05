package {

import flash.external.ExternalInterface;

public class Log
{
    public static function log (msg :String) :void
    {
        ExternalInterface.call("console.debug", msg);
    }
}
}
