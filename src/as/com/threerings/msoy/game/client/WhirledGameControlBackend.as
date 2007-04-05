package com.threerings.msoy.game.client {

import flash.display.Sprite;
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

    protected function getHeadShot_v1 (occupant :int) :Sprite
    {
        validateConnected();
        var info :GameMemberInfo = _ezObj.occupantInfo.get(occupant) as GameMemberInfo;
        if (info != null) {
            var sprite :Sprite = _sprites[occupant];
            if (sprite == null) {
                _sprites[occupant] = sprite = new MediaContainer(info.headShot.getMediaPath());
            }
            return sprite;
        }
        return null;
    }

    /** A cache of loaded avatar headshots, indexed by occupant id. */
    protected var _sprites :Dictionary = new Dictionary();
}
}
