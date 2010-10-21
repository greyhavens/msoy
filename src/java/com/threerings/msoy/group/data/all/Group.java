//
// $Id$

package com.threerings.msoy.group.data.all;

import java.util.Date;

import com.google.common.collect.ComparisonChain;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.samskivert.util.ByteEnum;
import com.samskivert.util.ByteEnumUtil;
import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.data.all.StaticMediaDesc;
import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.group.data.all.GroupMembership.Rank;

/**
 * Contains the basic data of a group.
 */
public class Group
    implements Streamable, IsSerializable, Comparable<Group>
{
    /** Types of political policy a group can have. */
    public enum Policy implements ByteEnum
    {
        /** Allows all comers. */
        PUBLIC((byte)1),

        /** Allows membership by invitation only. */
        INVITE_ONLY((byte)2),

        /** The group is hidden from non-members. */
        EXCLUSIVE((byte)3);

        // from ByteEnum
        public byte toByte () {
            return _value;
        }

        Policy (byte value) {
            _value = value;
        }

        protected byte _value;
    }

    /** Types of permissions, used in various fields. */
    public enum Perm implements ByteEnum
    {
        ALL((byte)1),
        MEMBER((byte)2),
        MANAGER((byte)3);

        // from ByteEnum
        public byte toByte () {
            return _value;
        }

        Perm (byte value) {
            _value = value;
        }

        protected byte _value;
    }

    /** Actions that may be permitted or denied on a group. */
    public enum Access
    {
        /** Ability to read a group's forums. */
        READ,

        /** Thread-related access for a group's forums. */
        THREAD,

        /** Post-related access for a group's forums. */
        POST
    }

    /** The maximum allowed length for a group's blurb. */
    public static final int MAX_BLURB_LENGTH = 200;

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

    /** The id of this group's hall scene. */
    public int homeSceneId;

    /** The id of the person who created the group. */
    public int creatorId;

    /** The date on which this group was created. */
    public Date creationDate;

    /** This group's political policy. */
    public Policy policy;

    /** Required permission level for starting a thread. */
    public Perm threadPerm;

    /** Required permission level for replying to a thread. */
    public Perm postPerm;

    /** Required permission level for starting a party. Should never be {@link Perm#ALL}. */
    public Perm partyPerm;

    /** A snapshot of the number of members in this group. */
    public int memberCount;

    /** The id of the game associated with this whirled, or 0 if there is none */
    public int gameId;

    /** If the group is displayed more prominently, set only by admin. */
    public boolean official;

    /**
     * Return the specified MediaDesc, or the group default logo if it's null.
     */
    public static MediaDesc logo (MediaDesc desc)
    {
        return (desc != null) ? desc : getDefaultGroupLogoMedia();
    }

    /**
     * Creates a default logo for use with groups that have no logo.
     */
    public static MediaDesc getDefaultGroupLogoMedia ()
    {
        return new StaticMediaDesc(MediaMimeTypes.IMAGE_PNG, "photo", "group_logo",
                                   // we know that we're 66x60
                                   MediaDesc.HALF_VERTICALLY_CONSTRAINED);
    }

    /**
     * Returns true if anyone can join a group of the specified policy without an invitation.
     */
    public static boolean canJoin (Policy policy)
    {
        return (policy == Policy.PUBLIC);
    }

    /**
     * Returns true if a person of the specified rank can invite someone to join a group with the
     * specified policy.
     */
    public static boolean canInvite (Policy policy, Rank rank)
    {
        switch (rank) {
        case MANAGER: return true;
        case MEMBER: return (policy == Policy.PUBLIC);
        default: return false;
        }
    }

    /**
     * Composes thread and post permissions into a value for DB storage.
     */
    public byte getForumPerms ()
    {
        return (byte)((threadPerm.toByte() << 4) | postPerm.toByte());
    }

    /**
     * Assigns our {@link #threadPerm} and {@link #postPerm} from a composed DB value.
     */
    public void setForumPerms (byte forumPerms)
    {
        // we need to take the max because a bug during 2008 (april to december) that allowed
        // forum perms to be insterted as zero, which is out of range
        // TODO: migrate old permission values to max(1, old)
        threadPerm = ByteEnumUtil.fromByte(Perm.class, (byte)Math.max(1, (forumPerms >> 4) & 0xf));
        postPerm = ByteEnumUtil.fromByte(Perm.class, (byte)Math.max(1, forumPerms & 0xf));
    }

    /**
     * Returns this group's logo, or the default.
     */
    public MediaDesc getLogo ()
    {
        return logo(logo);
    }

    /**
     * Returns this group's name as a {@link GroupName} record.
     */
    public GroupName getName ()
    {
        return new GroupName(name, groupId);
    }

    /**
     * Returns true if a member of the specified rank (or non-member) has the specified access.
     *
     * @param flags only used when checking {@link Access#THREAD}, indicates the flags of the
     * thread to be created/edited.
     */
    public boolean checkAccess (Rank rank, Access access, int flags)
    {
        // managers can always dowhattheylike
        if (rank == Rank.MANAGER) {
            return true;
        }

        Perm havePerm = (rank == Rank.MEMBER) ? Perm.MEMBER : Perm.ALL;
        switch (access) {
        case READ:
            // members can always read, non-members can read messages in non-exclusive groups
            return (rank != Rank.NON_MEMBER) ? true : (policy != Policy.EXCLUSIVE);

        case THREAD:
            // the thread must be non-sticky/non-announce and they must have permissions
            return (flags == 0) && (threadPerm.ordinal() <= havePerm.ordinal());

        case POST:
            // non-managers cannot post to locked threads
            if ((flags & ForumThread.FLAG_LOCKED) != 0) {
                return false;
            }
            return postPerm.ordinal() <= havePerm.ordinal();

        default:
            throw new IllegalArgumentException(
                "Requested invalid access check [rank=" + rank + ", access=" + access + "].");
        }
    }

    // from Comparable
    public int compareTo (Group other)
    {
        return ComparisonChain.start().compare(name, other.name)
            .compare(groupId, other.groupId).result();
    }
}
