package com.threerings.msoy.client {

import com.threerings.util.Config;

import com.threerings.msoy.client.persist.SharedObjectSceneRepository;

public class Prefs
{
    /** The underlying config object used to store our prefs. A listener
     * may be installed on this object to hear about changes to preferences. */
    public static const config :Config = new Config("rsrc/config/msoy");

    public static const USERNAME :String = "username";
    public static const SESSION_TOKEN :String = "sessionTok";
    public static const MACHINE_IDENT :String = "machIdent";
    public static const VOLUME :String = "volume";
    public static const CHAT_DECAY :String = "chatDecay";
    public static const CHAT_FILTER :String = "chatFilter";
    public static const CHAT_HISTORY :String = "chatHistory";
    public static const LOG_TO_CHAT :String = "logToChat";

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

//    public static function getMediaPosition (id :String) :Number
//    {
//        return (config.getValue(mediaKey(id), 0) as Number);
//    }
//
//    public static function setMediaPosition (id :String, position :Number) :void
//    {
//        config.setValue(mediaKey(id), position);
//    }

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

    /**
     * Returns whether chat history is on or off.
     */
    public static function getShowingChatHistory () :Boolean
    {
        return (config.getValue(CHAT_HISTORY, true) as Boolean);
    }

    public static function setShowingChatHistory (showing :Boolean) :void
    {
        config.setValue(CHAT_HISTORY, showing);
    }

    public static function getLogToChat () :Boolean
    {
        return (config.getValue(LOG_TO_CHAT, false) as Boolean);
    }

    public static function setLogToChat (logToChat :Boolean) :void
    {
        config.setValue(LOG_TO_CHAT, logToChat);
    }

//    /**
//     * Internal function to create a media position key.
//     */
//    private static function mediaKey (id :String) :String
//    {
//        return "mediaPos_" + id;
//    }

    /**
     * A static initializer.
     */
    private static function staticInit () :void
    {
        var lastBuild :String = (config.getValue("lastBuild", null) as String);
        if (lastBuild == null) {
            // TEMP: added 2007-03-21, can be removed after a while.
            // We need to ensure that all cached scenes are no more.
            (new SharedObjectSceneRepository()).TEMPClearSceneCache();
        }

        // update our stored last build time
        if (lastBuild != DeploymentConfig.buildTime) {
            config.setValue("lastBuild", DeploymentConfig.buildTime);
        }
    }
    staticInit();
}
}
