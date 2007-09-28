//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.util.Controller;

import com.threerings.msoy.client.WorldContext;

public class AVRGameController extends Controller
{
//     public function AVRGameController ()
//     {
//         super();
//         addDelegate(_worldDelegate = new AVRGameControllerDelegate(this));
//    }

//    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
//    {
//        super.init(ctx, config);
//        _mctx = (ctx as WorldContext);
//    }

//     override protected function createPlaceView (ctx :CrowdContext) :PlaceView
//     {
//         _panel = new AVRGamePanel(ctx, this);
//         return _panel;
//     }

//     override protected function setPlaceView () :void
//     {
//         _worldDelegate.setPlaceView(_panel);
//     }

//     override protected function clearPlaceView () :void
//     {
//         _worldDelegate.clearPlaceView();
//     }

    protected var _mctx :WorldContext;
    protected var _worldDelegate :AVRGameControllerDelegate;
}
}

// import com.threerings.crowd.util.CrowdContext;
// import com.threerings.ezgame.client.EZGamePanel;
// import com.threerings.ezgame.client.GameControlBackend;
// import com.threerings.msoy.client.WorldContext;
// import com.threerings.msoy.game.client.AVRGameControlBackend;
// import com.threerings.msoy.game.client.AVRGameController;
// import com.threerings.msoy.game.data.AVRGameObject;

// class AVRGamePanel extends EZGamePanel // we need no chat
// {
//     public function AVRGamePanel (ctx :CrowdContext, ctrl :AVRGameController)
//     {
//         super(ctx, ctrl);
//     }

//     override protected function createBackend () :GameControlBackend
//     {
//         return new AVRGameControlBackend(
//             _ctx as WorldContext, _ezObj as AVRGameObject, _ctrl as AVRGameController);
//     }
// }
