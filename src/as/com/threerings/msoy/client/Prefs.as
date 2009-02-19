//
// $Id$

package com.threerings.msoy.client {

import flash.media.SoundMixer;
import flash.media.SoundTransform;

import flash.utils.Dictionary;

import com.threerings.util.Config;
import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

/**
 * Dispatched when a piece of media is bleeped or unbleeped.
 * This is dispatched on the 'config' object.
 *
 * @eventType com.threerings.msoy.client.Prefs.BLEEPED_MEDIA;
 * arg: [ mediaId (String), bleeped (Boolean) ]
 * mediaId may be the GLOBAL_BLEEP constant.
 */
[Event(name="bleepedMedia", type="com.threerings.util.ValueEvent")]

public class Prefs
{
    /** The underlying config object used to store our prefs. A listener
     * may be installed on this object to hear about changes to preferences. */
    public static const config :Config = new Config("rsrc/config/msoy");

    public static const USERNAME :String = "username";
    public static const SESSION_TOKEN :String = "sessionTok";
    public static const MACHINE_IDENT :String = "machIdent";
    public static const VOLUME :String = "volume";
    public static const ZOOM :String = "zoom";
    public static const CHAT_FONT_SIZE :String = "chatFontSize";
    public static const CHAT_DECAY :String = "chatDecay";
    public static const CHAT_FILTER :String = "chatFilter";
    public static const CHAT_HISTORY :String = "chatHistory";
    public static const CHAT_SIDEBAR :String = "chatSliding"; // legacy name
    public static const OCCUPANT_LIST :String = "occupantList";
    public static const LOG_TO_CHAT :String = "logToChat";
    public static const BLEEPED_MEDIA :String = "bleepedMedia";
    public static const GRID_AUTOSHOW :String = "gridAutoshow";
    public static const PARTY_GROUP :String = "partyGroup";
    public static const PERMAGUEST_USERNAME :String = "permaguestUsername";

    public static const CHAT_FONT_SIZE_MIN :int = 10;
    public static const CHAT_FONT_SIZE_MAX :int = 24;

    public static const GLOBAL_BLEEP :String = "_bleep_";

    /**
     * Effect the global sound volume.
     */
    public static function useSoundVolume () :void
    {
        // set up the global sound transform
        SoundMixer.soundTransform = new SoundTransform(getSoundVolume());
    }

    public static function getUsername () :String
    {
        return (config.getValue(USERNAME, "") as String);
    }

    public static function setUsername (username :String) :void
    {
        config.setValue(USERNAME, username);
    }

    public static function getPermaguestUsername () :String
    {
        return (config.getValue(PERMAGUEST_USERNAME, null) as String);
    }

    public static function setPermaguestUsername (username :String) :void
    {
        if (username == null) {
            config.remove(PERMAGUEST_USERNAME);

        } else {
            config.setValue(PERMAGUEST_USERNAME, username);
        }
    }

    public static function getMachineIdent () :String
    {
        return (config.getValue(MACHINE_IDENT, "") as String);
    }

    public static function setMachineIdent (ident :String) :void
    {
        config.setValue(MACHINE_IDENT, ident);
    }

    public static function getGridAutoshow () :Boolean
    {
        return Boolean(config.getValue(GRID_AUTOSHOW, true));
    }

    public static function setGridAutoshow (show :Boolean) :void
    {
        config.setValue(GRID_AUTOSHOW, show);
    }

    public static function setMediaBlocked (id :String, blocked :Boolean) :void
    {
        checkLoadBlockedMedia();
        if (id == GLOBAL_BLEEP) {
            _globalBleep = blocked;

        } else if (blocked) {
            _bleepedMedia[id] = true;

        } else {
            delete _bleepedMedia[id];
        }

        // TODO: right now we don't persist this.
        //config.setValue(BLEEPED_MEDIA, _bleepedMedia, false);

        config.dispatchEvent(new ValueEvent(BLEEPED_MEDIA, [ id, blocked ]));
    }

    public static function isMediaBlocked (id :String) :Boolean
    {
        checkLoadBlockedMedia();
        return _globalBleep || (id in _bleepedMedia);
    }

    public static function getSoundVolume () :Number
    {
        return (config.getValue(VOLUME, 1) as Number);
    }

    public static function setSoundVolume (vol :Number) :void
    {
        config.setValue(VOLUME, vol);
        useSoundVolume();
    }

    public static function getZoom () :Number
    {
        return (config.getValue(ZOOM, 1) as Number);
    }

    public static function setZoom (zoom :Number) :void
    {
        config.setValue(ZOOM, zoom);
    }

    /**
     * Get the preferred chat font size.
     * Default value: 14.
     */
    public static function getChatFontSize () :int
    {
        return (config.getValue(CHAT_FONT_SIZE, 14) as int);
    }

    /**
     * Set the user's new preferred chat size.
     */
    public static function setChatFontSize (newSize :int) :void
    {
        config.setValue(CHAT_FONT_SIZE, newSize);
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
        return (config.getValue(CHAT_HISTORY, false) as Boolean);
    }

    public static function setShowingChatHistory (showing :Boolean) :void
    {
        config.setValue(CHAT_HISTORY, showing);
    }

    /**
     * Returns whether chat is in sidebar mode.
     */
    public static function getSidebarChat () :Boolean
    {
        return (config.getValue(CHAT_SIDEBAR, false) as Boolean);
    }

    public static function setSidebarChat (sidebar :Boolean) :void
    {
        config.setValue(CHAT_SIDEBAR, sidebar);
    }

    /**
     * Returns whether to display the channel occupant list or not.
     */
    public static function getShowingOccupantList () :Boolean
    {
        return (config.getValue(OCCUPANT_LIST, false) as Boolean);
    }

    public static function setShowingOccupantList (showing :Boolean) :void
    {
        config.setValue(OCCUPANT_LIST, showing);
    }

    public static function getPartyGroup () :int
    {
        return (config.getValue(PARTY_GROUP, 0) as int);
    }

    public static function setPartyGroup (groupId :int) :void
    {
        config.setValue(PARTY_GROUP, groupId);
    }

    protected static function checkLoadBlockedMedia () :void
    {
        if (_bleepedMedia == null) {
            _bleepedMedia = config.getValue(BLEEPED_MEDIA, null) as Dictionary;
            if (_bleepedMedia == null) {
                _bleepedMedia = new Dictionary();
            }
        }
    }

    /** A set of media ids that are bleeped (the keys of the dictionary). */
    protected static var _bleepedMedia :Dictionary;
    protected static var _globalBleep :Boolean;

    /**
    * A static initializer.
    */
    private static function staticInit () :void
    {
        var lastBuild :String = (config.getValue("lastBuild", null) as String);

        // update our stored last build time
        if (lastBuild != DeploymentConfig.buildTime) {
            config.setValue("lastBuild", DeploymentConfig.buildTime);
        }
    }

    staticInit();

    //private static const log :Log = Log.getLog(Prefs);
}
}
