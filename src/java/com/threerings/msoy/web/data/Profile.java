//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.web.MediaDesc;

/**
 * Contains all member profile data for display on the profile page.
 */
public class Profile implements IsSerializable
{
    /** The member's unique id. */
    public int memberId;

    /** The member's display name. */
    public String displayName;

    /** The the member's selected profile picture. */
    public MediaDesc photo;

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
}
