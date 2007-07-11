//
// $Id$

package com.threerings.msoy.client {

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.msoy.data.MsoyCodes;

/**
 * Handy class for easily getting message bundles on the client.
 */
public class Msgs
{
    /** The general message bundle. */
    public static function get GENERAL () :MessageBundle
    {
        return _general;
    }

    /** The chat message bundle. */
    public static function get CHAT () :MessageBundle
    {
        return _chat;
    }

    /** The editing message bundle. */
    public static function get EDITING () :MessageBundle
    {
        return _editing;
    }

    /** The game message bundle. */
    public static function get GAME () :MessageBundle
    {
        return _game;
    }

    /** The item message bundle. */
    public static function get ITEM () :MessageBundle
    {
        return _item;
    }

    /** The prefs message bundle. */
    public static function get PREFS () :MessageBundle
    {
        return _prefs;
    }

    /**
     * Initialize the bundles.
     */
    public static function init (msgMgr :MessageManager) :void
    {
        if (_general) {
            return;
        }

        _general = msgMgr.getBundle(MsoyCodes.GENERAL_MSGS);
        _chat = msgMgr.getBundle(MsoyCodes.CHAT_MSGS);
        _editing = msgMgr.getBundle(MsoyCodes.EDITING_MSGS);
        _game = msgMgr.getBundle(MsoyCodes.GAME_MSGS);
        _item = msgMgr.getBundle(MsoyCodes.ITEM_MSGS);
        _prefs = msgMgr.getBundle(MsoyCodes.PREFS_MSGS);
    }

    protected static var _general :MessageBundle;
    protected static var _chat :MessageBundle;
    protected static var _editing :MessageBundle;
    protected static var _game :MessageBundle;
    protected static var _item :MessageBundle;
    protected static var _prefs :MessageBundle;
}
}
