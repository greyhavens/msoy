//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.presents.util.PresentsContext;

import com.whirled.game.client.ThaneGameBackend;
import com.whirled.game.data.ThaneGameConfig;
import com.whirled.game.data.WhirledGameObject;
import com.whirled.game.data.WhirledPlayerObject;

import com.threerings.msoy.game.data.PlayerObject;

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

    // from ThaneGameBackend
    override protected function subscribedToPlayer (player :WhirledPlayerObject) :void
    {
        // this does the same thing as the superclass method, except that it looks up the
        // OccupantInfo by the VizMemberName stored in PlayerObject rather than the username field
        // in ClientObject.  This is necessary because in msoy code, the the
        // GameObject.getOccupantInfo() method is checking against VizMemberNames.
        var occInfo :OccupantInfo = _gameObj.getOccupantInfo((player as PlayerObject).memberName);
        if (isInited(occInfo)) {
            occupantAdded(occInfo);
            if (!_gameStarted && _gameObj.isInPlay()) {
                gameStateChanged(true);
            }
        }
    }
}

}
