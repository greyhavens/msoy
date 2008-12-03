//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;
import com.threerings.presents.dobj.DSet_Entry;

/**
 * Represents a fellow party-goer connection.
 */
public class PartierEntry extends PlayerEntry
{
    // Nothing for now

    public function PartierEntry (
        name :VizMemberName = null, status :String = null)
    {
        super(name, status);
    }

    override public function toString () :String
    {
        return "PartierEntry[" + name + "]";
    }
}
}
