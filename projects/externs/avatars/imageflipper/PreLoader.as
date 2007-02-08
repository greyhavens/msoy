package {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.IOErrorEvent;

import flash.net.URLRequest;

import flash.utils.describeType; // function import

[SWF(width="222", height="363")]
public class PreLoader extends Sprite
{
    public function PreLoader ()
    {
        _loader = new Loader();
        _loader.contentLoaderInfo.addEventListener(Event.COMPLETE, handleComplete);
        _loader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, handleError);

        // register our listener weakly, so that we can be garbage collected
        // if that's the only reference back to us
        _loader.contentLoaderInfo.sharedEvents.addEventListener("controlConnect",
            this.root.loaderInfo.sharedEvents.dispatchEvent, false, 0, true);

        var ourInfo :LoaderInfo = this.loaderInfo;
        if (ourInfo.bytesLoaded != ourInfo.bytesTotal) {
            ourInfo.addEventListener(Event.COMPLETE, loadingComplete, false, 0, true);
        } else {
            trace("ourInfo.bytesLoaded: " + ourInfo.bytesLoaded);
            loadingComplete();
        }
    }

    protected function loadingComplete (event :Event = null) :void
    {
        trace("We are complete" + ((event == null) ? " now" : "") + ".");

        trace("Url: " + this.root.loaderInfo.url);
        trace("Url: " + this.root.loaderInfo.loaderURL);
        trace("Url: " + this.loaderInfo.url);
        trace("Url: " + this.loaderInfo.loaderURL);
        trace("params: " + describeType(this.root.loaderInfo.parameters));

//        var ourURL :String = this.root.loaderInfo.url;
//        trace("ourURL: " + ourURL);
//        var lastSlash :int = ourURL.lastIndexOf("/");
//        var url :String = ourURL.substring(0, lastSlash + 1) + _contentURL;
        var url :String =
            "http://tasman.sea.earth.threerings.net:8080/media/" +
            //"http://bogocorp.com/~ray/tempsoy/" +
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

    protected var _loader :Loader;

    protected var _contentURL :String = "6c015e7cf97af6418ca304acd0128d27ad4913f5.swf";
}
}
