//
// $Id

package com.threerings.msoy.party.data;

public class PartyDetail extends PartyBoardInfo
{
    /** The name of the group. */
    public String groupName;

    /** The people in this party. */
    public PartyPeep[] peeps;

    /** Suitable for unserialization. */
    public PartyDetail () {}

    /**
     * Construct a party detail.
     */
    public PartyDetail (PartyInfo info, PartyPeep[] peeps)
    {
        super(info);
        this.peeps = peeps;
    }
}
