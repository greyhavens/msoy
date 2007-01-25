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
        size: [ 300, 300 ],
        facePosition: [ 50, 50 ],
        faceCenter: [ 100, 100 ],
        hourPoint: [ 40, 130 ],
        minutePoint: [ 40, 130 ],
        secondPoint: [ 40, 130 ],
        smoothSeconds: true

        // to figure out:
        // swf: one instance
        // images: one instance? (or class?)
        // audio: one instance? (or class?)
    };

    public function Data ()
    {
        content["face"] = face__image;
        content["hourHand"] = hourHand__image;
        content["minuteHand"] = minuteHand__image;
        content["secondHand"] = secondHand__image;
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

    [Embed(source="face.png")]
    private var face__image :Class;

    [Embed(source="hour.png")]
    private var hourHand__image :Class;

    [Embed(source="minute.png")]
    private var minuteHand__image :Class;

    [Embed(source="second.png")]
    private var secondHand__image :Class;

//    [Embed(source="Clock.swf", mimeType="application/octet-stream")]
//    private var tits__bytes :Class;
}
}
