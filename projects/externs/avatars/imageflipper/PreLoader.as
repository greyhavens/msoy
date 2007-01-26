package {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.IOErrorEvent;

import flash.net.URLRequest;

[SWF(width="222", height="363")]
public class PreLoader extends Sprite
{
    public function PreLoader ()
    {
        _loader = new Loader();
        _loader.contentLoaderInfo.addEventListener(Event.COMPLETE, handleComplete);
        _loader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, handleError);

        _loader.contentLoaderInfo.sharedEvents.addEventListener("controlConnect",
            controlPassThru);

//        trace("Url: " + this.root.loaderInfo.url);
//        trace("Url: " + this.loaderInfo.url);
//        trace("Url: " + this.loaderInfo.url);

//        var ourURL :String = this.root.loaderInfo.url;
//        trace("ourURL: " + ourURL);
//        var lastSlash :int = ourURL.lastIndexOf("/");
//        var url :String = ourURL.substring(0, lastSlash + 1) + _contentURL;
        var url :String =
            //"http://tasman.sea.earth.threerings.net:8080/media/" +
            "http://bogocorp.com/~ray/tempsoy/" +
            _contentURL;
        _loader.load(new URLRequest(url));
    }

    protected function handleComplete (event :Event) :void
    {
        addChild(_loader.content);
    }

    protected function handleError (event :IOErrorEvent) :void
    {
        // nada?
    }

    /**
     * Pass-through the *Control hook-up stuff.
     */
    protected function controlPassThru (evt :Event) :void
    {
        this.root.loaderInfo.sharedEvents.dispatchEvent(evt.clone());
    }

    protected var _loader :Loader;

    protected var _contentURL :String = "95d7dc5a9a1c884bd692b130d5062f14e58c50f4.swf";
}
}
