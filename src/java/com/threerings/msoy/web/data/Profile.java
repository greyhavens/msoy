//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

/**
 * Contains all member profile data for display on the profile page.
 */
public class Profile implements IsSerializable
{
    /** The default profile photo. */
    public static final MediaDesc DEFAULT_PHOTO =
        new StaticMediaDesc(MediaDesc.IMAGE_PNG, Item.PHOTO, "profile_photo",
                            // we know that we're 120x127
                            MediaDesc.HALF_VERTICALLY_CONSTRAINED);

    /** The minimum length for a display name. */
    public static final int MIN_DISPLAY_NAME_LENGTH = 3;

    /** The maximum length for a display name. */
    public static final int MAX_DISPLAY_NAME_LENGTH = 30;

    /** The the member's selected profile picture. */
    public MediaDesc photo = DEFAULT_PHOTO;

    /** A member provided profile headline. */
    public String headline;

    /** A member provided homepage URL. */
    public String homePageURL;

    /** The time at which the member last logged on. 0L means online now, -1L means they have never
     * logged on. */
    public long lastLogon;

    /** The member's professed gender. A/S/L's S. */
    public boolean isMale;

    /** The member's professed age. A/S/L's A. */
    public int age;

    /** The member's professed location.  A/S/L's L. */
    public String location;

    /** True if this member has a blog. */
    public boolean hasBlog;

    /** True if this member has a photo gallery. */
    public boolean hasGallery;

    /** The user's permaName */
    public String permaName;
}
