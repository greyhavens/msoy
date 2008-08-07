//
// $Id$

package com.threerings.msoy.room.client {

import flash.errors.IOError;

import flash.events.Event;
import flash.events.EventDispatcher;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;

import flash.media.Sound;
import flash.media.SoundChannel;
import flash.media.SoundTransform;

import flash.net.URLRequest;

import com.threerings.util.Log;

import com.threerings.msoy.data.all.MediaDesc;

public class SoundPlayer extends EventDispatcher
{
    /**
     * Shit, this was originally supposed to take a MediaDesc, the thought
     * being that you could find the SoundPlayer and tag the media...
     */
    public function SoundPlayer (url :String)
    {
        _url = url;

        _sound = new Sound(new URLRequest(url));

        // TODO: rethink? Adding listeners for these events will provide
        // access to the _sound, which we may want to prevent so that
        // this is the only place in control.
        _sound.addEventListener(IOErrorEvent.IO_ERROR, dispatchEvent);
        _sound.addEventListener(ProgressEvent.PROGRESS, dispatchEvent);
        _sound.addEventListener(Event.COMPLETE, dispatchEvent);
    }

    /**
     * Get the length of the sound, in ms.
     * TODO: find the fuck out if this is known before COMPLETE, or what.
     * God forbid that Adobe's docs could help us in this department.
     */
    public function getLength () :Number
    {
        return _sound.length;
    }

    /**
     * Set the volume of this sound.
     */
    public function setVolume (volume :Number) :void
    {
        _volume = volume;
        if (_chan != null) {
            _chan.soundTransform = new SoundTransform(_volume);
        }
    }

    /**
     * Play the sound.
     *
     * Using the startTime parameter is discouraged right now, as high
     * values seem to crash the flash player, as well as cause looping
     * to wrap around to a position other than the begining of the sound.
     */
    public function play (startTime :Number = 0, loops :int = 0) :void
    {
        stop();
        _chan = _sound.play(startTime, loops, new SoundTransform(_volume));
        if (_chan == null) {
            Log.getLog(this).warning("All sound channels are in use; " +
                "unable to play sound [sound=" + _desc + "].");
        }
    }

    /**
     * Loop the sound.
     */
    public function loop (startTime :Number = 0) :void
    {
        play(startTime, int.MAX_VALUE);
    }

    /**
     * Is the sound playing?
     */
    public function isPlaying () :Boolean
    {
        return (_chan != null);
    }

    /**
     * Stop playing (or looping) the sound.
     */
    public function stop () :void
    {
        if (_chan != null) {
            _chan.stop();
            _chan = null;
        }
    }

    /**
     * Close any loading data, stop the sound, etc.
     * AKA shutdown
     */
    public function close () :void
    {
        try {
            _sound.close();
        } catch (err :IOError) {
            // toss
        }
        stop();
    }

//    /**
//     * Get the media descriptor for the music we're playing.
//     */
//    public function getMedia () :MediaDesc
//    {
//        return _desc;
//    }

    /**
     * Get the original url for the music we've loaded.
     */
    public function getURL () :String
    {
        return _url;
    }

    /**
     * Get the current position of this media.
     */
    public function getPosition () :Number
    {
        return (_chan == null) ? 0 : _chan.position;
    }

    protected function handleSoundComplete (event :Event) :void
    {
        // simply let go of our _chan reference
        stop();

        // dispatch this mofo, too
        dispatchEvent(event);
    }

//    protected function ioError (event :IOErrorEvent) :void
//    {
//        Log.getLog(this).warning("ioError loading sound [sound=" + _desc +
//            ", error=" + event + "].");
//    }
//
//    protected function loadingProgress (event :ProgressEvent) :void
//    {
//        trace("sound progress: " + event.bytesLoaded +
//            "/" + event.bytesTotal);
//    }
//
//    protected function loadingComplete (event :Event) :void
//    {
//        trace("loading complete, total length=" + _sound.length);
//    }

    protected var _sound :Sound;

    protected var _chan :SoundChannel;

    protected var _desc :MediaDesc;
    protected var _url :String;

    protected var _volume :Number = 1;
}
}
