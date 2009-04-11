//
// $Id$

package com.threerings.msoy.party.data;

public class PartyDetail extends PartyBoardInfo
{
    /** The people in this party. */
    public PartyPeep[] peeps;

    /** Suitable for unserialization. */
    public PartyDetail () {}

    /**
     * Construct a party detail.
     */
    public PartyDetail (PartySummary summary, PartyInfo info, PartyPeep[] peeps)
    {
        super(summary, info);
        this.peeps = peeps;
    }
}
