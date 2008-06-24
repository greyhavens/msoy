//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.util.Name;

import com.threerings.msoy.world.data.PetInfo;

public class StudioPetInfo extends PetInfo
{
    public function StudioPetInfo (petUrl :String = null)
    {
        if (petUrl != null) {
            username = new Name("Pet"); // TODO
            _media = new StudioMediaDesc(petUrl);
        }
    }

    public function setState (state :String) :void
    {
        _state = state;
    }
}
}
