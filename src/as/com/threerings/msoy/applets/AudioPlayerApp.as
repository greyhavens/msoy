//
// $Id$

package com.threerings.msoy.applets {

import flash.display.Sprite;

import flash.events.Event;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.media.Mp3AudioPlayer;

import com.threerings.msoy.ui.MsoyAudioDisplay;

[SWF(width="320", height="68")]
public class AudioPlayerApp extends Sprite
{
    public function AudioPlayerApp ()
    {
        this.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        ParameterUtil.getParameters(this, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        if ("audio" in params) {
            var player :Mp3AudioPlayer = new Mp3AudioPlayer();
            _aud = new MsoyAudioDisplay(player);
            addChild(_aud);

            player.load(String(params["audio"]));
        }
    }

    protected function handleUnload (event :Event) :void
    {
        if (_aud != null) {
            _aud.unload();
            _aud = null;
        }
    }

    protected var _aud :MsoyAudioDisplay;
}
}
