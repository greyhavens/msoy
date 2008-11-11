//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.MouseEvent;
import flash.events.StatusEvent;

import flash.geom.Point;

import flash.net.LocalConnection;
import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import com.threerings.util.Log;
import com.threerings.util.MethodQueue;
import com.threerings.util.Util;
import com.threerings.util.ValueEvent;

import com.threerings.flash.LoaderUtil;

import com.threerings.flash.video.VideoPlayer;
import com.threerings.flash.video.VideoPlayerCodes;

import com.threerings.msoy.client.DeploymentConfig;

import com.threerings.msoy.item.client.ExternalMediaDisplayer;

public class YouTubePlayer extends EventDispatcher
    implements VideoPlayer, ExternalMediaDisplayer
{
    public function YouTubePlayer ()
    {
        _loader = new Loader();
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
        // unload any previous..
        unload();

        _videoId = id;

        // create a new ID for communicating with the stub
        var chars :Array = [];
        for (var ii :int = 0; ii < 12; ii++) {
            var char :int = int(Math.random() * 26);
            var cap :Boolean =  Math.random() > .5;
            chars.push(char + (cap ? 65 : 97));
        }
        _stubId = String.fromCharCode.apply(null, chars);
        log.debug("Youtube player starting with secret id: " + _stubId);

        _lc = new LocalConnection();
        _lc.client = {
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
        default: return VideoPlayerCodes.STATE_UNREADY;
        case 5: return VideoPlayerCodes.STATE_READY;
        case 1: return VideoPlayerCodes.STATE_PLAYING;
        case 2: return VideoPlayerCodes.STATE_PAUSED;
        }
    }

    // from VideoPlayer
    public function getSize () :Point
    {
        return null; // TODO
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
        return 1; // TODO
    }

    // from VideoPlayer
    public function setVolume (volume :Number) :void
    {
        // TODO
    }

    // from VideoPlayer
    public function unload () :void
    {
        if (_lc != null) {
            send("doUnload");
            MethodQueue.callLater(MethodQueue.callLater, [ LoaderUtil.unload, [ _loader ] ]);
            _lc = null;
            _stubId = null;
            _loader = new Loader();
        }

        _ytState = int.MIN_VALUE;
        _videoId = null;
    }

    protected function handleError (evt :ErrorEvent) :void
    {
        log.warning("Error loading: " + evt.text);
    }

    protected function send (method :String, ... args) :void
    {
        if (_lc == null) {
            log.warning("No lc!");
            return;
        }
        log.debug("Sending to as2", "method", method);
        args.unshift("_" + _stubId, method);
        _lc.send.apply(null, args);
    }

    /**
     * From the stub.
     */
    protected function handleStateChanged (state :int) :void
    {
        _ytState = state;
        log.debug("=== got new state from as2: playerState: " + state);

        switch (_ytState) {
        case -1: // unstarted
            send("cueVideo", _videoId);
            break;
        }

        dispatchEvent(new ValueEvent(VideoPlayerCodes.STATE, getState()));
    }

    protected function handleGotDuration (duration :Number) :void
    {
        _duration = duration;
        dispatchEvent(new ValueEvent(VideoPlayerCodes.DURATION, duration));
        log.debug("Got duration from as2: " + duration);
    }

    protected function handleGotPosition (position :Number) :void
    {
        _position = position
        dispatchEvent(new ValueEvent(VideoPlayerCodes.POSITION, position));
    }

    protected var _loader :Loader;

    protected var _videoId :String;

    protected var _stubId :String;

    protected var _lc :LocalConnection;

    /** The current state of the youtube chromeless player. */
    protected var _ytState :int;

    protected var _duration :Number = NaN;

    protected var _position :Number = NaN;

    protected static const log :Log = Log.getLog(YouTubePlayer);

    protected static const YOUTUBE_STUB_URL :String = "youtubestub.swf";
}
}
