//
// $Id$

package com.threerings.msoy.client {

import flash.events.EventDispatcher;

import flash.media.SoundMixer;
import flash.media.SoundTransform;

import com.threerings.util.Config;
import com.threerings.util.ConfigValueSetEvent;
import com.threerings.util.ValueEvent;

/**
 * Dispatched when a piece of media is bleeped or unbleeped.
 * This is dispatched on the 'events' object.
 *
 * @eventType com.threerings.msoy.client.Prefs.BLEEPED_MEDIA;
 * arg: [ mediaId (String), bleeped (Boolean) ]
 * mediaId may be the GLOBAL_BLEEP constant.
 */
[Event(name="bleepedMedia", type="com.threerings.util.ValueEvent")]

/**
 * Dispatched when a preference is changed.
 * This is dispatched on the 'events' object.
 *
 * @eventType com.threerings.util.ConfigValueSetEvent.CONFIG_VALUE_SET;
 */
[Event(name="ConfigValSet", type="com.threerings.util.ConfigValueSetEvent")]

public class Prefs
{
    /** We're a static class, so events are dispatched here. */
    public static const events :EventDispatcher = new EventDispatcher();

    public static const USERNAME :String = "username";
    public static const SESSION_TOKEN :String = "sessionTok";
    public static const MACHINE_IDENT :String = "machIdent";
    public static const VOLUME :String = "volume";
    public static const CHAT_FONT_SIZE :String = "chatFontSize";
    public static const CHAT_DECAY :String = "chatDecay";
    public static const CHAT_FILTER :String = "chatFilter";
    public static const CHAT_HISTORY :String = "chatHistory";
    public static const CHAT_SIDEBAR :String = "chatSliding"; // legacy name
    public static const OCCUPANT_LIST :String = "occupantList";
    public static const LOG_TO_CHAT :String = "logToChat";
    public static const BLEEPED_MEDIA :String = "bleepedMedia";
    public static const PARTY_GROUP :String = "partyGroup";
    public static const PERMAGUEST_USERNAME :String = "permaguestUsername";
    public static const USE_CUSTOM_BACKGROUND_COLOR :String = "useCustomBgColor";
    public static const CUSTOM_BACKGROUND_COLOR :String = "customBgColor";
    public static const AUTOSHOW_PREFIX :String = "autoShow_";
    public static const ROOM_ZOOM :String = "roomZoom";

    public static const APRIL_FOOLS :String = "aprilFools";

    public static const CHAT_FONT_SIZE_MIN :int = 10;
    public static const CHAT_FONT_SIZE_MAX :int = 24;

    public static const GLOBAL_BLEEP :String = "_bleep_";

    public static const IS_APRIL_FOOLS :Boolean =
        ((new Date().month) == 3) && ((new Date().date) == 1); // April 1st

    public static function setEmbedded (embedded :Boolean) :void
    {
        _config.setPath(embedded ? null : CONFIG_PATH);
        useSoundVolume();
    }

    /**
     * Set the build time. Return true if it's changed. Should only be done on non-embedded clients.
     */
    public static function setBuildTime (buildTime :String) :Boolean
    {
        var lastBuild :String = (_config.getValue("lastBuild", null) as String);
        if (lastBuild != buildTime) {
            _config.setValue("lastBuild", buildTime);

            // TEMP code: please to remove someday TODO
            var oldVal :Object = _config.getValue("gridAutoshow", null);
            if (oldVal != null) {
                _config.remove("gridAutoshow");
                setAutoshow("grid", Boolean(oldVal));
            }
            // END: TEMP TODO

            // TEMP code: please to remove someday TODO
            if (_config.getValue("zoom", null) != null) {
                _config.remove("zoom");
            }
            // END: TEMP TODO
            return true;
        }
        return false;
    }

    public static function isAprilFoolsEnabled () :Boolean
    {
        return IS_APRIL_FOOLS && _config.getValue(APRIL_FOOLS, true);
    }

    public static function setAprilFoolsEnabled (enabled :Boolean) :void
    {
        _config.setValue(APRIL_FOOLS, enabled);
    }

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
        return (_config.getValue(USERNAME, "") as String);
    }

    public static function setUsername (username :String) :void
    {
        _config.setValue(USERNAME, username);
    }

    public static function getPermaguestUsername () :String
    {
        return (_machineConfig.getValue(PERMAGUEST_USERNAME, null) as String);
    }

    public static function setPermaguestUsername (username :String) :void
    {
        if (username == null) {
            _machineConfig.remove(PERMAGUEST_USERNAME);

        } else {
            _machineConfig.setValue(PERMAGUEST_USERNAME, username);
        }
    }

    public static function getMachineIdent () :String
    {
        return (_config.getValue(MACHINE_IDENT, "") as String);
    }

    public static function setMachineIdent (ident :String) :void
    {
        _config.setValue(MACHINE_IDENT, ident);
    }

    public static function setMediaBleeped (id :String, bleeped :Boolean) :void
    {
        checkLoadBleepedMedia();
        if (bleeped) {
            _bleepedMedia[id] = true;

        } else {
            delete _bleepedMedia[id];
        }
        _config.setValue(BLEEPED_MEDIA, _bleepedMedia, false); // don't flush
        events.dispatchEvent(new ValueEvent(BLEEPED_MEDIA, [ id, bleeped ]));
    }

    /**
     * Return true if the specified media is bleeped, regardless of the global bleep setting.
     */
    public static function isMediaBleeped (id :String) :Boolean
    {
        checkLoadBleepedMedia();
        return (id in _bleepedMedia);
    }

    public static function setGlobalBleep (bleeped :Boolean) :void
    {
        _globalBleep = bleeped;
        events.dispatchEvent(new ValueEvent(BLEEPED_MEDIA, [ GLOBAL_BLEEP, bleeped ]));
    }

    public static function isGlobalBleep () :Boolean
    {
        return _globalBleep;
    }

    public static function getSoundVolume () :Number
    {
        return (_config.getValue(VOLUME, 1) as Number);
    }

    public static function setSoundVolume (vol :Number) :void
    {
        _config.setValue(VOLUME, vol);
        useSoundVolume();
    }

    /**
     * Returns the last set value for the room zoom or null if not set.
     */
    public static function getRoomZoom () :String
    {
        return _config.getValue(ROOM_ZOOM, null) as String;
    }

    /**
     * Sets the room zoom default.
     */
    public static function setRoomZoom (value :String) :void
    {
        _config.setValue(ROOM_ZOOM, value);
    }

    /**
     * Get the preferred chat font size.
     * Default value: 14.
     */
    public static function getChatFontSize () :int
    {
        return (_config.getValue(CHAT_FONT_SIZE, 14) as int);
    }

    /**
     * Set the user's new preferred chat size.
     */
    public static function setChatFontSize (newSize :int) :void
    {
        _config.setValue(CHAT_FONT_SIZE, newSize);
    }

    /**
     * Get the value of the chat decay setting, which specifies how long
     * chat should remain before fading out.
     *
     * @return an integer: 0 = fast, 1 = medium (default), 2 = slow.
     */
    public static function getChatDecay () :int
    {
        // in embedded mode (when configs don't persist, we default to fast chat clearing)
        return (_config.getValue(CHAT_DECAY, _config.isPersisting() ? 1 : 0) as int);
    }

    /**
     * Set the new chat decay value.
     */
    public static function setChatDecay (value :int) :void
    {
        _config.setValue(CHAT_DECAY, value);
    }

    /**
     * Return the chat filtration level, as a constant from
     * com.threerings.crowd.chat.client.CurseFilter.
     */
    public static function getChatFilterLevel () :int
    {
        // 2 == CurseFilter.VERNACULAR, which is a bitch to import and
        // the subclass doesn't have it.
        return (_config.getValue(CHAT_FILTER, 2) as int);
    }

    /**
     * Set the chat filtration level.
     */
    public static function setChatFilterLevel (lvl :int) :void
    {
        _config.setValue(CHAT_FILTER, lvl);
    }

    /**
     * Returns whether chat history is on or off.
     */
    public static function getShowingChatHistory () :Boolean
    {
        return (_config.getValue(CHAT_HISTORY, false) as Boolean);
    }

    public static function setShowingChatHistory (showing :Boolean) :void
    {
        _config.setValue(CHAT_HISTORY, showing);
    }

    /**
     * Returns whether chat is in sidebar mode.
     */
    public static function getSidebarChat () :Boolean
    {
        return (_config.getValue(CHAT_SIDEBAR, false) as Boolean);
    }

    public static function setSidebarChat (sidebar :Boolean) :void
    {
        _config.setValue(CHAT_SIDEBAR, sidebar);
    }

    /**
     * Returns whether to display the channel occupant list or not.
     */
    public static function getShowingOccupantList () :Boolean
    {
        return (_config.getValue(OCCUPANT_LIST, false) as Boolean);
    }

    public static function setShowingOccupantList (showing :Boolean) :void
    {
        _config.setValue(OCCUPANT_LIST, showing);
    }

    public static function getPartyGroup () :int
    {
        return (_config.getValue(PARTY_GROUP, 0) as int);
    }

    public static function setPartyGroup (groupId :int) :void
    {
        _config.setValue(PARTY_GROUP, groupId);
    }

    public static function getUseCustomBackgroundColor () :Boolean
    {
        return (_config.getValue(USE_CUSTOM_BACKGROUND_COLOR, false) as Boolean);
    }

    public static function setUseCustomBackgroundColor (value :Boolean) :void
    {
        _config.setValue(USE_CUSTOM_BACKGROUND_COLOR, value);
    }

    public static function getCustomBackgroundColor () :uint
    {
        return (_config.getValue(CUSTOM_BACKGROUND_COLOR, 0) as uint);
    }

    public static function setCustomBackgroundColor (value :uint) :void
    {
        _config.setValue(CUSTOM_BACKGROUND_COLOR, value);
    }

    /**
     * Returns whether the dialog of the given name should be shown automatically. The name is an
     * arbitrary string chosen by the caller to represent the dialog.
     */
    public static function getAutoshow (dialogName :String) :Boolean
    {
        return Boolean(_config.getValue(AUTOSHOW_PREFIX + dialogName, true));
    }

    /**
     * Sets whether the dialog of the given name should be shown automatically. The name is an
     * arbitrary string chosen by the caller to represent the dialog.
     */
    public static function setAutoshow (dialogName :String, show :Boolean) :void
    {
        _config.setValue(AUTOSHOW_PREFIX + dialogName, show);
    }

    protected static function checkLoadBleepedMedia () :void
    {
        if (_bleepedMedia == null) {
            _bleepedMedia = _config.getValue(BLEEPED_MEDIA, null) as Object;
            if (_bleepedMedia == null) {
                _bleepedMedia = new Object();
            }
        }
    }

    /** The path of our config object. */
    protected static const CONFIG_PATH :String = "rsrc/config/msoy";

    /** A set of media ids that are bleeped (the keys of the dictionary). */
    protected static var _bleepedMedia :Object;
    protected static var _globalBleep :Boolean;

    /** Our config object. */
    protected static var _config :Config = new Config(null);

    protected static var _machineConfig :Config = new Config(CONFIG_PATH);

    /**
    * A static initializer.
    */
    private static function staticInit () :void
    {
        // route events
        _config.addEventListener(ConfigValueSetEvent.CONFIG_VALUE_SET, events.dispatchEvent);
        _machineConfig.addEventListener(ConfigValueSetEvent.CONFIG_VALUE_SET, events.dispatchEvent);
    }

    staticInit();
}
}
