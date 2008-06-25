//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.Name;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.world.data.PetInfo;

public class StudioPetInfo extends PetInfo
{
    public function StudioPetInfo (ctx :StudioContext = null, petUrl :String = null)
    {
        if (ctx != null) {
            username = new Name("Pet"); // TODO
            _media = new StudioMediaDesc(petUrl);
            _ident = new ItemIdent(Item.OCCUPANT, 0);
        }
    }

    public function setState (state :String) :void
    {
        _state = state;
    }
}
}
