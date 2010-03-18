//
// $Id$

package com.threerings.msoy.item.client {

import flash.display.DisplayObject;
import flash.display.Loader;

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.StatusEvent;

import flash.geom.Point;

import flash.net.LocalConnection;
import flash.net.URLRequest;

import flash.utils.clearInterval;
import flash.utils.setInterval;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;
import flash.system.Security;
import flash.system.System;

import com.threerings.util.DelayUtil;
import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.display.LoaderUtil;

import com.threerings.media.MediaPlayerCodes;
import com.threerings.media.VideoPlayer;

import com.threerings.msoy.client.DeploymentConfig;

public class YouTubePlayer extends EventDispatcher
    implements VideoPlayer, ExternalMediaDisplayer
{
    public function YouTubePlayer ()
    {
        // are we OK with this? it does not seem to be strictly necessary.
        Security.allowDomain("www.youtube.com");

        _loader = new Loader();
        // since we autoplay, we don't need to have the youtube play button be clickable,
        // and we also suppress clicking on the youtube watermark.
        _loader.mouseEnabled = false;
        _loader.mouseChildren = false;
        _loader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, onLoaderError);
        _loader.contentLoaderInfo.addEventListener(Event.INIT, onLoaderInit);
        _loader.load(new URLRequest(YOUTUBE_PLAYER_URL),
                     new LoaderContext(false, new ApplicationDomain(null)));
    }

    // from interface ExternalMediaDisplayer
    public function displayExternal (data :Object) :void
    {
        load(String(data.id));
    }

    public function load (id :String) :void
    {
        if (_loader == null) {
            log.warning("Ignoring request to load after unloading.");
            return;
        }

        _videoId = id;
        if (getState() != MediaPlayerCodes.STATE_UNREADY) {
            doLoadVideo();
        }
    }

    // from VideoPlayer
    public function getDisplay () :DisplayObject
    {
        return _loader;
    }

    // from VideoPlayer
    public function getState () :int
    {
        if (_player == null) {
            return MediaPlayerCodes.STATE_UNREADY;
        }
        switch(_player.getPlayerState()) {
        default:
            return MediaPlayerCodes.STATE_UNREADY;
        case YT_STATE_UNSTARTED:
        case YT_STATE_CUED:
        case YT_STATE_BUFFERING:
            return MediaPlayerCodes.STATE_READY;
        case YT_STATE_PLAYING:
            return MediaPlayerCodes.STATE_PLAYING;
        case YT_STATE_PAUSED:
            return MediaPlayerCodes.STATE_PAUSED;
        case YT_STATE_ENDED:
            return MediaPlayerCodes.STATE_STOPPED;
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
        if (ensurePlayer("play")) {
            _player.playVideo();
        }
    }

    // from VideoPlayer
    public function pause () :void
    {
        if (ensurePlayer("pause")) {
            _player.pauseVideo();
        }
    }

    // from VideoPlayer
    public function getDuration () :Number
    {
        return ensurePlayer("getDuration") ? _player.getDuration() : NaN;
    }

    // from VideoPlayer
    public function getPosition () :Number
    {
        return ensurePlayer("getPosition") ? _player.getCurrentTime() : NaN;
    }

    // from VideoPlayer
    public function seek (position :Number) :void
    {
        if (ensurePlayer("seek")) {
            _player.seekTo(position, true);
        }
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
        if (ensurePlayer("setVolume")) {
            _player.setVolume(_volume * 100);
        }
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

        DelayUtil.delayFrames(2, LoaderUtil.unload, [ _loader ]);
        _loader.contentLoaderInfo.removeEventListener(IOErrorEvent.IO_ERROR, onLoaderError);
        _loader.contentLoaderInfo.removeEventListener(Event.INIT, onLoaderInit);
        _loader = null;
    }

    protected function doLoadVideo () :void
    {
        _player.loadVideoById(_videoId);
        _player.setSize(320, 240);
        _interval = setInterval(poll, PLAYER_POLL_INTERVAL);
    }

    protected function poll () :void
    {
        if (_player != null) {
            var duration :Number = getDuration();
            if (duration > 0) {
                dispatchEvent(new ValueEvent(MediaPlayerCodes.DURATION, duration));
            }
            var position :Number = getPosition();
            if (position > 0) {
                dispatchEvent(new ValueEvent(MediaPlayerCodes.POSITION, position));
            }
        } else {
            clearInterval(_interval);
        }
    }

    protected function ensurePlayer (cmd :String) :Boolean
    {
        if (_player != null) {
            return true;
        }
        log.debug("YouTube player not initialized, dropping command", "cmd", cmd);
        return false;
    }

    protected function shutdownPlayer () :void
    {
        if (_player != null) {
            _player.destroy();
            _player.removeEventListener("onReady", onPlayerReady);
            _player.removeEventListener("onError", onPlayerError);
            _player.removeEventListener("onStateChange", onPlayerStateChange);
            _player = null;
        }
        _videoId = null;
    }

    // feedback from the loader
    protected function onLoaderInit (event :Event):void {
        _player = _loader.content;
        _player.addEventListener("onReady", onPlayerReady);
        _player.addEventListener("onError", onPlayerError);
        _player.addEventListener("onStateChange", onPlayerStateChange);
    }

    protected function onLoaderError (evt :ErrorEvent) :void
    {
        log.warning("Error loading player: " + evt.text);
    }

    // feedback from the player
    protected function onPlayerReady (event :Event) :void
    {
        setVolume(_volume);

        // if a video has been requested, fire it up
        if (_videoId != null) {
            doLoadVideo();
        }
    }

    protected function onPlayerError (event :Event) :void
    {
        var code :int = Object(event).data;
        dispatchEvent(new ValueEvent(MediaPlayerCodes.ERROR, "YouTubePlayer error: " + code));
    }

    protected function onPlayerStateChange (event :Event) :void
    {
        if (_loader != null) {
            dispatchEvent(new ValueEvent(MediaPlayerCodes.STATE, getState()));
        }
    }

    // used to load the youtube player SWF, will only be null after unloading
    protected var _loader :Loader;
    // the actual player object, on which we call API methods and from which we receive events
    protected var _player :Object;
    // the youtube ID of the video
    protected var _videoId :String;

    // we poll for duration and position every so often
    protected var _interval :Number;

    /** The current volume of the player. */
    protected var _volume :Number = 1;

    // youtube API states
    protected static const YT_STATE_UNSTARTED :int = -1;
    protected static const YT_STATE_ENDED :int = 0;
    protected static const YT_STATE_PLAYING :int = 1;
    protected static const YT_STATE_PAUSED :int = 2;
    protected static const YT_STATE_BUFFERING :int = 3;
    protected static const YT_STATE_CUED :int = 5;

    // I can't actually find any reference to the key argument being supported in the
    // modern API documentation (http://code.google.com/apis/youtube/flash_api_reference.html)
    // but we'll include it until we find out it's truly obsolete -- Zell
    protected static const YOUTUBE_PLAYER_URL :String =
        "http://www.youtube.com/apiplayer?version=3&key=AI39si7biXRBHFJQi4eHyd8nia_GmMjsbZ-3QdN8i2BlaxGx1KegEoAm8R-eT_Xuspn6fa5zdyByUj58m4ZecgUpG5ROCns0hA";

    // how often to poll for duration and position (in ms)
    protected static const PLAYER_POLL_INTERVAL :int = 250;

    protected static const log :Log = Log.getLog(YouTubePlayer);
}
}
