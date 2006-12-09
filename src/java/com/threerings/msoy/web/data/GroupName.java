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

    // from Comparable
    public int compareTo (Object o) 
    {
        return groupId - ((GroupName) o).groupId;
    }
}
