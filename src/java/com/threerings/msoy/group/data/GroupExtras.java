//
// $Id$

package com.threerings.msoy.group.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains extra information about a group, such as the background image used on the GroupView
 * page.  This should be used to hold information that is only needed on the GroupView page itself,
 * and not in other places that Groups are fetched.
 */
public class GroupExtras
    implements Streamable, IsSerializable
{
    /** Tile the background images to the edge of the screen */
    public static final int BACKGROUND_TILED = 0;

    /** Do not tile the images, and do not constrain the size of the areas */
    public static final int BACKGROUND_ANCHORED = 1;

    /** The group's charter, or null if one has yet to be set. */
    public String charter;
  
    /** The URL of the group's homepage. */
    public String homepageUrl;

    /** Flag to indicate how page flow is controlled */
    public int backgroundControl;

    /** The background image for the group area. */
    public MediaDesc background;
}
