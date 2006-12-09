//
// $Id$

package com.threerings.msoy.web.data;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Contains the details of a group.
 */
public class GroupDetail
    implements Streamable, IsSerializable
{
    /** The group whose details we contain. */
    public Group group;

    /** The person who created the group. */
    public MemberName creator;

    /** The members of this group, expressed as a Set of GroupMembership(s) */
    public List members;
}
