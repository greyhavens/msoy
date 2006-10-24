package com.threerings.msoy.world.client {

import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;

import flash.media.Sound;
import flash.media.SoundChannel;

import flash.net.URLRequest;

import com.threerings.msoy.item.web.MediaDesc;

public class SoundPlayer
{
    public function SoundPlayer (desc :MediaDesc)
    {
        _desc = desc;

        _sound = new Sound(new URLRequest(desc.getMediaPath()));

        _sound.addEventListener(IOErrorEvent.IO_ERROR, ioError);
//        _sound.addEventListener(ProgressEvent.PROGRESS, loadingProgress);
//        _sound.addEventListener(Event.COMPLETE, loadingComplete);
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
        _chan = _sound.play(startTime, loops);
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
     * Get the media descriptor for the music we're playing.
     */
    public function getMedia () :MediaDesc
    {
        return _desc;
    }

    /**
     * Get the current position of this media.
     */
    public function getPosition () :Number
    {
        return (_chan == null) ? 0 : _chan.position;
    }

    protected function ioError (event :IOErrorEvent) :void
    {
        Log.getLog(this).warning("ioError loading sound [sound=" + _desc +
            ", error=" + event + "].");
    }

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
}
}
