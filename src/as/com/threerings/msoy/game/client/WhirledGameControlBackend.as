package com.threerings.msoy.game.client {

import flash.display.Loader;
import flash.net.URLRequest;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.utils.Dictionary;

import com.threerings.ezgame.client.GameControlBackend;

import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.game.data.MsoyGameObject;
import com.threerings.msoy.game.data.GameMemberInfo;

/**
 * Extends the basic EZGame backend with flow and other whirled services.
 */
public class WhirledGameControlBackend extends GameControlBackend
{
    public function WhirledGameControlBackend (
        ctx :WorldContext, gameObj :MsoyGameObject, ctrl :MsoyGameController)
    {
        super(ctx, gameObj, ctrl);
    }

    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);

        var ctrl :MsoyGameController = (_ctrl as MsoyGameController);
        o["getAvailableFlow_v1"] = ctrl.getAvailableFlow_v1;
        o["awardFlow_v1"] = ctrl.awardFlow_v1;
        o["setChatEnabled_v1"] = ctrl.setChatEnabled_v1;
        o["getHeadShot_v1"] = getHeadShot_v1;
    }

    protected function getHeadShot_v1 (occupant :int, callback :Function) :void
    {
        validateConnected();
        var info :GameMemberInfo = _ezObj.occupantInfo.get(occupant) as GameMemberInfo;
        if (info != null) {
            var headshot :Headshot = _headshots[occupant];
            if (headshot == null) {
                _headshots[occupant] = headshot = new Headshot(info.headShot.getMediaPath());
            }
            headshot.newRequest(callback);
            return;
        }
        throw new Error("Failed to find occupant: " + occupant);
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
