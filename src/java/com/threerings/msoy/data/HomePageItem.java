//
// $Id$

package com.threerings.msoy.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.NavItemData;

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

    /** Go and work on a specific badge. The data is an instance of {@link InProgressBadge}. */
    public static final byte ACTION_BADGE = 1;

    /** Play a game. The data is an instance of {@link BasicNavItemData} where the id is the game
     * id, and the name is the game's name. */
    public static final byte ACTION_GAME = 2;

    /** View online friends. The data is ignored. */
    public static final byte ACTION_FRIENDS = 3;

    /** View news feed. The data is ignored. */
    public static final byte ACTION_FEED = 4;

    /** Go to a room. The data is an instance of {@link BasicNavItemData} where the id is the scene
     * id, and the name is the room's name. */
    public static final byte ACTION_ROOM = 5;

    /** View a GWT page. The data is an instance of {@link BasicNavItemData}. */
    public static final byte ACTION_GWT_PAGE = 6;

    /** Go to your inbox. The data is ignored. */
    public static final byte ACTION_INBOX = 7;

    /** Go to a group's discussions. The data is an instance of {@link BasicNavItemData} where the
     * id is the group id, and the name is the group's name. */
    public static final byte ACTION_GROUP = 8;

    /** Go to a user profile. The data is an instance of {@link BasicNavItemData}.*/
    public static final byte ACTION_PROFILE = 9;

    /** Kick off the whirled tour. The data is ignored.*/
    public static final byte ACTION_EXPLORE = 10;

    /**
     * Creates a new home page item.
     * @param action the action for this item
     * @param navItemData the data associated with the action
     * @param image the icon to show for this action
     */
    public HomePageItem (byte action, NavItemData navItemData, MediaDesc image)
    {
        _action = action;
        _navItemData = navItemData;
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
     * Gets the item data associated with this home page item.
     */
    public NavItemData getNavItemData ()
    {
        return _navItemData;
    }

    /**
     * Gets the image or media to show to represent this item.
     */
    public MediaDesc getImage ()
    {
        return _image;
    }
    
    protected /* final */ byte _action;
    protected /* final */ NavItemData _navItemData;
    protected /* final */ MediaDesc _image;    
}
