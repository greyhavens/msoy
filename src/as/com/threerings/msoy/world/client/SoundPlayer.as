package com.threerings.msoy.world.client {

import flash.events.IOErrorEvent;
import flash.events.ProgressEvent;

import flash.media.Sound;
import flash.media.SoundChannel;

import flash.net.URLRequest;

import com.threerings.msoy.data.MediaData;

public class SoundPlayer
{
    public function SoundPlayer (desc :MediaData)
    {
        _desc = desc;

        _sound = new Sound(new URLRequest(desc.URL));

        _sound.addEventListener(IOErrorEvent.IO_ERROR, ioError);
        _sound.addEventListener(ProgressEvent.PROGRESS, loadingProgress);
    }

    /**
     * Play the sound.
     */
    public function play (startTime :Number = 0, loops :int = 0) :void
    {
        stop();
        _chan = _sound.play(startTime, loops);
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

    public function getMediaId () :int
    {
        return _desc.id;
    }

    public function getPosition () :Number
    {
        return (_chan == null) ? 0 : _chan.position;
    }

    protected function ioError (event :IOErrorEvent) :void
    {
        Log.getLog(this).warning("ioError loading sound [sound=" + _desc +
            ", error=" + event + "].");
    }

    protected function loadingProgress (event :ProgressEvent) :void
    {
        //trace("sound progress: " + event.bytesLoaded + "/" + event.bytesTotal);
    }

    protected var _sound :Sound;

    protected var _chan :SoundChannel;

    protected var _desc :MediaData;
}
}
