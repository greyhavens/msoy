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

//        var ourURL :String = this.root.loaderInfo.url;
//        trace("ourURL: " + ourURL);
//        var lastSlash :int = ourURL.lastIndexOf("/");
//        var url :String = ourURL.substring(0, lastSlash + 1) + _contentURL;
        var url :String = "http://tasman.sea.earth.threerings.net:8080/media/" +
            _contentURL;
        _loader.load(new URLRequest(url));
    }

    protected function handleComplete (event :Event) :void
    {
        this.parent.addChild(_loader.content);
        this.parent.removeChild(this);
    }

    protected function handleError (event :IOErrorEvent) :void
    {
        // nada?
    }

    protected var _loader :Loader;

    protected var _contentURL :String = "95d7dc5a9a1c884bd692b130d5062f14e58c50f4.swf";
}
}
