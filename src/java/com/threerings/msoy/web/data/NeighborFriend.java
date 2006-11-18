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

    /** The shortest path betwen this member and the center member. */
    public byte distance;

    /** The id of the nearest parent node on the path to the center member. */
    public int parentId;
    
    /** Constructor for unserializing. */
    public NeighborFriend ()
    {
        super();
    }
}
