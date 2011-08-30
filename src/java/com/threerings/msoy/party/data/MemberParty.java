//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.presents.dobj.DSet;

/**
 * Contains basic information on the current party of a member on a peer.
 */
@com.threerings.util.ActionScript(omit=true)
public class MemberParty extends SimpleStreamableObject
    implements DSet.Entry
{
    /** The id of the member. */
    public Integer memberId;

    /** Their party id. */
    public int partyId;

    /** Suitable for deserialization. */
    public MemberParty ()
    {
    }

    /**
     * Constructor.
     */
    public MemberParty (Integer memberId, int partyId)
    {
        this.memberId = memberId;
        this.partyId = partyId;
    }

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return memberId;
    }
}
