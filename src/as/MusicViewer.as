//
// $Id$

package {

import flash.display.Loader;
import flash.display.Sprite;

import flash.events.Event;

import flash.net.URLRequest;

import com.threerings.util.ParameterUtil;

import com.threerings.msoy.room.client.SoundPlayer;

[SWF(width="320", height="240")]
public class MusicViewer extends Sprite
{
    public function MusicViewer ()
    {
        this.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        ParameterUtil.getParameters(this, gotParams);
    }

    protected function gotParams (params :Object) :void
    {
        if ("music" in params) {
            _snd = new SoundPlayer(String(params["music"]));
            _snd.play();
        }
        if ("icon" in params) {
            var loader :Loader = new Loader();
            addChild(loader);
            loader.load(new URLRequest(String(params("icon"))));
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
