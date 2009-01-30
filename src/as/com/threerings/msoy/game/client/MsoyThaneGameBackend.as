//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.presents.util.PresentsContext;
import com.whirled.game.client.ThaneGameBackend;
import com.whirled.game.data.ThaneGameConfig;
import com.whirled.game.data.WhirledGameObject;

/** Msoy-specific thane game backend. */
public class MsoyThaneGameBackend extends ThaneGameBackend
{
    /** Creates a new thane game backend. */
    public function MsoyThaneGameBackend (
        ctx :PresentsContext,
        gameObj :WhirledGameObject,
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
