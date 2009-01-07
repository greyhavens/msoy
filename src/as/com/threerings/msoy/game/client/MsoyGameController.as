//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.whirled.game.client.WhirledGameController;
import com.whirled.game.client.BaseGameBackend;

import com.threerings.msoy.client.BootablePlaceController;
import com.threerings.msoy.client.OccupantReporter;

public class MsoyGameController extends WhirledGameController
    implements BootablePlaceController
{
    // from PlaceController
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        // wire up our occupant reporter (don't report initial occupants)
        _occReporter.willEnterPlace(_ctx, plobj, false);

        // this is only used for testing game loading issues per WRLD-531,
        // and will be removed after the test is over. -- robert
        (_pctx as GameContext).getMsoyContext().getMsoyClient().trackClientAction(
            "WRLD-531-2 game started", "stage 7");
    }

    // from PlaceController
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        // shut down our occupant reporter
        _occReporter.didLeavePlace(plobj);
    }

    // from BootablePlaceController
    public function canBoot () :Boolean
    {
        return false;
        //return (_pctx as GameContext).getMsoyContext().getTokens().isSupport();
    }

    // from WhirledGameController
    override public function userCodeIsConnected (autoReady :Boolean) :void
    {
        super.userCodeIsConnected(autoReady);
        
        // this is only used for testing game loading issues per WRLD-531,
        // and will be removed after the test is over. -- robert
        (_pctx as GameContext).getMsoyContext().getMsoyClient().trackClientAction(
            "WRLD-531-2 game started", "stage 8");
    }
                                                                       
    // from BaseGameController
    override protected function createBackend () :BaseGameBackend
    {
        return new MsoyGameBackend(_ctx as GameContext, _gameObj, this);
    }

    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new MsoyGamePanel((ctx as GameContext), this);
    }

    /** Reports occupant entry and exit. */
    protected var _occReporter :OccupantReporter = new OccupantReporter();
}
}
