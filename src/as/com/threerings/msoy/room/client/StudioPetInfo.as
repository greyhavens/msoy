//
// $Id$

package com.threerings.msoy.room.client {

import com.threerings.util.Name;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.room.data.PetInfo;

public class StudioPetInfo extends PetInfo
{
    public function StudioPetInfo (name :String = null, petUrl :String = null)
    {
        if (name != null) {
            username = new Name(name);
            _media = new StudioMediaDesc(petUrl);
            _ident = new ItemIdent(Item.OCCUPANT, RoomStudioView.PET_ID);
        }
    }
}
}
