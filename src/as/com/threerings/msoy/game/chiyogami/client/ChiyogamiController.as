package com.threerings.msoy.game.chiyogami.client {

import com.threerings.io.TypedArray;
import com.threerings.util.Float;

import com.threerings.presents.dobj.AttributeChangedEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.parlor.game.client.GameController;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.item.web.MediaDesc;

import com.threerings.msoy.world.client.ActorSprite;
import com.threerings.msoy.world.client.RoomView;

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

        // this is a pile of crap- continue trying to set up the boss's
        // health bar until we succeed. It appears we are instantiated even prior
        // to the roomview becoming active. Wow!
        var fn :Function = function () :void {
            if (!updateBossHealth()) {
                _mctx.getClient().callLater(fn);
            }
        };
        fn();
    }

    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        _gameObj = null;
    }

    override public function attributeChanged (ace :AttributeChangedEvent) :void
    {
        var name :String = ace.getName();
        if (name == ChiyogamiObject.BOSS_OID || name == ChiyogamiObject.BOSS_HEALTH) {
            updateBossHealth()

        } else {
            super.attributeChanged(ace);
        }
    }

    /**
     * Routed from usercode- the score and style will be reported at
     * the discretion of the minigame.
     */
    public function miniGameReportedPerformance (score :Number, style :Number) :void
    {
        _gameObj.manager.invoke("reportPerf", Float.valueOf(score), Float.valueOf(style));
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
        _panel.gameDidStart();

        super.gameDidStart();
    }

    override protected function gameDidEnd () :void
    {
        super.gameDidEnd();

        _panel.gameDidEnd();
    }

    /**
     * Update the boss's health.
     * @return false if we were unable to do so because things seem
     *               to still be initializing.
     */
    protected function updateBossHealth () :Boolean
    {
        if (_gameObj.bossOid == 0) {
            _bossHealth = null;
            return true;

        } else if (_bossHealth == null) {
            // find the actor of the boss and add the health meter
            var roomView :RoomView = (_mctx.getTopPanel().getPlaceView() as RoomView);
            if (roomView == null) {
                return false;
            }
            var boss :ActorSprite = roomView.getActor(_gameObj.bossOid);
            if (boss == null) {
                return false;
            }

            _bossHealth = new HealthMeter();
            boss.addDecoration(_bossHealth);
        }

        _bossHealth.setHealth(_gameObj.bossHealth);
        return true;
    }

    /**
     * Check the status of the user being able to control their avatar.
     */
    protected function recheckAvatarControl () :void
    {
        var inPlay :Boolean = _gameObj.isInPlay();

        _worldDelegate.setAvatarControl(!inPlay);

        if (inPlay) {
            // TODO: filter dance-worthy actions/states here, or always on the server???

//            var myActions :Array = _worldDelegate.getMyActions();
//            var actions :TypedArray = TypedArray.create(String);
//            actions.addAll(myActions);

            var myStates :Array = _worldDelegate.getMyStates();
            var states :TypedArray = TypedArray.create(String);
            states.addAll(myStates);

            _gameObj.manager.invoke("setStates", states);
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

    /** The boss's health meter. */
    protected var _bossHealth :HealthMeter;
}
}
