//
// $Id$

package {

import flash.display.Sprite;

import flash.events.Event;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.video.FlvVideoPlayer;
import com.threerings.flash.video.SimpleVideoDisplay;

[SWF(width="320", height="240")]
public class VideoViewer extends Sprite
{
    public function VideoViewer ()
    {
        _vid = new FlvVideoPlayer();
        addChild(new SimpleVideoDisplay(_vid));

        this.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);
        ParameterUtil.getParameters(this, gotParams);
    }

    /**
     * Our ParameterUtil callback.
     */
    protected function gotParams (params :Object) :void
    {
        _vid.load(String(params["video"]));
    }

    protected function handleUnload (event :Event) :void
    {
        _vid.unload();
    }

    /** Our video displayer component. */
    protected var _vid :FlvVideoPlayer;
}
}
