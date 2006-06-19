package com.threerings.msoy.client {

import com.threerings.util.Config;

public class Prefs
{
    public static const config :Config = new Config("rsrc/config/msoy");

    public static function getMediaPosition (id :int) :Number
    {
        return (config.getValue("mediaPos_" + id, 0) as Number);
    }

    public static function setMediaPosition (id :int, position :Number) :void
    {
        config.setValue("mediaPos_" + id, position);
    }
}
}
