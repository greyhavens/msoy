//
// $Id$

package com.threerings.msoy.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.msoy.data.all.MemberName;

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

    /**
     * The members of this group, expressed as a Set of GroupMembership(s).
     *
     * @gwt.typeArgs <com.threerings.msoy.data.all.GroupMembership>
     */
    public List members;
}
