//
// $Id$

package com.threerings.msoy.game.client {

import flash.geom.Rectangle;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.whirled.game.client.WhirledGameController;

import com.threerings.msoy.client.OccupantReporter;

import com.threerings.msoy.game.data.MsoyGameConfig;

public class MsoyGameController extends WhirledGameController
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

    // from WhirledGameController
    override public function backToWhirled (showLobby :Boolean = false) :void
    {
        (_pctx as GameContext).backToWhirled(showLobby);
    }

    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new MsoyGamePanel((ctx as GameContext), this);
    }

    /** Reports occupant entry and exit. */
    protected var _occReporter :OccupantReporter = new OccupantReporter();
}
}
