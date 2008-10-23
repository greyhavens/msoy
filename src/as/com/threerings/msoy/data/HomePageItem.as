//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.badge.data.all.InProgressBadge;

/**
 * A home page item is shown to the user within a flash popup in their home room. It is currently a
 * quick implementation to give Deviant Arts users links to all the things that they may want to do.
 * Each item specifies a specific activity in whirled. As far as possible, no further information
 * should be required to engage in the activity. For example, the game item goes straight to a game
 * lobby rather than a screenload of links to other games.
 */
public class HomePageItem
    implements Streamable
{
    /** Non action. */
    public static const ACTION_NONE :int = 0;

    /** Go and work on a specific badge. The data is an insance of {@link InProgressBadge}. */
    public static const ACTION_BADGE :int = 1;

    /** Play a game. The data is an <code>Integer</code> game id. */
    public static const ACTION_GAME :int = 2;

    /** View online friends. The data is ignored. */
    public static const ACTION_FRIENDS :int = 3;

    /** View news feed. The data is ignored. */
    public static const ACTION_FEED :int = 4;

    /** Go to a room. The data is an <code>Integer</code> scene id. */
    public static const ACTION_ROOM :int = 5;

    /** View a GWT page. The data is a string url. */
    public static const ACTION_GWT_PAGE :int = 6;

    /** Go to your inbox. The data is ignored. */
    public static const ACTION_INBOX :int = 7;

    /** Go to a group. The data is an <code>Integer</code> group id. */
    public static const ACTION_GROUP :int = 8;

    /** Go to a user profile. The data is an <code>Integer</code> profile.*/
    public static const ACTION_PROFILE :int = 9;

    /**
     * Gets the action for this item.
     */
    public function getAction () :int
    {
        return _action;
    }

    /**
     * Gets the name of this item. The name is sometimes given as null by the server because the
     * client is supposed to fill it in.
     */
    public function getName () :String
    {
        return _name;
    }

    /**
     * Gets the data needed to engage in this action.
     */
    public function getActionData () :Object
    {
        return _actionData;
    }

    /**
     * Gets the image or media to show to represent this item.
     */
    public function getImage () :MediaDesc
    {
        return _image;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        _action = ins.readByte();
        _name = String(ins.readField(String));
        _actionData = ins.readObject();
        _image = MediaDesc(ins.readObject());
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error();
    }

    public function toString () :String
    {
        return "HomePageItem (action=" + _action + ", name=" + _name + ", data=" + _actionData +
            ", image=" + _image + ")";
    }

    protected var _action :int;
    protected var _name :String;
    protected var _actionData :Object;
    protected var _image :MediaDesc;
}
}
