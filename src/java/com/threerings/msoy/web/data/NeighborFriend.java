//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * Represents data for a single member in a neighborhood query result.
 */
public class NeighborFriend 
    implements IsSerializable, Streamable, Cloneable
{
    /** The member's id/name. */
    public MemberGName member;

    /** Whether or not this member is currently online. */
    public boolean isOnline;

    /** Constructor for unserializing. */
    public NeighborFriend ()
    {
        super();
    }

    /** Constructor from fields. */
    public NeighborFriend (MemberGName member, boolean isOnline)
    {
        super();
        this.member = member;
        this.isOnline = isOnline;
    }

}
