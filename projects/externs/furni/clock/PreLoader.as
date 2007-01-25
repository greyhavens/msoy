package {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.events.Event;

import flash.net.URLRequest;

[SWF(width="300", height="300")]
public class PreLoader extends Sprite
{
    public function PreLoader ()
    {
        _loader = new Loader();
        _loader.contentLoaderInfo.addEventListener(Event.COMPLETE, handleComplete);
        _loader.load(new URLRequest(_contentURL));
    }

    protected function handleComplete (event :Event) :void
    {
        this.parent.addChild(_loader.content);
        this.parent.removeChild(this);
    }

    protected var _loader :Loader;

    protected var _contentURL :String = "file:Clock.swf";
}
}
