//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.HeaderBar;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.TopPanel;

/**
 * Customizes the MsoyController for operation in the standalone game client.
 */
public class GameController extends MsoyController
{
    public function GameController (gctx :GameContext, topPanel :TopPanel)
    {
        super(gctx.getMsoyContext(), topPanel);
        _gctx = gctx;
    }

    // from MsoyController
    override public function handleClosePlaceView () : void
    {
        // TODO
    }

    // from MsoyController
    override public function handleMoveBack () :void
    {
        // TODO
    }

    // from MsoyController
    override public function goToPlace (params :Object) :void
    {
        if (null != params["gameLocation"]) {
            var gameOid :int = int(params["gameLocation"]);
            _gctx.getLocationDirector().moveTo(gameOid);
        } // else support showing lobby if desired
    }

    // from MsoyController
    override protected function updateTopPanel (headerBar :HeaderBar, controlBar :ControlBar) :void
    {
        // TODO
    }

    protected var _gctx :GameContext;
}
}
