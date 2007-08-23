//
// $Id$

package com.threerings.msoy.game.client {

import flash.geom.Rectangle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.ezgame.client.EZGameController;

import com.threerings.msoy.client.OccupantReporter;

public class MsoyGameController extends EZGameController
{
    // from PlaceController
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

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
        return new MsoyGamePanel((ctx as GameContext), this);
    }

    /** Reports occupant entry and exit. */
    protected var _occReporter :OccupantReporter = new OccupantReporter();
}
}
