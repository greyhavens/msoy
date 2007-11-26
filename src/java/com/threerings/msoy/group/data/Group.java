//
// $Id$

package com.threerings.msoy.group.data;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.fora.data.ForumThread;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.StaticMediaDesc;

/**
 * Contains the basic data of a group.
 */
public class Group
    implements Streamable, IsSerializable, Comparable
{
    /** A policy constant for groups that allow all comers. */
    public static final byte POLICY_PUBLIC = 1;

    /** A policy constant for groups that only allow membership by invitation. */
    public static final byte POLICY_INVITE_ONLY = 2;

    /** A policy constant for groups that are not visible to non-members. */
    public static final byte POLICY_EXCLUSIVE = 3;

    /** Indicates read access for a group's forums. */
    public static final int ACCESS_READ = 1;

    /** Indicates thread-related access for a group's forums. */
    public static final int ACCESS_THREAD = 2;

    /** Indicates post-related access for a group's forums. */
    public static final int ACCESS_POST = 3;

    /** The maximum allowed length for a group's blurb. */
    public static final int MAX_BLURB_LENGTH = 80;

    /** The maximum allowed length for a group's charter. */
    public static final int MAX_CHARTER_LENGTH = 2000;

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

    /** The date on which this group was created. */
    public Date creationDate;

    /** This group's political policy (e.g. {@link #POLICY_PUBLIC}). */
    public byte policy;

    /** A snapshot of the number of members in this group. */
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

    /**
     * Returns this group's logo, or the default.
     */
    public MediaDesc getLogo ()
    {
        return (logo == null) ? getDefaultGroupLogoMedia() : logo;
    }

    /**
     * Returns true if a member of the specified rank (or non-member) has the specified access.
     *
     * @param flags only used when checking {@link #ACCESS_THREAD}, indicates the flags of the
     * thread to be created/edited.
     */
    public boolean checkAccess (byte rank, int access, int flags)
    {
        switch (access) {
        case ACCESS_READ:
            // members can always read, non-members can read messages in non-exclusive groups
            return (rank != GroupMembership.RANK_NON_MEMBER) ? true : (policy != POLICY_EXCLUSIVE);

        case ACCESS_THREAD:
            switch (rank) {
            default:
            case GroupMembership.RANK_NON_MEMBER:
                return false; // non-members can never create threads
            case GroupMembership.RANK_MEMBER:
                return (flags == 0); // members can only create non-stick/announce threads
            case GroupMembership.RANK_MANAGER:
                return true; // managers can dowhattheylike
            }

        case ACCESS_POST:
            // non-managers cannot post to locked threads
            if (rank != GroupMembership.RANK_MANAGER && (flags & ForumThread.FLAG_LOCKED) != 0) {
                return false;
            }
            // non-members can only post to public groups
            if (rank == GroupMembership.RANK_NON_MEMBER && policy != POLICY_PUBLIC) {
                return false;
            }
            // otherwise we're good to go
            return true;

        default:
            throw new IllegalArgumentException(
                "Requested invalid access check [rank=" + rank + ", access=" + access + "].");
        }
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
