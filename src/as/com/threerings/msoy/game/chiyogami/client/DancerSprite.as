//
// $Id$

package com.threerings.msoy.game.chiyogami.client {

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.world.client.AvatarSprite;
import com.threerings.msoy.world.client.EntityBackend;

public class DancerSprite extends AvatarSprite
{
    public function DancerSprite ()
    {
        super(null);
    }

    public function setDancer (desc :MediaDesc) :void
    {
        setup(desc, null); // TODO: include the ItemIdent..
    }

    public function toggleFacing () :void
    {
        (_backend as DancerBackend).toggleFacing();
    }

    public function setWalking (walking :Boolean) :void
    {
        (_backend as DancerBackend).setWalking(walking);
    }

    override protected function createBackend () :EntityBackend
    {
        return new DancerBackend();
    }
}
}
