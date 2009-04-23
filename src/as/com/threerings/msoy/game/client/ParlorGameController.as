//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.ValueEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.whirled.game.client.WhirledGameController;
import com.whirled.game.client.BaseGameBackend;

import com.threerings.msoy.client.BootablePlaceController;
import com.threerings.msoy.client.OccupantReporter;
import com.threerings.msoy.client.TopPanel;

import com.threerings.msoy.game.data.ParlorGameConfig;
import com.threerings.msoy.game.data.ParlorGameObject;

public class ParlorGameController extends WhirledGameController
    implements BootablePlaceController
{
    // from PlaceController
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        // wire up our occupant reporter (don't report initial occupants)
        _occReporter.willEnterPlace((_pctx as GameContext).getWorldContext(), plobj);

        // dispatch an event with our current location and (lack of) owner info
        var top :TopPanel = (_pctx as GameContext).getWorldContext().getTopPanel();
        top.dispatchEvent(new ValueEvent(TopPanel.LOCATION_NAME_CHANGED,
                                         (_gconfig as ParlorGameConfig).game.name));
        top.dispatchEvent(new ValueEvent(TopPanel.LOCATION_OWNER_CHANGED, null));
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
        //return (_pctx as GameContext).getWorldContext().getTokens().isSupport();
    }

    // from BaseGameController
    override protected function createBackend () :BaseGameBackend
    {
        return new ParlorGameBackend(_ctx as GameContext, _gameObj as ParlorGameObject, this);
    }

    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new ParlorGamePanel((ctx as GameContext), this);
    }

    /** Reports occupant entry and exit. */
    protected var _occReporter :OccupantReporter = new OccupantReporter();
}
}
