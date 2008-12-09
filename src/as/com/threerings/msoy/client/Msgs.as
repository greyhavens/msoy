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

    /** The world message bundle. */
    public static function get WORLD () :MessageBundle
    {
        return _world;
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

    /** The notify message bundle. */
    public static function get NOTIFY () :MessageBundle
    {
        return _notify;
    }

    /** The prefs message bundle. */
    public static function get PREFS () :MessageBundle
    {
        return _prefs;
    }

    /** The studio message bundle. */
    public static function get STUDIO () :MessageBundle
    {
        return _studio;
    }

    /** The passport message bundle. */
    public static function get PASSPORT () :MessageBundle
    {
        return _passport;
    }

    /** The party message bundle. */
    public static function get PARTY () :MessageBundle
    {
        return _party;
    }

    /** The home page grid message bundle. */
    public static function get HOME_PAGE_GRID () :MessageBundle
    {
        return _homePageGrid;
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
        _world = msgMgr.getBundle(MsoyCodes.WORLD_MSGS);
        _chat = msgMgr.getBundle(MsoyCodes.CHAT_MSGS);
        _editing = msgMgr.getBundle(MsoyCodes.EDITING_MSGS);
        _game = msgMgr.getBundle(MsoyCodes.GAME_MSGS);
        _item = msgMgr.getBundle(MsoyCodes.ITEM_MSGS);
        _notify = msgMgr.getBundle(MsoyCodes.NOTIFY_MSGS);
        _prefs = msgMgr.getBundle(MsoyCodes.PREFS_MSGS);
        _studio = msgMgr.getBundle(MsoyCodes.STUDIO_MSGS);
        _passport = msgMgr.getBundle(MsoyCodes.PASSPORT_MSGS);
        _party = msgMgr.getBundle(MsoyCodes.PARTY_MSGS);
        _homePageGrid = msgMgr.getBundle(MsoyCodes.HOME_PAGE_GRID_MSGS);
    }

    protected static var _general :MessageBundle;
    protected static var _world :MessageBundle;
    protected static var _chat :MessageBundle;
    protected static var _editing :MessageBundle;
    protected static var _game :MessageBundle;
    protected static var _item :MessageBundle;
    protected static var _notify :MessageBundle;
    protected static var _prefs :MessageBundle;
    protected static var _studio :MessageBundle;
    protected static var _passport :MessageBundle;
    protected static var _party :MessageBundle;
    protected static var _homePageGrid :MessageBundle;
}
}
