//
// $Id$

package com.threerings.msoy.data.all;

/**
 * Represents a fellow party-goer connection.
 */
public class PartierEntry extends PlayerEntry
{
    // Nothing for now

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
