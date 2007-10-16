//
// $Id$

package com.threerings.msoy.web.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

/**
 * Contains the basic data of a group.
 */
public class Group
    implements Streamable, IsSerializable, Comparable
{
    public static final byte POLICY_PUBLIC = 1;
    public static final byte POLICY_INVITE_ONLY = 2;
    public static final byte POLICY_EXCLUSIVE = 3;

    /** The unique id of this group. */
    public int groupId;

    /** The name of the group. */
    public String name;

    /** The blurb for the group. */
    public String blurb;

    /** The group's logo. */
    public MediaDesc logo;

    /** The id of the person who created the group. */
    public int creatorId;

    public Date creationDate;

    public byte policy;

    public int memberCount;

    /**
     * Creates a default logo for use with groups that have no logo.
     */
    public static MediaDesc getDefaultGroupLogoMedia ()
    {
        return new StaticMediaDesc(MediaDesc.IMAGE_PNG, Item.PHOTO, "group_logo",
                                   // we know that we're 66x60
                                   MediaDesc.HALF_VERTICALLY_CONSTRAINED);
    }

    // from Comparable
    public int compareTo (Object o) 
    {
        // The compareTo contract allows ClassCastException
        Group other = (Group)o;

        // this is used to sort groups on the GroupList page, so sort by group name first, then 
        // by groupId if necessary.
        int nameComparison = name.compareTo(other.name);
        if (nameComparison == 0) {
            return groupId == other.groupId ? 0 : (groupId < other.groupId ? -1 : 1);
        } else {
            return nameComparison;
        }
    }
}
