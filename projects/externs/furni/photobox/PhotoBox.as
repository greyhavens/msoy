//
// $Id$

package {

import flash.display.Loader;
import flash.display.Sprite;

import flash.text.TextField;
import flash.text.TextFieldType;

import flash.events.Event;
import flash.events.KeyboardEvent;

import flash.ui.Keyboard;

import com.adobe.webapis.flickr.FlickrService;
import com.adobe.webapis.flickr.PagedPhotoList;
import com.adobe.webapis.flickr.Photo;
import com.adobe.webapis.flickr.PhotoUrl;
import com.adobe.webapis.flickr.events.FlickrResultEvent;

// TODO: This doesn't presently work because youtube has changed their
// crossdomain.xml file to prevent the youtube flash API from working at all.
//
[SWF(width="500", height="550")]
public class PhotoBox extends Sprite
{
    public function PhotoBox ()
    {
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        // this is my (Ray Greenwell)'s personal Flickr key!!
        _flickr = new FlickrService("7aa4cc43b7fd51f0f118b0022b7ab13e")

        _flickr.addEventListener(FlickrResultEvent.PHOTOS_SEARCH,
            handlePhotoSearch);

        _flickr.addEventListener(FlickrResultEvent.PHOTOS_GET_INFO,
            handlePhotoInfo);

        _tagField = new TextField();
        _tagField.type = TextFieldType.INPUT;
        addChild(_tagField);
        _tagField.addEventListener(KeyboardEvent.KEY_DOWN, handleKey);

        _loader = new Loader();
        _loader.y = 50;
        addChild(_loader);
    }

    protected function handlePhotoSearch (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure searching for photos!");
            trace("error: " + evt.data.error.errorCode);
            trace("error: " + evt.data.error.errorMessage);
            return;
        }
        _photos = (evt.data.photos as PagedPhotoList).photos;

        loadNextPhoto();
    }

    protected function loadNextPhoto () :void
    {
        var photo :Photo = (_photos.shift() as Photo);
        _flickr.photos.getInfo(photo.id, photo.secret);
    }

    protected function handlePhotoInfo (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure getting photo info");
            trace("error: " + evt.data.error.errorCode);
            trace("error: " + evt.data.error.errorMessage);
            return;
        }
        var photo :Photo = (evt.data.photo as Photo);

        trace("photo.urls: " + photo.urls);
        for each (var url :PhotoUrl in photo.urls) {
            trace("url(" + url.type + "): " + url.url);
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

    /**
     * Take care of releasing resources when we unload.
     */
    protected function handleUnload (event :Event) :void
    {
    }

    protected var _tagField :TextField;

    protected var _flickr :FlickrService;

    protected var _loader :Loader;

    protected var _photos :Array;
}
}
