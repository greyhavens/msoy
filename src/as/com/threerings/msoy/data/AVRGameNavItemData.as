//
// $Id$

package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;

/**
 * Data for an AVRG home page item, which requires the group id.
 */
public class AVRGameNavItemData extends BasicNavItemData
{
    // for serialization
    public function AVRGameNavItemData ()
    {
    }

    public function getGroupId () :int
    {
        return _groupId; 
    }

    // from BasicNavItemData
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);
        _groupId = ins.readInt();
    }

    protected var _groupId :int;
}
}
