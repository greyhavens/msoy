//
// $Id$

package {

import flash.display.Loader;
import flash.display.Sprite;

import flash.text.TextField;
import flash.text.TextFieldType;

import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.TimerEvent;

import flash.net.URLRequest;

import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import flash.ui.Keyboard;

import flash.utils.Timer;

import com.adobe.webapis.flickr.FlickrService;
import com.adobe.webapis.flickr.PagedPhotoList;
import com.adobe.webapis.flickr.Photo;
import com.adobe.webapis.flickr.PhotoSize;
import com.adobe.webapis.flickr.PhotoUrl;
import com.adobe.webapis.flickr.events.FlickrResultEvent;

import com.threerings.msoy.export.FurniControl;

// TODO: This doesn't presently work because youtube has changed their
// crossdomain.xml file to prevent the youtube flash API from working at all.
//
[SWF(width="500", height="550")]
public class PhotoBox extends Sprite
{
    public function PhotoBox ()
    {
        // configure our conrols
        _furni = new FurniControl(this);
        _furni.eventTriggered = handleMsoyEvent;

        // this is my (Ray Greenwell)'s personal Flickr key!!
        _flickr = new FlickrService("7aa4cc43b7fd51f0f118b0022b7ab13e")
        _flickr.addEventListener(FlickrResultEvent.PHOTOS_SEARCH,
            handlePhotoSearch);
        _flickr.addEventListener(FlickrResultEvent.PHOTOS_GET_SIZES,
            handlePhotoSizes);
//        _flickr.addEventListener(FlickrResultEvent.PHOTOS_GET_INFO,
//            handlePhotoInfo);

        _timer = new Timer(7000, 1); // 7 seconds, fire once
        _timer.addEventListener(TimerEvent.TIMER, handleTimer);

        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        // set up the UI
        var prompt :TextField = new TextField();
        prompt.autoSize = TextFieldAutoSize.LEFT;
        prompt.background = true;
        prompt.backgroundColor = 0xCCFFFF;
        prompt.wordWrap = true;
        prompt.width = 175;
        var format :TextFormat = new TextFormat();
        format.size = 16;
        format.bold = true;
        prompt.defaultTextFormat = format;
        prompt.text = "Enter Flickr tags\nseparated by commas:";
        addChild(prompt);

        _tagField = new TextField();
        _tagField.type = TextFieldType.INPUT;
        _tagField.background = true;
        _tagField.backgroundColor = 0xFFFFFF;
        _tagField.x = prompt.textWidth + 15;
        _tagField.height = prompt.height;
        _tagField.width = 500 - _tagField.x;
        addChild(_tagField);
        _tagField.addEventListener(KeyboardEvent.KEY_DOWN, handleKey);
        format = new TextFormat();
        format.size = 36;
        _tagField.defaultTextFormat = format;

        _loader = new Loader();
        _loader.y = 50;
        addChild(_loader);
    }

    protected function handlePhotoSearch (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure searching for photos " +
                "[" + evt.data.error.errorMessage + "]");
            return;
        }

        _photos = (evt.data.photos as PagedPhotoList).photos;
        loadNextPhoto();
    }

    protected function loadNextPhoto () :void
    {
        if (_photos == null || _photos.length == 0) {
            _photos = null;
            return;
        }

        var photo :Photo = (_photos.shift() as Photo);
        //_flickr.photos.getInfo(photo.id, photo.secret);
        _flickr.photos.getSizes(photo.id);
    }

//    protected function handlePhotoInfo (evt :FlickrResultEvent) :void
//    {
//        if (!evt.success) {
//            trace("Failure getting photo info " +
//                "[" + evt.data.error.errorMessage + "]");
//            return;
//        }
//        var photo :Photo = (evt.data.photo as Photo);
//
//        trace("photo.urls: " + photo.urls);
//        for each (var url :PhotoUrl in photo.urls) {
//            trace("url(" + url.type + "): " + url.url);
//        }
//    }

    protected function handlePhotoSizes (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure getting photo sizes " +
                "[" + evt.data.error.errorMessage + "]");
            return;
        }
        _controlId++;

        var sizes :Array = (evt.data.photoSizes as Array);
        var url :String = getMediumPhotoSource(sizes);
        if (url != null) {
            displayPhoto(url);
            // always broadcast if we can
            if (_furni.isConnected()) {
                _furni.triggerEvent("show", [ _controlId, url ]);
            }
        }

        // if our photos aren't null, restart the timer
        if (_photos != null) {
            _timer.reset();
            _timer.start();
        }
    }

    protected function handleKey (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.ENTER) {
            var tags :String = _tagField.text;
            _tagField.text = "";
            _flickr.photos.search("", tags);
        }
    }

    protected function handleTimer (event :TimerEvent) :void
    {
        loadNextPhoto();
    }

    protected function handleMsoyEvent (event :String, arg :Object) :void
    {
        if (event == "show") {
            var args :Array  = (arg as Array);
            var newId :int = int(args[0]);
            var url :String = String(args[1]);

            if (newId > _controlId) {
                _controlId = newId;
                _photos = null; // kill our own display of photos
            }

            if (_photos == null) {
                displayPhoto(url);

            } else {
                // else, ignore our own events
                //trace("ignoring " + url);
            }
        }
    }

    /**
     * Take care of releasing resources when we unload.
     */
    protected function handleUnload (event :Event) :void
    {
        _timer.stop();
    }

    /**
     * Given an array of PhotoSize objects, return the source url
     * for the medium size photo.
     */
    protected function getMediumPhotoSource (sizes :Array) :String
    {
        for each (var p :PhotoSize in sizes) {
            if (p.label == "Medium") {
                return p.source;
            }
        }

        return null;
    }

    protected function displayPhoto (url :String) :void
    {
        _loader.load(new URLRequest(url));
    }

    protected var _flickr :FlickrService;

    protected var _furni :FurniControl;

    protected var _timer :Timer;

    protected var _tagField :TextField;

    protected var _loader :Loader;

    protected var _photos :Array;

    /** The 'control id' which determines which piece of furni is in control. */
    protected var _controlId :int = 0;
}
}
