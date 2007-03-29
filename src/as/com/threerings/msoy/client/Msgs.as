//
// $Id$

package com.threerings.msoy.client {

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

/**
 * TODO: The problem with this is that if we want to use the message
 * bundle name as an arg to displayFeedback() or somesuch...
 */
public class Msgs
{
    /** The general message bundle. */
    public static function get GENERAL () :MessageBundle
    {
        return _general;
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

        _general = msgMgr.getBundle("general");
        _editing = msgMgr.getBundle("editing");
        _game = msgMgr.getBundle("game");
        _item = msgMgr.getBundle("item");
        _prefs = msgMgr.getBundle("prefs");
    }

    protected static var _general :MessageBundle;
    protected static var _editing :MessageBundle;
    protected static var _game :MessageBundle;
    protected static var _item :MessageBundle;
    protected static var _prefs :MessageBundle;
}
}
