//
// $Id$

package com.threerings.msoy.data;

/**
 * Data for an AVRG home page item, which requires the group id.
 */
@com.threerings.util.ActionScript(omit=true)
public class AVRGameNavItemData extends BasicNavItemData
{
    // for serialization
    public AVRGameNavItemData ()
    {
        // For serialization
    }

    public AVRGameNavItemData (int id, String name, int groupId)
    {
        super(id, name);
        _groupId = groupId;
    }

    public int getGroupId ()
    {
        return _groupId;
    }

    protected int _groupId;
}
