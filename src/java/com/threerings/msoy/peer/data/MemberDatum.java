//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.io.SimpleStreamableObject;
import com.threerings.presents.dobj.DSet;

/**
 * Tracks a piece of information for a particular member in the network.
 */
@com.threerings.util.ActionScript(omit=true)
public class MemberDatum extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The id of the member in question. */
    public Integer memberId;

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return memberId;
    }
}
