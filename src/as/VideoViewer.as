//
// $Id$

package {

import flash.display.Sprite;

import flash.events.Event;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.video.FlvVideoPlayer;
import com.threerings.flash.video.SimpleVideoDisplay;
import com.threerings.flash.video.VideoPlayer;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.client.ExternalMediaUtil;
import com.threerings.msoy.ui.YouTubePlayer;

[SWF(width="320", height="240")]
public class VideoViewer extends Sprite
{
    public function VideoViewer ()
    {
        this.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);
        ParameterUtil.getParameters(this, gotParams);
    }

    /**
     * Our ParameterUtil callback.
     */
    protected function gotParams (params :Object) :void
    {
        const url :String = String(params["video"]);
        var display :SimpleVideoDisplay;

        // see if it's FLV or youtube
        switch (MediaDesc.suffixToMimeType(url)) {
        default:
            trace("WTF!: " + url);
            return;

        case MediaDesc.VIDEO_FLASH:
            var flvPlayer :FlvVideoPlayer = new FlvVideoPlayer();
            _vid = flvPlayer;
            display = new SimpleVideoDisplay(flvPlayer);
            flvPlayer.load(url);
            break;

        case MediaDesc.EXTERNAL_YOUTUBE:
            var youtubePlayer :YouTubePlayer = new YouTubePlayer();
            _vid = youtubePlayer;
            display = new SimpleVideoDisplay(youtubePlayer);
            ExternalMediaUtil.fetch(url, youtubePlayer);
            break;
        }

        addChild(display);
    }

    protected function handleUnload (event :Event) :void
    {
        _vid.unload();
    }

    /** Our video displayer component. */
    protected var _vid :VideoPlayer;
}
}
