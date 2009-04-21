//
// $Id$

package com.threerings.msoy.game.client {

import com.whirled.game.client.ThaneGameBackend;
import com.whirled.game.client.ThaneGameController;

import com.threerings.msoy.game.data.ParlorGameObject;

/** Msoy-specific game controller. */
public class MsoyThaneGameController extends ThaneGameController
{
    /** Creates a new game controller. */
    public function MsoyThaneGameController ()
    {
    }

    /** @inheritDoc */
    // from ThaneGameController
    override protected function createBackend () :ThaneGameBackend
    {
        // create the msoy-specific subclass
        return new MsoyThaneGameBackend(_ctx, _gameObj as ParlorGameObject, this);
    }
}

}
