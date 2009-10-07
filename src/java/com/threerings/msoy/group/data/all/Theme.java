//
// $Id: $

package com.threerings.msoy.group.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.msoy.data.all.MediaDesc;

/**
 *  Contains the definition of a Theme.
 */
public class Theme extends SimpleStreamableObject
    implements IsSerializable
{
    /** The group id of this theme. */
    public int groupId;

    /** The media of the theme's Whirled logo replacement image. */
    public MediaDesc logo;

    /**
     * An empty constructor for deserialization
     */
    public Theme ()
    {
    }

    /**
     * An initialization constructor.
     */
    public Theme (int groupId, MediaDesc logo)
    {
        this.groupId = groupId;
        this.logo = logo;
    }

    @Override
    public int hashCode ()
    {
        return groupId;
    }

    @Override
    public boolean equals (Object o)
    {
        if (!(o instanceof Theme)) {
            return false;
        }
        Theme other = (Theme)o;
        if (groupId != other.groupId) {
            return false;
        }
        return (logo != null) ? logo.equals(other.logo) : (other.logo != null);
    }
}
