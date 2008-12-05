//
// $Id$

package com.threerings.msoy.party.data;

import com.threerings.msoy.data.all.PlayerEntry;
import com.threerings.msoy.data.all.VizMemberName;

/**
 * Represents a fellow party-goer connection.
 */
public class PartyPeep extends PlayerEntry
{
    /**
     * The order of the partier among all the players who have joined this party. The lower this
     * value, the better priority they have to be auto-assigned leadership.
     */
    public int joinOrder;

    /** Suitable for deserialization. */
    public PartyPeep ()
    {
    }

    /** Mr. Constructor. */
    public PartyPeep (VizMemberName name, int joinOrder)
    {
    	super(name);
        this.joinOrder = joinOrder;
    }

    @Override
    public String toString ()
    {
        return "PartyPeep[" + name + "]";
    }
}
