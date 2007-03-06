package com.threerings.msoy.game.client {

import com.threerings.ezgame.client.GameControlBackend;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.data.MsoyGameObject;

/**
 * Extends the basic EZGame backend with flow and other whirled services.
 */
public class WhirledGameControlBackend extends GameControlBackend
{
    public function WhirledGameControlBackend (
        ctx :WorldContext, gameObj :MsoyGameObject, ctrl :MsoyGameController)
    {
        super(ctx, gameObj, ctrl);
    }

    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);

        var ctrl :MsoyGameController = (_ctrl as MsoyGameController);
        o["getAvailableFlow_v1"] = ctrl.getAvailableFlow_v1;
        o["awardFlow_v1"] = ctrl.awardFlow_v1;
        o["setChatEnabled_v1"] = ctrl.setChatEnabled_v1;
    }
}
}
