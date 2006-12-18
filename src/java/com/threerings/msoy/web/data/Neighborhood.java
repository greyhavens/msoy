//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Represents all the data returned for a neighborhood query: an array of {@link NeighborGroup}
 * objects corresponding to the groups of which the member is a member, and an array of
 * {@link NeighborMember} objects corresponding to their friends.
 */
public class Neighborhood 
    implements IsSerializable, Streamable, Cloneable
{
    /** The member around whom this query is centered, or null. */
    public MemberName member;

    /** The group around which this query is centered, or null. */
    public GroupName group;

    /** An array of {@link NeighborGroup} objects for the member's memberships. */
    public NeighborGroup[] neighborGroups;
    
    /** The friends of the member. */
    public NeighborMember[] neighborMembers;

    /** Constructor for unserializing. */
    public Neighborhood ()
    {
        super();
    }
}
