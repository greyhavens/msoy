//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.MsoyController;

public class WorldGameController extends MsoyGameController
{
    public function WorldGameController ()
    {
        super();
        addDelegate(_worldDelegate = new WorldGameControllerDelegate(this));
    }

    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);
        _mctx = (ctx as WorldContext);
    }
    
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _panel = new WorldGamePanel(ctx, this);
        return _panel;
    }
    
    override protected function setPlaceView () :void
    {
        _worldDelegate.setPlaceView(_mctx, _panel);
    }
    
    override protected function clearPlaceView () :void
    {
        _worldDelegate.clearPlaceView(_mctx);
    }
    
    protected var _mctx :WorldContext;
    protected var _worldDelegate :WorldGameControllerDelegate;
}
}

import com.threerings.crowd.util.CrowdContext;
import com.threerings.ezgame.client.EZGamePanel;
import com.threerings.ezgame.client.GameControlBackend;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.game.client.WorldGameControlBackend;
import com.threerings.msoy.game.client.WorldGameController;
import com.threerings.msoy.game.data.WorldGameObject;

class WorldGamePanel extends EZGamePanel // we need no chat
{
    public function WorldGamePanel (ctx :CrowdContext, ctrl :WorldGameController)
    {
        super(ctx, ctrl);
    }
    
    override protected function createBackend () :GameControlBackend
    {
        return new WorldGameControlBackend(_ctx as WorldContext, _ezObj as WorldGameObject, _ctrl as WorldGameController);
    }
}
