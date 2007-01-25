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
        faceCenter: [ 100, 100 ],
        hourPoint: [ 10, 50 ],
        minutePoint: [ 10, 100 ],
        secondPoint: [ 1, 100 ],
        smoothSeconds: false

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

    [Embed(source="hour_hand.png")]
    private var hourHand__image :Class;

    [Embed(source="minute_hand.png")]
    private var minuteHand__image :Class;

    [Embed(source="second_hand.png")]
    private var secondHand__image :Class;

//    [Embed(source="Clock.swf", mimeType="application/octet-stream")]
//    private var tits__bytes :Class;
}
}
