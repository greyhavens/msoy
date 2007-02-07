package {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.events.Event;

import flash.utils.ByteArray;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

public class Data
{
    public var content :Object = {
        bounce: 20,
        bounceFreq: 400
        // to figure out:
        // swf: one instance
        // images: one instance? (or class?)
        // audio: one instance? (or class?)
    };

    public function Data ()
    {
        content["image"] = image__image;
   //     loadAsset("used_for_swfs");
    }

    protected function loadAsset (name :String) :void
    {
        getAsset(name, function (o :Object) :void {
            content[name] = o;
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

    [Embed(source="ray.jpg")]
    private var image__image :Class;
}
}
