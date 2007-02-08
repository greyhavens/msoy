package {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.events.Event;

import flash.utils.ByteArray;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

public class RemixAssets
{
    public static const bounce :int = 40;

    public static const bounceFreq :int = 200;

    [Embed(source="ray.jpg")]
    public static const image :Class;

    // Everything below this line is concerned with loading SWF assets

    public function RemixAssets ()
    {
   //     loadAsset("used_for_swfs");
    }

    protected function loadAsset (name :String) :void
    {
        getAsset(name, function (o :Object) :void {
            this[name] = o;
        });
    }

    protected function getAsset (name :String, callback :Function) :void
    {
        var clazz :Class = (this[name + "__bytes"] as Class);
        var bytes :ByteArray = (new clazz() as ByteArray);

        var l :Loader = new Loader();
        l.loadBytes(bytes);
        l.contentLoaderInfo.addEventListener(Event.COMPLETE,
            function (evt :Event) :void {
                callback(l.content);
            });
    }

}
}
