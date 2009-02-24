//
// $Id$

package com.threerings.msoy.item.client {

import flash.display.DisplayObject;
import flash.display.Loader;

import flash.events.ErrorEvent;
import flash.events.EventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.StatusEvent;

import flash.geom.Point;

import flash.net.LocalConnection;
import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import com.threerings.util.Log;
import com.threerings.util.MethodQueue;
import com.threerings.util.ValueEvent;

import com.threerings.flash.LoaderUtil;

import com.threerings.flash.media.MediaPlayerCodes;
import com.threerings.flash.media.VideoPlayer;

import com.threerings.msoy.client.DeploymentConfig;

public class YouTubePlayer extends EventDispatcher
    implements VideoPlayer, ExternalMediaDisplayer
{
    public function YouTubePlayer ()
    {
        _loader = new Loader();
        // since we autoplay, we don't need to have the youtube play button be clickable,
        // and we also suppress clicking on the youtube watermark.
        _loader.mouseEnabled = false;
        _loader.mouseChildren = false;
    }

    // from interface ExternalMediaDisplayer
    public function displayExternal (data :Object) :void
    {
        load(String(data.id));
    }

    public function load (id :String) :void
    {
        if (_ytState == UNLOAD_STATE) {
            log.warning("Ignoring request to load after unloading.");
            return;
        }

        // unload any previous..
        shutdownPlayer();

        _videoId = id;
        _ytState = -2;

        // create a new ID for communicating with the stub
        var chars :Array = [];
        for (var ii :int = 0; ii < 12; ii++) {
            var char :int = int(Math.random() * 26);
            var cap :Boolean =  Math.random() > .5;
            chars.push(char + (cap ? 65 : 97));
        }
        _stubId = String.fromCharCode.apply(null, chars);

        _lc = new LocalConnection();
        _lcReady = false;
        _lc.addEventListener(StatusEvent.STATUS, handleConnStatus);
        _lc.client = {
            setReady: handleSetReady,
            stateChanged: handleStateChanged,
            duration: handleGotDuration,
            position: handleGotPosition
        };
        _lc.connect("_" + _stubId + "-s");

        var url :String = DeploymentConfig.staticMediaURL + YOUTUBE_STUB_URL +
            "?name=" + _stubId;
        _loader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, handleError);
        _loader.load(new URLRequest(url), new LoaderContext(false, new ApplicationDomain(null)));
    }

    // from VideoPlayer
    public function getDisplay () :DisplayObject
    {
        return _loader;
    }

    // from VideoPlayer
    public function getState () :int
    {
        switch (_ytState) {
        default: return MediaPlayerCodes.STATE_UNREADY;
        case 5: return MediaPlayerCodes.STATE_READY;
        case 1: return MediaPlayerCodes.STATE_PLAYING;
        case 2: return MediaPlayerCodes.STATE_PAUSED;
        }
    }

    // from VideoPlayer
    public function getSize () :Point
    {
        return new Point(320, 240); // This is constant, for now
    }

    // from VideoPlayer
    public function play () :void
    {
        send("doPlay");
    }

    // from VideoPlayer
    public function pause () :void
    {
        send("doPause");
    }

    // from VideoPlayer
    public function getDuration () :Number
    {
        return _duration;
    }

    // from VideoPlayer
    public function getPosition () :Number
    {
        return _position;
    }

    // from VideoPlayer
    public function seek (position :Number) :void
    {
        send("doSeek", position);
    }

    // from VideoPlayer
    public function getVolume () :Number
    {
        return _volume;
    }

    // from VideoPlayer
    public function setVolume (volume :Number) :void
    {
        _volume = Math.max(0, Math.min(1, volume));
        send("doVolume", _volume);
    }

    // from VideoPlayer
    public function getMetadata () :Object
    {
        return null;
    }

    // from VideoPlayer
    public function unload () :void
    {
        shutdownPlayer();

        _ytState = UNLOAD_STATE;
    }

    protected function shutdownPlayer () :void
    {
        if (_lc != null) {
            send("doUnload");
            MethodQueue.callLater(MethodQueue.callLater, [ LoaderUtil.unload, [ _loader ] ]);
            _lc.removeEventListener(StatusEvent.STATUS, handleConnStatus);
            _lc = null;
            _stubId = null;
            _loader = new Loader();
        }
        _videoId = null;
    }

    protected function handleConnStatus (event :StatusEvent) :void
    {
        if (event.level != "status") {
            log.debug("localConnection status", "level", event.level, "code", event.code);
        }
    }

    protected function handleError (evt :ErrorEvent) :void
    {
        log.warning("Error loading: " + evt.text);
    }

    protected function send (method :String, ... args) :void
    {
        if (_lc == null || !_lcReady) {
            log.warning("No lc or not ready; dropping msg", "method", method, "args", args);
            return;
        }
        args.unshift("_" + _stubId, method);
        _lc.send.apply(null, args);
    }

    protected function handleSetReady (... args) :void
    {
        _lcReady = true;
    }

    /**
     * From the stub.
     */
    protected function handleStateChanged (state :int) :void
    {
        if (_ytState == UNLOAD_STATE) {
            return;
        }
        _ytState = state;

        setVolume(_volume);
        switch (_ytState) {
        case -1: // unstarted
            send("loadVideo", _videoId);
            break;
        }

        dispatchEvent(new ValueEvent(MediaPlayerCodes.STATE, getState()));
    }

    protected function handleGotDuration (duration :Number) :void
    {
        _duration = duration;
        dispatchEvent(new ValueEvent(MediaPlayerCodes.DURATION, duration));
    }

    protected function handleGotPosition (position :Number) :void
    {
        _position = position
        dispatchEvent(new ValueEvent(MediaPlayerCodes.POSITION, position));
    }

    protected var _loader :Loader;

    protected var _videoId :String;

    protected var _stubId :String;

    protected var _lc :LocalConnection;

    protected var _lcReady :Boolean;

    /** The current state of the youtube chromeless player. */
    protected var _ytState :int;

    protected var _duration :Number = NaN;

    protected var _position :Number = NaN;

    protected var _volume :Number = 1;

    protected static const UNLOAD_STATE :int = int.MIN_VALUE;

    protected static const log :Log = Log.getLog(YouTubePlayer);

    protected static const YOUTUBE_STUB_URL :String = "youtubestub.swf";
}
}
