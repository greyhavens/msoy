//
// $Id$

package com.threerings.msoy.group.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;

/**
 * Contains extra information about a group.  This should be used to hold information that is only
 * needed on the GroupView page itself, and not in other places that Groups are fetched.
 */
public class GroupExtras
    implements IsSerializable, CanonicalImageData
{
    /** The group's charter, or null if one has yet to be set. */
    public String charter;

    /** The URL of the group's homepage. */
    public String homepageUrl;

    /** The catalog category to link to. */
    public byte catalogItemType;

    /** The catalog tag to link to. */
    public String catalogTag;

    /** Game this group is attached to - used only in group creation */
    public Game game;

    /** Canonical image for this group **/
    public MediaDesc canonicalImage;
    
    /**
     * Create a group extras for a given game using default values
     */
    public static GroupExtras fromGame (Game game)
    {
        GroupExtras extras = new GroupExtras();
        extras.charter = "";
        extras.homepageUrl = "";
        extras.catalogItemType = Item.GAME;
        extras.catalogTag = game.name;
        extras.game = game;
        return extras;
    }
    
    /** Get the canonical image media descriptor for this group **/
    public MediaDesc getCanonicalImage () {
        return canonicalImage;
    }

    /** Set the canonical image media descriptor for this group **/
    public void setCanonicalImage (MediaDesc mediaDesc) {
        canonicalImage = mediaDesc;
    }
}
