//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.msoy.world.data.EffectData;

public class EffectSprite extends FurniSprite
{
    public function EffectSprite (effect :EffectData)
    {
        super(effect);
    }

    override public function getRoomLayer () :int
    {
        return (_furni as EffectData).roomLayer;
    }

    override public function getMaxContentWidth () :int
    {
        return int.MAX_VALUE;
    }

    override public function getMaxContentHeight () :int
    {
        return int.MAX_VALUE;
    }
}
}
