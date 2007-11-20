//
// $Id$

package com.threerings.msoy.group.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains extra information about a group, such as the background images used on the GroupView
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

    /** Constrain area sizes to that of the images supplied */
    public static final int BACKGROUND_FIT_TO_IMAGE = 2;

    /** Flag to indicate how page flow is controlled */
    public int backgroundControl;

    /** The tiled background image for the info area. */
    public MediaDesc infoBackground;

    /** The tiled background image for the detail area. */
    public MediaDesc detailBackground;

    /** the width of the detail background image - used when not tiling */
    public int detailBackgroundWidth;

    /** The height of the detail area. */
    public int detailAreaHeight;
    
    /** The tiled background image for the people area. */
    public MediaDesc peopleBackground;

    /** The upper cap on the tiled peopleBackground */
    public MediaDesc peopleUpperCap;

    /** The height of the upper cap */
    public int peopleUpperCapHeight;

    /** The lower cap on the tiled peopleBackground */
    public MediaDesc peopleLowerCap;

    /** The height of the lower cap. */
    public int peopleLowerCapHeight;

    /** The group's charter, or null if one has yet to be set. */
    public String charter;
  
    /** The URL of the group's homepage. */
    public String homepageUrl;
}
