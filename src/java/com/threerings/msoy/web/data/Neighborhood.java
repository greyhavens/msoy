//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Represents all the data returned for a neighborhood query. The data has two components:
 * one, an array of {@link NeighborGroup} objects corresponding to the groups of which the
 * member is a member; two, an array of arrays laying out first the friends of the member, 
 * then the friends of the friends, and so on.
 */
public class Neighborhood 
    implements IsSerializable, Streamable, Cloneable
{
    /** The member around whom this query is centered. */
    public MemberGName centerMember;

    /** An array of {@link NeighborGroup} objects for the center member's memberships. */
    public NeighborGroup[] neighborGroups;
    
    /** The friends of friends, etc, of the center member, arranged by friendship distance. */
    public NeighborFriend[][] neighborFriends;

    /** Constructor for unserializing. */
    public Neighborhood ()
    {
        super();
    }
}
