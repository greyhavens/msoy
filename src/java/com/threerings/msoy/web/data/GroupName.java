//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

/**
 * Contains a group name and group ID in one handy object.
 */
public class GroupName
    implements Streamable, IsSerializable, Comparable
{
    /** The group's name. */
    public String groupName;

    /** The group's id. */
    public int groupId;

    /**
     * Creates a group name that can be used as a key for a DSet lookup or whereever else one might
     * need to use a {@link GroupName} instance as a key but do not have the (unneeded) group name.
     */
    public static GroupName makeKey (int groupId)
    {
        return new GroupName(null, groupId);
    }

    /** Used when unserializing */
    public GroupName ()
    {
    }

    /**
     * Creates a group name with the specified information.
     */
    public GroupName (String groupName, int groupId)
    {
        this.groupName = groupName;
        this.groupId = groupId;
    }

    // @Override // from Object
    public int hashCode ()
    {
        return groupId;
    }

    // @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof GroupName) && groupId == ((GroupName)other).groupId;
    }

    // from Comparable
    public int compareTo (Object o) 
    {
        return groupId - ((GroupName) o).groupId;
    }
}
