//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;

import mx.containers.HBox;

import com.threerings.mx.controls.CommandButton;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.ezgame.client.EZGameController;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;

public class WorldGameController extends EZGameController
{
    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);
        _mctx = (ctx as MsoyContext);
    }
    
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _panel = new WorldGamePanel(ctx, this);
        return _panel;
    }
    
    override protected function setPlaceView () :void
    {
        _panel.percentWidth = 100;
        
        _vcont = new HBox();
        _vcont.addChild(_panel);
        
        var qbutton :CommandButton = new CommandButton(MsoyController.LEAVE_WORLD_GAME);
        qbutton.label = Msgs.GAME.get("b.leave_world_game");
        _vcont.addChild(qbutton);

        _mctx.getTopPanel().setBottomPanel(_vcont);
    }
    
    override protected function clearPlaceView () :void
    {
        _mctx.getTopPanel().clearBottomPanel(_vcont);
    }
    
    protected var _mctx :MsoyContext;
    protected var _vcont :HBox;
}
}

import com.threerings.crowd.util.CrowdContext;
import com.threerings.ezgame.client.EZGamePanel;
import com.threerings.ezgame.client.GameControlBackend;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.game.client.WorldGameControlBackend;
import com.threerings.msoy.game.client.WorldGameController;
import com.threerings.msoy.game.data.WorldGameObject;

class WorldGamePanel extends EZGamePanel
{
    public function WorldGamePanel (ctx :CrowdContext, ctrl :WorldGameController)
    {
        super(ctx, ctrl);
    }
    
    override protected function createBackend () :GameControlBackend
    {
        return new WorldGameControlBackend(_ctx as MsoyContext, _ezObj as WorldGameObject);
    }
}
