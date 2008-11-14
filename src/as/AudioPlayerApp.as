//
// $Id$

package {

import flash.display.Sprite;

import flash.events.Event;

import com.threerings.util.ParameterUtil;

import com.threerings.msoy.room.client.SoundPlayer;

[SWF(width="320", height="240")]
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
            _snd = new SoundPlayer(String(params["audio"]));
            _snd.play();
        }
    }

    protected function handleUnload (event :Event) :void
    {
        if (_snd != null) {
            _snd.close();
            _snd = null;
        }
    }

    protected var _snd :SoundPlayer;
}
}
