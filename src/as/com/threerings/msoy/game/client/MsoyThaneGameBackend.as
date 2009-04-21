//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.presents.util.PresentsContext;

import com.whirled.game.client.ThaneGameBackend;

import com.threerings.msoy.game.data.ParlorGameObject;

/** Msoy-specific thane game backend. */
public class MsoyThaneGameBackend extends ThaneGameBackend
{
    /** Creates a new thane game backend. */
    public function MsoyThaneGameBackend (
        ctx :PresentsContext,
        gameObj :ParlorGameObject,
        ctrl :MsoyThaneGameController)
    {
        super(ctx, gameObj, ctrl);
    }

    /** @inheritDoc */
    // from BaseGameBackend
    override protected function reportGameError (message :String, error :Error = null) :void
    {
        // We don't want these messages in our logs, go straight to the user code
        _ctrl.outputToUserCode(message, error);
    }
}

}
