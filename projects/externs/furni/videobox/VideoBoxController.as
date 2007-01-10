//
// $Id$

package {

import flash.display.Sprite;

import flash.text.TextField;
import flash.text.TextFieldType;

import flash.events.Event;
import flash.events.KeyboardEvent;

import flash.system.Security;

import flash.ui.Keyboard;

import com.adobe.webapis.youtube.Video;
import com.adobe.webapis.youtube.YouTubeService;
import com.adobe.webapis.youtube.events.YouTubeServiceEvent;

// TODO: This doesn't presently work because youtube has changed their
// crossdomain.xml file to prevent the youtube flash API from working at all.
//
[SWF(width="300", height="500")]
public class VideoBoxController extends Sprite
{
    public function VideoBoxController ()
    {
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        // this is my (Ray Greenwell)'s private API key
        _youtube = new YouTubeService("BJMICrPrbZk");

        _youtube.addEventListener(YouTubeServiceEvent.VIDEOS_LIST_BY_TAG,
            handleVideosByTag);

        _tagField = new TextField();
        _tagField.type = TextFieldType.INPUT;
        addChild(_tagField);
        _tagField.addEventListener(KeyboardEvent.KEY_DOWN, handleKey);
    }

    protected function handleVideosByTag (evt :YouTubeServiceEvent) :void
    {
        trace("Got data: " + evt.data);
    }

    protected function handleKey (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.ENTER) {
            var tag :String = _tagField.text;
            _youtube.videos.listByTag(tag);
        }
    }

    /**
     * Take care of releasing resources when we unload.
     */
    protected function handleUnload (event :Event) :void
    {
    }

    protected var _tagField :TextField;

    protected var _youtube :YouTubeService;
}
}
