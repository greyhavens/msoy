//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.Loader;
import flash.geom.Point;
import flash.net.URLRequest;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.utils.Dictionary;

import com.whirled.client.WhirledGameBackend;

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyGameObject;
import com.threerings.msoy.game.data.PlayerInfo;

/**
 * Implements the various Msoy specific parts of the Whirled Game backend.
 */
public class MsoyGameBackend extends WhirledGameBackend
{
    public function MsoyGameBackend (
        ctx :GameContext, gameObj :MsoyGameObject, ctrl :MsoyGameController)
    {
        super(ctx, gameObj, ctrl);
    }

    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);

        var ctrl :MsoyGameController = (_ctrl as MsoyGameController);
        o["getHeadShot_v1"] = getHeadShot_v1;

        // backwards compatibility
        o["getAvailableFlow_v1"] = getAvailableFlow_v1;
        o["awardFlow_v1"] = awardFlow_v1;
        o["awardFlow_v2"] = awardFlow_v2;
    }

    protected function getHeadShot_v1 (occupant :int, callback :Function) :void
    {
        validateConnected();
        var info :PlayerInfo = _ezObj.occupantInfo.get(occupant) as PlayerInfo;
        if (info != null) {
            var headshot :Headshot = _headshots[occupant];
            if (headshot == null) {
                _headshots[occupant] = headshot = new Headshot(info.getHeadshotURL());
            }
            headshot.newRequest(callback);
            return;
        }
        throw new Error("Failed to find occupant: " + occupant);
    }

    override protected function getSize_v1 () :Point
    {
        var p :Point = super.getSize_v1();
        p.x = Math.max(p.x, 700);
        p.y = Math.max(p.y, 500);
        return p;
    }

    // from WhirledGameBackend
    override protected function playerOwnsData (type :int, ident :String) :Boolean
    {
        var cfg :MsoyGameConfig = (_ctrl.getPlaceConfig() as MsoyGameConfig);
        return (_ctx as GameContext).getPlayerObject().ownsGameContent(cfg.getGameId(), type, ident)
    }

    // ------ Compatibility methods, for operating with old games

    /** A backwards compatible method. */
    protected function getAvailableFlow_v1 () :int
    {
        return 0;
    }

    /** A backwards compatible method. */
    protected function awardFlow_v1 (amount :int) :void
    {
        // NOOP!
    }

    /** A backwards compatible method. */
    protected function awardFlow_v2 (perf :int) :int
    {
        return 0;
    }

    /** A cache of loaded avatar headshots, indexed by occupant id. */
    protected var _headshots :Dictionary = new Dictionary();
}
}

import flash.events.Event;
import flash.events.IOErrorEvent;

import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.DisplayObject;

import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import com.threerings.flash.ImageUtil;
import com.threerings.flash.MediaContainer;

class Headshot extends MediaContainer
{
    public static const STATE_LOADING :int = 0;
    public static const STATE_COMPLETE :int = 1;
    public static const STATE_ERROR :int = 2;

    public var callbacks :Array = [ ];
    public var state :int;

    function Headshot (url :String)
    {
        callbacks = [ ];

        state = STATE_LOADING;
        setMedia(url);
    }

    public function newRequest (callback :Function) :void
    {
        if (state != STATE_LOADING) {
            respondTo(callback);
        } else {
            callbacks.push(callback);
        }
    }

    override protected function loadingComplete (event :Event) :void
    {
        super.loadingComplete(event);

        state = STATE_COMPLETE;

        for (var ii :int = 0; ii < callbacks.length; ii ++) {
            respondTo(callbacks[ii]);
        }
        callbacks = null;
    }

    override protected function loadError (event :IOErrorEvent) :void
    {
        super.loadError(event);

        state = STATE_ERROR;

        for (var ii :int = 0; ii < callbacks.length; ii ++) {
            respondTo(callbacks[ii]);
        }
        callbacks = null;
    }

    protected function respondTo (callback :Function) :void
    {
        callback(this, state == STATE_COMPLETE);
    }
}
