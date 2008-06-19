//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.presents.util.PresentsContext;
import com.whirled.game.client.ThaneGameBackend;
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
}

}
