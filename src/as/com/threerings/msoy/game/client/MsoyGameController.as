//
// $Id$

package com.threerings.msoy.game.client {

import flash.utils.getTimer; // function import

import com.threerings.presents.client.InvocationAdapter;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.ezgame.client.EZGameController;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.data.MsoyGameObject;

public class MsoyGameController extends EZGameController
{
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        if (_gobj.isInPlay()) {
            _startStamp = getTimer();
        }
    }

    /**
     * This method is used by the WhirledGameControlBackend.
     */
    public function getAvailableFlow_v1 () :int
    {
        if (_startStamp == 0) {
            return 0;
        }
        var elapsed :int = (getTimer() - _startStamp) / 1000;
        var humanity :Number = (_pctx as WorldContext).getClientObject().getHumanity();
        var flowPerMinute :int = (_gobj as MsoyGameObject).flowPerMinute;
        return int((humanity * flowPerMinute * elapsed) / 60) - _flowAwarded;
    }

    /**
     * This method is used by the WhirledGameControlBackend.
     */
    public function awardFlow_v1 (amount :int) :void
    {
        _flowAwarded += amount;
        (_gobj as MsoyGameObject).msoyGameService.awardFlow(_pctx.getClient() ,amount,
            new InvocationAdapter(function (cause :String) :void {
                Log.getLog(MsoyGameController).warning("Unable to award flow: " + cause);
            }));
    }

    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new MsoyGamePanel((ctx as WorldContext), this);
    }

    override protected function gameDidStart () :void
    {
        super.gameDidStart();

        _startStamp = getTimer();
    }

    override protected function gameDidEnd () :void
    {
        super.gameDidEnd();

        _startStamp = 0;
    }

    /** The timestamp at which we started playing. */
    protected var _startStamp :Number;

    /** The amount of flow awarded thus far. */
    protected var _flowAwarded :int;
}
}
