//
// $Id$

package com.threerings.msoy.room.client {

public class FurniBackend extends EntityBackend
{
    override protected function populateControlProperties (o :Object) :void
    {
        super.populateControlProperties(o);

        o["setHasAction_v1"] = setHasAction_v1;
    }

    protected function setHasAction_v1 (action :Boolean) :void
    {
        FurniSprite(_sprite).setHasUsercodeAction(action);
    }
}
}
