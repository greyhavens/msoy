//
// $Id$

package com.threerings.msoy.applets {

import flash.display.Loader;
import flash.display.Sprite;
import flash.display.StageAlign;
import flash.display.StageScaleMode;
import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.net.URLRequest;
import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.Security;
import flash.text.TextField;
import flash.text.TextFormat;

import com.kongregate.as3.client.KongregateAPI;
import com.kongregate.as3.client.events.KongregateEvent;

import com.threerings.msoy.data.UberClientModes;

// On Kongregate, the width limit is 700, but there appears to be
// no height limit.

// On newgrounds, width limit is 800, height limit 700

// miniclip- ? (new user registration was broken)

// addicting games: ?

// armorgames: dunno, submissions are reviewed, 1000 wide x 700 tall
// was submit-able

/**
 * Stub. A small wrapper to load up a whirled client and play a game or visit a room
 * from some 3rd party site.
 *
 * NOTE: Please avoid using any non-builtin classes here.
 *  1) We shouldn't need to
 *  2) It helps keep the size down
 *  3) It avoids possible problems. For example, if we use StringUtil in here, then that's loaded
 *    in. We _do_ put the client in a new ApplicationDomain, but I'm still worried about
 *    possible collision if our client uses newly-added methods in StringUtil and this stub
 *    has defined a version of StringUtil that is old and lacking those methods.
 */
//[SWF(width="400", height="400")]
[SWF(width="700", height="575")]
public class EmbedStub extends Sprite
{
    public static const WIDTH :int = 700;
    public static const HEIGHT :int = 575;

    /** This gets replaced. */
    public static const ARGS :String = "&&stubargs&&";

    /** This gets replaced. */
    public static const SERVER_URL :String = "//stuburl//";

    public static const CLIENT :String = "clients/world-client.swf";

    public function EmbedStub ()
    {
        init();
    }

    protected function init () :void
    {
        if (stage != null) {
            stage.scaleMode = StageScaleMode.NO_SCALE;
            stage.align = StageAlign.TOP_LEFT;
        }

        _label = new TextField();
        _label.width = WIDTH;
        _label.height = HEIGHT;
        _label.selectable = false;
        _label.wordWrap = true;
        var tf :TextFormat = new TextFormat();
        tf.font = "_sans";
        tf.size = 18;
        tf.bold = true;
        tf.color = 0xFFFFFF;
        _label.defaultTextFormat = tf;
        addChild(_label);
        _label.text = "Loading...";

        // allow all loaded content to cross-script this SWF
        // @TODO - is there any reason to make this more restrictive?
        Security.allowDomain("*");

        // now let's try loading the client
        _clientLoader = new Loader();
        _clientLoader.contentLoaderInfo.addEventListener(Event.INIT, handleLoaded);
        _clientLoader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, handleLoadError);
        _clientLoader.contentLoaderInfo.sharedEvents.addEventListener(
            UberClientModes.CLIENT_READY, handleClientReady);
        _clientLoader.load(new URLRequest(SERVER_URL + CLIENT),
            new LoaderContext(true, new ApplicationDomain(null)));
        addChild(_clientLoader);

        // if we're on Kongregate, try grabbing our username from the Kongregate API
        var url :String = stage.root.loaderInfo.loaderURL;
        if (url.indexOf("kongregate.com") != -1) {
            var kapi :KongregateAPI = new KongregateAPI();
            addChild(kapi);
            kapi.addEventListener(KongregateEvent.COMPLETE, function (... ignored) :void {
                if (kapi.user.getName() != "Guest") { // KAPI provides no constant for Guest
                    dispatchBridgeEvent(UberClientModes.GOT_EXTERNAL_NAME, kapi.user.getName());
                }
                removeChild(kapi);
            });
        }
    }

    /**
     * The uberclient will call this method to retrieve startup parameters.
     */
    public function getWhirledParams () :String
    {
        return "mode=10&" + ARGS;
    }

    protected function handleLoaded (... ignored) :void
    {
        removeChild(_label);
        _label = null;
    }

    protected function handleClientReady (... ignored) :void
    {
        var onClientReady :Function = _onClientReady;
        _onClientReady = CLIENT_IS_READY;
        if (onClientReady != null) {
            onClientReady();
        }
    }

    protected function handleLoadError (e :ErrorEvent) :void
    {
        removeChild(_clientLoader);
        _clientLoader = null;
        _label.text = "Error loading: " + e.text;
    }

    /**
     * Dispatches a bridge event to MsoyClient. If this is called before the client has reported in
     * as initialized, the event will be postponed until we do hear from the client.
     */
    protected function dispatchBridgeEvent (type :String, info :String) :void
    {
        if (_onClientReady == CLIENT_IS_READY) {
            _clientLoader.contentLoaderInfo.sharedEvents.dispatchEvent(new BridgeEvent(type, info));
        } else if (_onClientReady != null) {
            var chainedReady :Function = _onClientReady;
            _onClientReady = function () :void { chainedReady(); dispatchBridgeEvent(type, info); }
        } else {
            _onClientReady = function () :void { dispatchBridgeEvent(type, info); }
        }
    }

    protected var _clientLoader :Loader;
    protected var _label :TextField;
    protected var _onClientReady :Function;

    /** A marker assigned to _onClientReady once the client has reported ready. */
    protected static const CLIENT_IS_READY :Function = function () :void {}
}
}

import flash.events.Event;

class BridgeEvent extends Event
{
    public var info :String;

    public function BridgeEvent (type :String, info :String) {
        super(type);
        this.info = info;
    }

    override public function clone () :Event {
        return new BridgeEvent(type, info);
    }
}
