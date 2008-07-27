//
// $Id$

package com.threerings.msoy.group.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.fora.gwt.ForumThread;

import com.threerings.msoy.group.data.all.Group;
import com.threerings.msoy.group.data.all.GroupMembership;

/**
 * Contains the details of a group.
 */
public class GroupDetail
    implements Streamable, IsSerializable
{
    /** The group whose details we contain. */
    public Group group;

    /** The extra details that are needed on the GroupView page. */
    public GroupExtras extras;

    /** The person who created the group. */
    public MemberName creator;

    /** The number of members in this group. */
    public int memberCount;

    /** My rank in this group ({@link GroupMembership#RANK_NON_MEMBER} if we're not a member). */
    public byte myRank;

    /** When my rank was assigned (in millis since the epoch), or 0 if we're a non-member. */
    public long myRankAssigned;

    /** Recent discussion threads for this group */
    public List<ForumThread> threads;

    /** The number of people online in this group's scenes (as of the last snapshot). */
    public int population;

    /** The top {@link NUM_TOP_MEMBERS} members of this group, ordered by rank */
    public List<GroupMemberCard> topMembers;

    public static int NUM_TOP_MEMBERS = 10;
}
