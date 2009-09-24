//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.presents.util.PresentsContext;

import com.whirled.game.client.ThaneGameBackend;

import com.threerings.msoy.party.client.PartyGameHelper;

import com.threerings.msoy.game.data.ParlorGameObject;
import com.threerings.msoy.game.data.PlayerObject;

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
        _partyHelper.init(gameObj, callUserCode);
    }

    override public function shutdown () :void
    {
        _partyHelper.shutdown();
        super.shutdown();
    }

    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);

        // Be sure to do the party stuff after super(), because ThaneGameBackend sets up
        // some test party stuff. There is presently no TestThaneGameBackend
        _partyHelper.populateProperties(o);
    }

    /** @inheritDoc */ // from BaseGameBackend
    override protected function reportGameError (message :String, error :Error = null) :void
    {
        // We don't want these messages in our logs, go straight to the user code
        _ctrl.outputToUserCode(message, error);
    }

    /** @inheritDoc */ // from BaseGameBackend
    override public_api function isRegistered_v1 (playerId :int = 0) :Boolean
    {
        var plobj :PlayerObject = (getPlayer(playerId) as PlayerObject);
        return (plobj == null) ? false : !plobj.isPermaguest();
    }

    protected var _partyHelper :PartyGameHelper = new PartyGameHelper();
}
}
