package com.threerings.msoy.client {

import com.threerings.util.Config;

public class Prefs
{
    public static const config :Config = new Config("rsrc/config/msoy");

    public static function getUsername () :String
    {
        return (config.getValue(USERNAME, "") as String);
    }

    public static function setUsername (username :String) :void
    {
        config.setValue(USERNAME, username);
    }

    public static function getSessionToken () :String
    {
        return (config.getValue(SESSION_TOKEN, null) as String);
    }

    public static function setSessionToken (token :String) :void
    {
        config.setValue(SESSION_TOKEN, token);
    }

    public static function getMachineIdent () :String
    {
        return (config.getValue(MACHINE_IDENT, "") as String);
    }

    public static function setMachineIdent (ident :String) :void
    {
        config.setValue(MACHINE_IDENT, ident);
    }

    public static function getMediaPosition (id :String) :Number
    {
        return (config.getValue(mediaKey(id), 0) as Number);
    }

    public static function setMediaPosition (id :String, position :Number) :void
    {
        config.setValue(mediaKey(id), position);
    }

    public static function getSoundVolume () :Number
    {
        return (config.getValue(VOLUME, 1) as Number);
    }

    public static function setSoundVolume (vol :Number) :void
    {
        config.setValue(VOLUME, vol);
    }

    /**
     * Get the value of the chat decay setting, which specifies how long
     * chat should remain before fading out.
     *
     * @return an integer: 0 = fast, 1 = medium (default), 2 = slow.
     */
    public static function getChatDecay () :int
    {
        return (config.getValue(CHAT_DECAY, 1) as int);
    }

    /**
     * Set the new chat decay value.
     */
    public static function setChatDecay (value :int) :void
    {
        config.setValue(CHAT_DECAY, value);
    }

    /**
     * Return the chat filtration level, as a constant from
     * com.threerings.crowd.chat.client.CurseFilter.
     */
    public static function getChatFilterLevel () :int
    {
        // 2 == CurseFilter.VERNACULAR, which is a bitch to import and
        // the subclass doesn't have it.
        return (config.getValue(CHAT_FILTER, 2) as int);
    }

    /**
     * Set the chat filtration level.
     */
    public static function setChatFilterLevel (lvl :int) :void
    {
        config.setValue(CHAT_FILTER, lvl);
    }

    public static function getLogToChat () :Boolean
    {
        return (config.getValue(LOG_TO_CHAT, false) as Boolean);
    }

    public static function setLogToChat (logToChat :Boolean) :void
    {
        config.setValue(LOG_TO_CHAT, logToChat);
    }

    /** Key constants. */
    private static const USERNAME :String = "username";
    private static const SESSION_TOKEN :String = "sessionTok";
    private static const MACHINE_IDENT :String = "machIdent";
    private static const VOLUME :String = "volume";
    private static const CHAT_DECAY :String = "chatDecay";
    private static const CHAT_FILTER :String = "chatFilter";
    private static const LOG_TO_CHAT :String = "logToChat";

    /**
     * Internal function to create a media position key.
     */
    private static function mediaKey (id :String) :String
    {
        return "mediaPos_" + id;
    }

    /**
     * A static initializer.
     */
    private static function staticInit () :void
    {
        // TEMP: remove obsolete keys
        config.remove("password");
    }
    staticInit();
}
}
