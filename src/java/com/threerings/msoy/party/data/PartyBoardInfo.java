//
//

package com.threerings.msoy.party.data;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.all.MediaDesc;

public class PartyBoardInfo extends SimpleStreamableObject
{
    public PartyInfo info;

    public MediaDesc icon;

    public PartyBoardInfo ()
    {
    }

    public PartyBoardInfo (PartyInfo info)
    {
        this.info = info;
    }
}
