//
// $Id: $

package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.StaticMediaDesc;

/**
 *  Contains the definition of a Theme.
 */
public class Theme extends SimpleStreamableObject
    implements IsSerializable
{
    /** The group of this theme. */
    public GroupName group;

    /** The media of the theme's Whirled logo replacement image. */
    public MediaDesc logo;

    /** Whether or not we start playing this group's associated AVRG upon room entry. */
    public boolean playOnEnter;

    /** The background colour of the main Whirled UI. */
    public int backgroundColor;

    /**
     * Return the specified MediaDesc, or the theme default logo if it's null.
     */
    public static MediaDesc logo (MediaDesc desc)
    {
        return (desc != null) ? desc : getDefaultThemeLogoMedia();
    }

    /**
     * Creates a default logo for use with groups that have no logo.
     */
    public static MediaDesc getDefaultThemeLogoMedia ()
    {
        return new StaticMediaDesc(MediaDesc.IMAGE_PNG, "photo", "header_logo",
                                   // we know that we're 143 x 40
                                   MediaDesc.HORIZONTALLY_CONSTRAINED);
    }

    /**
     * An empty constructor for deserialization
     */
    public Theme ()
    {
    }

    public Theme (GroupName group)
    {
        this(group, false, null);
    }

    /**
     * An initialization constructor.
     */
    public Theme (GroupName group, boolean playOnEnter, MediaDesc logo)
    {
        this.group = group;
        this.playOnEnter = playOnEnter;
        this.logo = logo;
    }

    /**
     * Returns this group's logo, or the default.
     */
    public MediaDesc getLogo ()
    {
        return logo(logo);
    }

    public int getGroupId ()
    {
        return (group != null) ? group.getGroupId() : 0;
    }

    @Override
    public int hashCode ()
    {
        return getGroupId();
    }

    @Override
    public boolean equals (Object o)
    {
        if (!(o instanceof Theme)) {
            return false;
        }
        Theme other = (Theme)o;
        if (playOnEnter != other.playOnEnter) {
            return false;
        }
        return ((logo != null) ? logo.equals(other.logo) : (other.logo == null)) &&
            ((group != null) ? group.equals(other.group) : (other.group == null));
    }
}
