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

class Headshot
{
    public var content :DisplayObject;
    public var callbacks :Array = [ ];
    public var success :Boolean;

    function Headshot (url :String)
    {
        callbacks = [ ];

        var loader :Loader = new Loader();
        loader.load(new URLRequest(url), new LoaderContext(false, new ApplicationDomain(null)));

        var info :LoaderInfo = loader.contentLoaderInfo;
        info.addEventListener(Event.COMPLETE, loadingComplete);
        info.addEventListener(IOErrorEvent.IO_ERROR, loadError);
    }

    public function newRequest (callback :Function) :void
    {
        if (content != null) {
            respondTo(callback);
        } else {
            callbacks.push(callback);
        }
    }

    protected function respondTo (callback :Function) :void
    {
        callback(content, success);
    }

    protected function loadingComplete (event :Event) :void
    {
        var info :LoaderInfo = (event.target as LoaderInfo);
        info.removeEventListener(Event.COMPLETE, loadingComplete);
        info.removeEventListener(IOErrorEvent.IO_ERROR, loadError);

        success = true;
        content = info.loader.content;
        for (var ii :int = 0; ii < callbacks.length; ii ++) {
            respondTo(callbacks[ii]);
        }
        callbacks = null;
    }

    protected function loadError (event :IOErrorEvent) :void
    {
        success = false;
        content = ImageUtil.createErrorImage(100, 100);
        for (var ii :int = 0; ii < callbacks.length; ii ++) {
            respondTo(callbacks[ii]);
        }
        callbacks = null;
    }
}
