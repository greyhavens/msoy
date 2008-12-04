//
// $Id$

package com.threerings.msoy.data.all;

/**
 * Represents a fellow party-goer connection.
 */
public class PartierEntry extends PlayerEntry
{
    /**
     * The order of the partier among all the players who have joined this party. The lower this
     * value, the better priority they have to be auto-assigned leadership.
     */
    public int joinOrder;

    /** Suitable for deserialization. */
    public PartierEntry ()
    {
    }

    /** Mr. Constructor. */
    public PartierEntry (VizMemberName name)
    {
    	super(name);
    }

    @Override
    public String toString ()
    {
        return "PartierEntry[" + name + "]";
    }
}
