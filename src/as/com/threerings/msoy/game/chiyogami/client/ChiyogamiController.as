//
// $Id$

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

import com.threerings.msoy.item.data.all.MediaDesc;

import com.threerings.msoy.world.client.AvatarSprite;
import com.threerings.msoy.world.client.RoomView;

import com.threerings.msoy.game.client.AVRGameControllerDelegate;

import com.threerings.msoy.game.chiyogami.data.ChiyogamiObject;

public class ChiyogamiController extends GameController
{
    /** A command to be submitted when tags are entered. */
    public static const TAGS_ENTERED :String = "TagsEntered";

    public function ChiyogamiController ()
    {
        super();
        addDelegate(_worldDelegate = new AVRGameControllerDelegate(this));
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

        recheckBoss();
    }

    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        _checkingBoss = false;
        _gameObj = null;
    }

    override public function attributeChanged (ace :AttributeChangedEvent) :void
    {
        var name :String = ace.getName();
        if (name == ChiyogamiObject.BOSS_OID || name == ChiyogamiObject.BOSS_HEALTH) {
            recheckBoss();

        } else if (name == ChiyogamiObject.PHASE) {
            phaseChanged();

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

    /**
     * Handle the TAGS_ENTERED command.
     */
    public function handleTagsEntered (tags :String) :void
    {
        _gameObj.manager.invoke("submitTags", tags);
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

    protected function phaseChanged () :void
    {
        _panel.checkPhase();
        recheckAvatarControl();
    }

    /**
     * Called to initiate a few things that need to be done with the boss.
     */
    protected function recheckBoss () :void
    {
        if (_checkingBoss) {
            return; // already doing it
        }

        _checkingBoss = true;
        // this is a pile of crap- continue trying to set up the boss's
        // health bar until we succeed. It appears we are instantiated even prior
        // to the roomview becoming active. Wow!
        var fn :Function = function () :void {
            if (_checkingBoss && !doBossCheck()) {
                _mctx.getClient().callLater(fn);

            } else {
                _checkingBoss = false;
            }
        };
        fn();
    }

    /**
     * Find the boss avatar and do what we can to initialize it.
     * Don't call this directly, call recheckBoss().
     *
     * @return false if we were unable to do so because things seem
     *               to still be initializing.
     */
    protected function doBossCheck () :Boolean
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
            var boss :AvatarSprite = (roomView.getActor(_gameObj.bossOid) as AvatarSprite);
            if (boss == null) {
                return false;
            }
            if (!boss.isContentInitialized()) {
                return false;
            }

            // tell the manager about the boss' states
            _gameObj.manager.invoke("setBossStates", massageStates(boss.getAvatarStates()));

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
        var hasControl :Boolean = (_gameObj.phase != ChiyogamiObject.BATTLE);

        _worldDelegate.setAvatarControl(hasControl);

        if (!hasControl) {
            _gameObj.manager.invoke("setStates", massageStates(_worldDelegate.getMyStates()));
        }
    }

    /**
     * Turn an Array into a TypedArray of String, and replace the first state with null.
     */
    protected function massageStates (states :Array) :TypedArray
    {
        var wrapped :TypedArray = TypedArray.create(String);
        wrapped.addAll(states);
        // if not being shown to a user, the default state can be replaced with null
        if (wrapped.length > 0) {
            wrapped[0] = null;
        }
        return wrapped;
    }

    /** Our world context. */
    protected var _mctx :WorldContext;

    /** Our game object. */
    protected var _gameObj :ChiyogamiObject;

    /** Our world delegate. */
    protected var _worldDelegate :AVRGameControllerDelegate;

    /** Our panel. */
    protected var _panel :ChiyogamiPanel;

    /** Are we currently trying to initialize various things with the boss? */
    protected var _checkingBoss :Boolean;

    /** The boss's health meter. */
    protected var _bossHealth :HealthMeter;
}
}
