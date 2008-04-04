//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.Loader;
import flash.geom.Point;
import flash.net.URLRequest;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.utils.Dictionary;

import com.whirled.game.client.GameBackend;
import com.whirled.game.data.WhirledGameObject;

import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.PlayerInfo;

/**
 * Implements the various Msoy specific parts of the Whirled Game backend.
 */
public class MsoyGameBackend extends GameBackend
{
    public function MsoyGameBackend (
        ctx :GameContext, gameObj :WhirledGameObject, ctrl :MsoyGameController)
    {
        super(ctx, gameObj, ctrl);
    }

    override protected function getHeadShot_v1 (occupant :int, callback :Function) :void
    {
        validateConnected();
        var info :PlayerInfo = _gameObj.occupantInfo.get(occupant) as PlayerInfo;
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

    /** A cache of loaded avatar headshots, indexed by occupant id. */
    protected var _headshots :Dictionary = new Dictionary();
}
}

import flash.events.Event;
import flash.events.ErrorEvent;

import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.DisplayObject;

import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import com.threerings.flash.ImageUtil;

import com.threerings.msoy.ui.MsoyMediaContainer;

// TODO: it's a little sketchy to return this to the usercode, as they could
// call toggleBlocked or other things. Thought required here.
class Headshot extends MsoyMediaContainer
{
    public static const STATE_LOADING :int = 0;
    public static const STATE_COMPLETE :int = 1;
    public static const STATE_ERROR :int = 2;

    public var callbacks :Array;
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

    override protected function handleComplete (event :Event) :void
    {
        super.handleComplete(event);

        state = STATE_COMPLETE;

        for (var ii :int = 0; ii < callbacks.length; ii ++) {
            respondTo(callbacks[ii]);
        }
        callbacks = null;
    }

    override protected function handleError (event :ErrorEvent) :void
    {
        super.handleError(event);

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
