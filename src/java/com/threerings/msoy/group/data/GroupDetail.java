//
// $Id$

package com.threerings.msoy.group.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.group.data.GroupMembership;

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

    /** My rank in this group ({@link GroupMembership#RANK_NON_MEMBER} if we're not a member). */
    public byte myRank;

    /**
     * The managers of this group.
     *
     * @gwt.typeArgs <com.threerings.msoy.group.data.GroupMembership>
     */
    public List managers;
}
