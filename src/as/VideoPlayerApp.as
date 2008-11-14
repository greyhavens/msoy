//
// $Id$

package {

import flash.display.Sprite;

import flash.events.Event;

import flash.external.ExternalInterface;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.video.FlvVideoPlayer;
import com.threerings.flash.video.VideoPlayer;

import com.threerings.msoy.ui.MsoyVideoDisplay;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.client.ExternalMediaDisplayer;
import com.threerings.msoy.item.client.ExternalMediaUtil;
import com.threerings.msoy.item.client.YouTubePlayer;

// TODO: Currently the video player only lets you do full-volume and mute.
// It would be nice to inherit a user's configured msoy volume, but we don't have
// a way to actually manipulate that here, so if they had everything muted, we wouldn't
// be able to fix it.
// Perhaps.. the right thing to do is:
// 1) Implement full volume control in the MsoyVideoDisplay UI.
// 2) When this standalone player starts up, start with the global volume setting.
[SWF(width="320", height="240")]
public class VideoPlayerApp extends Sprite
    implements ExternalMediaDisplayer
{
    public function VideoPlayerApp ()
    {
        this.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);
        try {
            if (ExternalInterface.available) {
                ExternalInterface.addCallback("setYoutubeId", setYoutubeId);
            }
        } catch (err :Error) {
            // not to worry
        }
        ParameterUtil.getParameters(this, gotParams);
    }

    /**
     * Exposed externally.
     */
    protected function setYoutubeId (id :String) :void
    {
        if (id == "") {
            setupBlank();
            return;
        }

        var youtubePlayer :YouTubePlayer = new YouTubePlayer();
        addPlayer(youtubePlayer);
        youtubePlayer.load(id);
    }

    /**
     * Our ParameterUtil callback.
     */
    protected function gotParams (params :Object) :void
    {
        if (!("video" in params)) {
            // just fill with white and call it good, waiting to be contacted externally
            setupBlank();
            return;
        }

        const url :String = String(params["video"]);

        // see if it's FLV or youtube
        switch (MediaDesc.suffixToMimeType(url)) {
        default:
            trace("VideoPlayer: unrecognized url: " + url);
            return;

        case MediaDesc.VIDEO_FLASH:
            var flvPlayer :FlvVideoPlayer = new FlvVideoPlayer();
            addPlayer(flvPlayer);
            flvPlayer.load(url);
            break;

        case MediaDesc.EXTERNAL_YOUTUBE:
            // we act like an ExternalMediaDisplayer ourselves, so that we can access the id
            // when it's available
            ExternalMediaUtil.fetch(url, this);
            break;
        }
    }

    // from interface ExternalMediaDisplayer
    public function displayExternal (data :Object) :void
    {
        var youtubeId :String = String(data.id);
        // try telling the host GWT page about the id (don't worry if it ignores it)
        try {
            if (ExternalInterface.available) {
                ExternalInterface.call("gotYoutubeId", youtubeId);
            }
        } catch (err :Error) {
            // nada
        }
        // then, actually display the video
        setYoutubeId(youtubeId);
    }

    protected function addPlayer (vid :VideoPlayer) :void
    {
        // remove any previous player
        handleUnload();

        _vid = vid;
        addChild(new MsoyVideoDisplay(vid));
    }

    protected function setupBlank () :void
    {
        handleUnload();

        var s :Sprite = new Sprite();
        s.graphics.beginFill(0xFFFFFF);
        s.graphics.drawRect(0, 0, 320, 240);
        s.graphics.endFill();
        addChild(s);
    }

    protected function handleUnload (event :Event = null) :void
    {
        if (_vid != null) {
            _vid.unload();
            _vid = null;
        }
        while (numChildren > 0) {
            removeChildAt(0);
        }
    }

    /** Our video displayer component. */
    protected var _vid :VideoPlayer;
}
}
