package com.threerings.msoy.game.chiyogami.client {

import com.threerings.io.TypedArray;

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
        _gameObj = (plobj as ChiyogamiObject);
        recheckAvatarControl();

        super.willEnterPlace(plobj);
    }

    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        _gameObj = null;
    }

    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _panel = new ChiyogamiPanel(ctx as WorldContext, this);
        return _panel;
    }

    override protected function setPlaceView () :void
    {
        _worldDelegate.setPlaceView(_panel);
    }

    override protected function clearPlaceView () :void
    {
        _worldDelegate.clearPlaceView();
    }

    override protected function stateDidChange (state :int) :Boolean
    {
        recheckAvatarControl();
        return super.stateDidChange(state);
    }

    override protected function gameDidStart () :void
    {
        super.gameDidStart();
    }

    override protected function gameDidEnd () :void
    {
        super.gameDidEnd();

    }

    /**
     * Check the status of the user being able to control their avatar.
     */
    protected function recheckAvatarControl () :void
    {
        var inPlay :Boolean = _gameObj.isInPlay();

        _worldDelegate.setAvatarControl(!inPlay);

        if (inPlay) {
            // scrape our actions out and send them off to the server
            var myActions :Array = _worldDelegate.getMyActions();

            // TODO: filter dance actions here???
            // for now, we just send a message with our actions

            var actions :TypedArray = TypedArray.create(String);
            actions.addAll(myActions);

            _gameObj.manager.invoke("setActions", actions);
        }
    }

    /** Our world context. */
    protected var _mctx :WorldContext;

    /** Our game object. */
    protected var _gameObj :ChiyogamiObject;

    /** Our world delegate. */
    protected var _worldDelegate :WorldGameControllerDelegate;

    /** Our panel. */
    protected var _panel :ChiyogamiPanel;
}
}
