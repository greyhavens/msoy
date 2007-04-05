package com.threerings.msoy.game.client {

import flash.display.Loader;
import flash.net.URLRequest;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.utils.Dictionary;

import com.threerings.flash.MediaContainer;

import com.threerings.ezgame.client.GameControlBackend;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.data.MsoyGameObject;
import com.threerings.msoy.game.data.GameMemberInfo;

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
        o["getHeadShot_v1"] = getHeadShot_v1;
    }

    protected function getHeadShot_v1 (occupant :int) :Loader
    {
        validateConnected();
        var info :GameMemberInfo = _ezObj.occupantInfo.get(occupant) as GameMemberInfo;
        if (info != null) {
            var loader :Loader = _headshots[occupant];
            if (loader == null) {
                _headshots[occupant] = loader = new Loader();
                loader.load(new URLRequest(info.headShot.getMediaPath()),
                            new LoaderContext(false, new ApplicationDomain(null)));
            }
            return loader;
        }
        throw new Error("Failed to find occupant: " + occupant);
    }

    /** A cache of loaded avatar headshots, indexed by occupant id. */
    protected var _headshots :Dictionary = new Dictionary();
}
}
