//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * Contains information on the current location of a member on a peer.
 */
public class MemberLocation extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The id of the member represented by this location. */
    public Integer memberId;

    // from DSet.Entry
    public Comparable getKey ()
    {
        return memberId;
    }
}
