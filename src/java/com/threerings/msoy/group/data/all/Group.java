//
// $Id$

package com.threerings.msoy.group.data.all;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.StaticMediaDesc;
import com.threerings.msoy.fora.gwt.ForumThread;
import com.threerings.msoy.group.gwt.CanonicalImageData;
import com.threerings.msoy.item.data.all.Game;

/**
 * Contains the basic data of a group.
 */
public class Group
    implements Streamable, IsSerializable, Comparable<Group>
{
    /** A policy constant for groups that allow all comers. */
    public static final byte POLICY_PUBLIC = 1;

    /** A policy constant for groups that only allow membership by invitation. */
    public static final byte POLICY_INVITE_ONLY = 2;

    /** A policy constant for groups that are not visible to non-members. */
    public static final byte POLICY_EXCLUSIVE = 3;

    /** Used for the {@link #forumPerms} setting. */
    public static final int PERM_ALL = 1;

    /** Used for the {@link #forumPerms} setting. */
    public static final int PERM_MEMBER = 2;

    /** Used for the {@link #forumPerms} setting. */
    public static final int PERM_MANAGER = 3;

    /** Indicates read access for a group's forums. */
    public static final int ACCESS_READ = 1;

    /** Indicates thread-related access for a group's forums. */
    public static final int ACCESS_THREAD = 2;

    /** Indicates post-related access for a group's forums. */
    public static final int ACCESS_POST = 3;

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

    /** This group's political policy (e.g. {@link #POLICY_PUBLIC}). */
    public byte policy;

    /** This group's forum permissions (see {@link #makePerms}). */
    public byte forumPerms;

    /** A snapshot of the number of members in this group. */
    public int memberCount;

    /**
     * Creates a default logo for use with groups that have no logo.
     */
    public static MediaDesc getDefaultGroupLogoMedia ()
    {
        return new StaticMediaDesc(MediaDesc.IMAGE_PNG, "photo", "group_logo",
                                   // we know that we're 66x60
                                   MediaDesc.HALF_VERTICALLY_CONSTRAINED);
    }

    /**
     * Returns true if anyone can join a group of the specified policy without an invitation.
     */
    public static boolean canJoin (byte policy)
    {
        return (policy == POLICY_PUBLIC);
    }

    /**
     * Returns true if a person of the specified rank can invite someone to join a group with the
     * specified policy.
     */
    public static boolean canInvite (byte policy, byte rank)
    {
        switch (rank) {
        case GroupMembership.RANK_MANAGER: return true;
        case GroupMembership.RANK_MEMBER: return (policy == POLICY_PUBLIC);
        default: return false;
        }
    }

    /**
     * Composes thread and post permissions into a {@link #forumPerms} value.
     */
    public static byte makePerms (int threadPerm, int postPerm)
    {
        return (byte)((threadPerm << 4) | postPerm);
    }

    /**
     * Returns this group's logo, or the default.
     */
    public MediaDesc getLogo ()
    {
        return (logo == null) ? getDefaultGroupLogoMedia() : logo;
    }

    /**
     * Returns this group's name as a {@link GroupName} record.
     */
    public GroupName getName ()
    {
        return new GroupName(name, groupId);
    }

    /**
     * Gets this group's thread permissions. See {@link #makePerms}.
     */
    public int getThreadPerm ()
    {
        return (forumPerms >> 4) & 0xF;
    }

    /**
     * Gets this group's post permissions. See {@link #makePerms}.
     */
    public int getPostPerm ()
    {
        return forumPerms & 0xF;
    }

    /**
     * Returns true if a member of the specified rank (or non-member) has the specified access.
     *
     * @param flags only used when checking {@link #ACCESS_THREAD}, indicates the flags of the
     * thread to be created/edited.
     */
    public boolean checkAccess (byte rank, int access, int flags)
    {
        // managers can always dowhattheylike
        if (rank == GroupMembership.RANK_MANAGER) {
            return true;
        }

        int havePerm = (rank == GroupMembership.RANK_MEMBER) ? PERM_MEMBER : PERM_ALL;
        switch (access) {
        case ACCESS_READ:
            // members can always read, non-members can read messages in non-exclusive groups
            return (rank != GroupMembership.RANK_NON_MEMBER) ? true : (policy != POLICY_EXCLUSIVE);

        case ACCESS_THREAD:
            // the thread must be non-sticky/non-announce and they must have permissions
            return (flags == 0) && (getThreadPerm() <= havePerm);

        case ACCESS_POST:
            // non-managers cannot post to locked threads
            if ((flags & ForumThread.FLAG_LOCKED) != 0) {
                return false;
            }
            return getPostPerm() <= havePerm;

        default:
            throw new IllegalArgumentException(
                "Requested invalid access check [rank=" + rank + ", access=" + access + "].");
        }
    }

    // from Comparable
    public int compareTo (Group other)
    {
        // this is used to sort groups on the GroupList page, so sort by group name first, then
        // by groupId if necessary.
        int nameComparison = name.compareTo(other.name);
        if (nameComparison == 0) {
            return groupId == other.groupId ? 0 : (groupId < other.groupId ? -1 : 1);
        } else {
            return nameComparison;
        }
    }

    /**
     * Create a new group for a given game using default values
     */
    public static Group fromGame (Game game)
    {
        Group group = new Group();

        // Deal with name issues as best we can
        String name = game.name + " Whirled";
        if (!Character.isLetter(name.charAt(0)) || Character.isDigit(name.charAt(0))) {
            name = "The " + name;
        }

        // truncate to the last space in the name
        if (name.length() > GroupName.LENGTH_MAX) {
            for (int ii = GroupName.LENGTH_MAX; ii >= 0; ii--) {
                char c = name.charAt(ii);
                if (c == ' ') {
                    name = name.substring(0, ii+1);
                    break;
                }
            }
            if (name.length() > GroupName.LENGTH_MAX) {
                name = name.substring(0, GroupName.LENGTH_MAX - 1);
            }
        }
        group.name = name;

        group.blurb = "A place to discuss the game " + game.name;
        // may be the default game icon
        group.logo = game.getThumbnailMedia();
        group.policy = Group.POLICY_PUBLIC;
        group.forumPerms = Group.makePerms(Group.PERM_ALL, Group.PERM_ALL);

        return group;
    }
}
