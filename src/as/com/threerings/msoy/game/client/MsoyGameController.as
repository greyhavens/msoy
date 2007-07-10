//
// $Id$

package com.threerings.msoy.game.client {

import flash.geom.Rectangle;
import flash.utils.getTimer; // function import

import com.threerings.presents.client.InvocationAdapter;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.ezgame.client.EZGameController;

import com.threerings.msoy.client.OccupantReporter;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.data.MsoyGameObject;

public class MsoyGameController extends EZGameController
{
    // from PlaceController
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        if (_gobj.isInPlay()) {
            _startStamp = getTimer();
        }

        // wire up our occupant reporter
        _occReporter.willEnterPlace(_ctx, plobj);
    }

    // from PlaceController
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        // shut down our occupant reporter
        _occReporter.didLeavePlace(plobj);
    }

    /**
     * Record a user performance for the specified identification.
     */
    public function awardFlow_v2 (perf :Number, id :String) :int
    {
        // TODO!

        // currently emulated using the old API
        var avail :int = getAvailableFlow_v1();
        if (perf >= 0 && perf <= 1) {
            avail *= perf;
        }
        awardFlow_v1(avail);
        return avail;
    }

    /**
     * This method is used by the WhirledGameControlBackend.
     */
    // TODO: remove. See changes in WhirledGameControlBackend
    public function getAvailableFlow_v1 () :int
    {
        _panel.backend.validateConnected();
        if (_startStamp == 0) {
            return 0;
        }
        var elapsed :int = (getTimer() - _startStamp) / 1000;
        var humanity :Number = (_pctx as WorldContext).getMemberObject().getHumanity();
        var flowPerMinute :int = (_gobj as MsoyGameObject).flowPerMinute;
        return int((humanity * flowPerMinute * elapsed) / 60) - _flowAwarded;
    }

    /**
     * This method is used by the WhirledGameControlBackend.
     */
    // TODO: remove. See changes in WhirledGameControlBackend
    public function awardFlow_v1 (amount :int) :void
    {
        _panel.backend.validateConnected();
        _flowAwarded += amount;
        (_gobj as MsoyGameObject).whirledGameService.awardFlow(_pctx.getClient() ,amount,
            new InvocationAdapter(function (cause :String) :void {
                Log.getLog(MsoyGameController).warning("Unable to award flow: " + cause);
            }));
    }

    /**
     * This method is used by the WhirledGameControlBackend.
     */
    public function setChatEnabled_v1 (enabled :Boolean) :void
    {
        _panel.backend.validateConnected();
        (_view as MsoyGamePanel).setChatEnabled(enabled);
    }

    /**
     * This method is used by the WhirledGameControlBackend.
     */
    public function setChatBounds_v1 (bounds :Rectangle) :void
    {
        _panel.backend.validateConnected();
        (_view as MsoyGamePanel).setChatBounds(bounds);
    }

    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new MsoyGamePanel((ctx as WorldContext), this);
    }

    override protected function gameDidStart () :void
    {
        super.gameDidStart();

        _flowAwarded = 0;
        _startStamp = getTimer();
    }

    override protected function gameDidEnd () :void
    {
        super.gameDidEnd();

        _startStamp = 0;
    }

    /** Reports occupant entry and exit. */
    protected var _occReporter :OccupantReporter = new OccupantReporter();

    /** The timestamp at which we started playing. */
    protected var _startStamp :Number;

    /** The amount of flow awarded thus far. */
    protected var _flowAwarded :int;
}
}
