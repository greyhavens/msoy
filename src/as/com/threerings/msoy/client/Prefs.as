package com.threerings.msoy.client {

import com.threerings.util.Config;

public class Prefs
{
    public static const config :Config = new Config("rsrc/config/msoy");

    public static function getUsername () :String
    {
        return (config.getValue("username", "") as String);
    }

    public static function setUsername (username :String) :void
    {
        config.setValue("username", username);
    }

    public static function getPassword () :String
    {
        return (config.getValue("password", "") as String);
    }

    public static function setPassword (password :String) :void
    {
        config.setValue("password", password);
    }

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
