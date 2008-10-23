//
// $Id$

package com.threerings.msoy.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * A home page item is shown to the user within a flash popup in their home room. It is currently a
 * quick implementation to give Deviant Arts users links to all the things that they may want to do.
 * Each item specifies a specific activity in whirled. As far as possible, no further information
 * should be required to engage in the activity. For example, the game item goes straight to a game
 * lobby rather than a screenload of links to other games.
 */
public class HomePageItem extends SimpleStreamableObject
{
    /** Non action. */
    public static final byte ACTION_NONE = 0;

    /** Go and work on a specific badge. The data is an insance of {@link InProgressBadge}. */
    public static final byte ACTION_BADGE = 1;

    /** Play a game. The data is an <code>Integer</code> game id. */
    public static final byte ACTION_GAME = 2;

    /** View online friends. The data is ignored. */
    public static final byte ACTION_FRIENDS = 3;

    /** View news feed. The data is ignored. */
    public static final byte ACTION_FEED = 4;

    /** Go to a room. The data is an <code>Integer</code> scene id. */
    public static final byte ACTION_ROOM = 5;

    /** View a GWT page. The data is a string url. */
    public static final byte ACTION_GWT_PAGE = 6;

    /** Go to your inbox. The data is ignored. */
    public static final byte ACTION_INBOX = 7;

    /** Go to a group. The data is an <code>Integer</code> group id. */
    public static final byte ACTION_GROUP = 8;

    /** Go to a user profile. The data is an <code>Integer</code> profile.*/
    public static final byte ACTION_PROFILE = 9;

    /**
     * Creates a new home page item.
     * @param action the action for this item
     * @param actionData the data associated with the action
     * @param image the icon to show for this action
     */
    public HomePageItem (byte action, Object actionData, MediaDesc image)
    {
        _action = action;
        _actionData = actionData;
        _image = image;
    }

    /**
     * Gets the action for this item.
     */
    public byte getAction ()
    {
        return _action;
    }

    /**
     * Gets the name of this item. The name is sometimes given as null by the server because the
     * client is supposed to fill it in.
     */
    public String getName ()
    {
        return _name;
    }

    /**
     * Sets the name of this item.
     */
    public void setName (String name)
    {
        _name = name;
    }

    /**
     * Gets the data needed to engage in this action.
     */
    public Object getActionData ()
    {
        return _actionData;
    }

    /**
     * Gets the image or media to show to represent this item.
     */
    public MediaDesc getImage ()
    {
        return _image;
    }
    
    protected byte _action;
    protected String _name;
    protected Object _actionData;
    protected MediaDesc _image;    
}
