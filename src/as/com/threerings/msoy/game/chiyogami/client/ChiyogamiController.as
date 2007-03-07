package com.threerings.msoy.game.chiyogami.client {

import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.parlor.game.client.GameController;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.game.client.WorldGameControllerDelegate;

import com.threerings.msoy.game.chiyogami.data.ChiyogamiObject;

public class ChiyogamiController extends GameController
{
    public function ChiyogamiController ()
    {
        super();
        addDelegate(_worldDelegate = new WorldGameControllerDelegate(this));
    }

    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);
        _mctx = (ctx as WorldContext);
    }

    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        _gameobj = (plobj as ChiyogamiObject);

        super.willEnterPlace(plobj);
    }

    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        _gameobj= null;
    }

//    override public function attributeChanged (event :AttributeChangedEvent) :void
//    {
//        var name :String = event.getName();
//        if (ChiyogamiObject.BOSS == name) {
//            _panel.setBoss(event.getValue() as MediaDesc);
//
//        } else {
//            super.attributeChanged(event);
//        }
//    }

    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _panel = new ChiyogamiPanel(ctx as WorldContext, this);
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

    /** Our world context. */
    protected var _mctx :WorldContext;

    /** Our game object. */
    protected var _gameobj :ChiyogamiObject;

    /** Our world delegate. */
    protected var _worldDelegate :WorldGameControllerDelegate;

    /** Our panel. */
    protected var _panel :ChiyogamiPanel;
}
}
