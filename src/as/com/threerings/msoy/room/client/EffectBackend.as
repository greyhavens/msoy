//
// $Id$

package com.threerings.msoy.room.client {

public class EffectBackend extends EntityBackend
{
    override protected function populateControlInitProperties (o :Object) :void
    {
        super.populateControlInitProperties(o);

        var sprite :EffectSprite = (_sprite as EffectSprite);
        o["parameters"] = sprite.getFurniData().actionData;
    }
}
}
