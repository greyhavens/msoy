//
// $Id$

package {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Sprite;

import flash.text.TextField;
import flash.text.TextFieldType;

import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
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

import com.whirled.ControlEvent;
import com.whirled.FurniControl;

[SWF(width="500", height="550")]
public class PhotoBox extends Sprite
{
    public function PhotoBox ()
    {
        // configure our conrols
        _furni = new FurniControl(this);
        _furni.addEventListener(ControlEvent.MESSAGE_RECEIVED, handleMessage);

        // this is my (Ray Greenwell)'s personal Flickr key!!
        _flickr = new FlickrService("7aa4cc43b7fd51f0f118b0022b7ab13e")
        _flickr.addEventListener(FlickrResultEvent.PHOTOS_SEARCH,
            handlePhotoSearch);
        _flickr.addEventListener(FlickrResultEvent.PHOTOS_GET_SIZES,
            handlePhotoSizes);

        _sendTimer = new Timer(7000, 1); // 7 seconds, fire once
        _sendTimer.addEventListener(TimerEvent.TIMER, queuePhotoForDisplay);
        _displayTimer = new Timer(7000, 1);
        _displayTimer.addEventListener(TimerEvent.TIMER, showNextPhoto);

        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload);

        configureUI();
    }

    /**
     * Configure the UI. Called from the constructor.
     */
    protected function configureUI () :void
    {
        var logo :DisplayObject = DisplayObject(new LOGO());
        addChild(logo);

        var prompt :TextField = new TextField();
        prompt.autoSize = TextFieldAutoSize.LEFT;
        prompt.background = true;
        prompt.backgroundColor = 0xFFFFFF;
        var format :TextFormat = new TextFormat();
        format.size = 16;
        format.bold = true;
        prompt.defaultTextFormat = format;
        prompt.text = "Enter tags:";
        prompt.y = logo.height;
        prompt.autoSize = TextFieldAutoSize.NONE;
        prompt.width = Math.max(prompt.width, logo.width);
        addChild(prompt);

        _tagEntry = new TextField();
        _tagEntry.type = TextFieldType.INPUT;
        _tagEntry.background = true;
        _tagEntry.backgroundColor = 0xCCFFFF;
        _tagEntry.x = Math.max(prompt.width, logo.width);
        _tagEntry.height = prompt.height + logo.height;
        _tagEntry.width = 500 - _tagEntry.x;
        addChild(_tagEntry);
        _tagEntry.addEventListener(KeyboardEvent.KEY_DOWN, handleKey);
//        _tagEntry.addEventListener(FocusEvent.FOCUS_IN, handleTagFocus);
//        _tagEntry.addEventListener(FocusEvent.FOCUS_OUT, handleTagFocus);
        format = new TextFormat();
        format.size = 36;
        _tagEntry.defaultTextFormat = format;

        _loader = new Loader();
        _loader.mouseEnabled = true;
        _loader.mouseChildren = true;
        _loader.addEventListener(MouseEvent.CLICK, handleClick);
        _loader.addEventListener(MouseEvent.ROLL_OVER, handleMouseRoll);
        _loader.addEventListener(MouseEvent.ROLL_OUT, handleMouseRoll);
        _loader.y = 50;
        addChild(_loader);

        _overlay = new Sprite();
        _overlay.y = _loader.y;
        addChild(_overlay);
    }

    /**
     * Handle the results of a tag search.
     */
    protected function handlePhotoSearch (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure searching for photos " +
                "[" + evt.data.error.errorMessage + "]");
            return;
        }

        _photos = (evt.data.photos as PagedPhotoList).photos;
        queuePhotoForDisplay();
    }

    /**
     * Load the next photo in the photo list maintained by this
     * photobox.
     */
    protected function queuePhotoForDisplay (... ignored) :void
    {
        if (_photos == null || _photos.length == 0) {
            _photos = null;
            return;
        }

        var photo :Photo = (_photos.shift() as Photo);
        _pageURL = "http://www.flickr.com/photos/" + photo.ownerId + "/" + 
            photo.id;
        _flickr.photos.getSizes(photo.id);
    }

    /**
     * Handle data arriving as a result of a getSizes() request.
     */
    protected function handlePhotoSizes (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure getting photo sizes " +
                "[" + evt.data.error.errorMessage + "]");
            return;
        }

        var sizes :Array = (evt.data.photoSizes as Array);
        var url :String = getMediumPhotoSource(sizes);
        if (url != null) {
            var args :Array = [ url, _pageURL ];
            if (_furni.isConnected()) {
                _furni.sendMessage("photo", args);

            } else {
                queuePhotoDisplay(args);
            }
        }

        // if we have more photos to send, queue up the send for 7 seconds
        // from now.
        if (_photos != null) {
            _sendTimer.reset();
            // when there are photos queued, throttle back the send timer
            // unfortunately this favors new instances, which won't
            // throttle as quickly because the other throttling instances
            // won't be filling their queue as quickly
            _sendTimer.delay = 7 * Math.max(1, _displayPhotos.length);
            _sendTimer.start();
        }
    }

//    /**
//     * Handle focus changes to our tag entry area.
//     */
//    protected function handleTagFocus (event :FocusEvent) :void
//    {
//        if (event.type == FocusEvent.FOCUS_IN) {
//            _tagEntry.backgroundColor = 0xFFFFFF;
//            _tagEntry.text = "";
////            _hasFocus = true;
//
//        } else {
//            _tagEntry.backgroundColor = 0xCCFFFF;
//            _tagEntry.text = _displayedTags;
////            _hasFocus = false;
//        }
//    }

    /**
     * Handle a user-generated keypress.
     */
    protected function handleKey (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.ENTER) {
            var tags :String = _tagEntry.text;
            tags = tags.replace(/\s+/g, ","); // replace spaces with commas
            tags = tags.replace(/,+/g, ","); // prune consecutive commas
            tags = tags.replace(/^,/, ""); // remove spurious comma at start
            tags = tags.replace(/,$/, ""); // remove spurious comma at end
//            _enteredTags = tags;
//            _displayedTags = tags;
//            _shownOwn = false;
            _flickr.photos.search("", tags, "all");

            // unfocus the tag entry area
            // (This seems to work even when we're in a security boundary)
            stage.focus = null; // will trigger unfocus event
        }
    }

    /**
     * Handle a message event from other instances of this photobox
     * running on other clients.
     */
    protected function handleMessage (event :ControlEvent) :void
    {
        if (event.name == "photo") {
            var args :Array  = (event.value as Array);
            queuePhotoDisplay(args);
        }
    }
//
//            var newId :int = int(args[0]);
//            var url :String = String(args[1]);
//            var tags :String = String(args[2]);
//            if (newId > _controlId) {
//                _controlId = newId;
//                _photos = null; // kill our own display of photos
//
//            } else if (newId == _controlId) {
//                if (tags == _enteredTags) {
//                    _shownOwn = true;
//
//                } else if (!_shownOwn) {
//                    // sorry charlie, we were second to try this controlId
//                    _photos = null;
//                }
//            }
//
//            if (_photos == null) {
//                displayPhoto(url);
//                _displayedTags = tags;
//                if (!_hasFocus) {
//                    _tagEntry.text = tags;
//                }
//
//            } else {
//                // else, ignore our own events
//                //trace("ignoring " + url);
//            }
//        }
//    }

    /**
     * Display the photo at the specified url.
     */
    protected function queuePhotoDisplay (args :Array) :void
    {
        _displayPhotos.push(args);

        if (!_displayTimer.running) {
            showNextPhoto();
        }
    }

    /**
     * Handle the timer expiring.
     */
    protected function showNextPhoto (... ignored) :void
    {
        if (_displayPhotos.length == 0) {
            return;
        }

        // show the photo!
        clearLoader();
        var nextPhoto :Array = (_displayPhotos.shift() as Array);
        var url :String = String(nextPhoto[0]);
        _displayPageURL = String(nextPhoto[1]);
        _loader.load(new URLRequest(url));

        // queue a timer for the next one
        _displayTimer.reset();
        _displayTimer.start();
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

    /**
     * Clear any resources from the loader and prepare it to load
     * another photo, or be unloaded.
     */
    protected function clearLoader () :void
    {
        try {
            _loader.close();
        } catch (e :Error) {
            // nada
        }
        _loader.unload();
        _displayPageURL = null;
        handleMouseRoll(null);
    }

    /**
     * Handle a click.
     */
    protected function handleClick (event :MouseEvent) :void
    {
        if (_displayPageURL == null) {
            return;
        }
        try {
            flash.net.navigateToURL(new URLRequest(_displayPageURL));
        } catch (err :Error) {
            trace("Oh my gosh: " + err);
        }
    }

    protected function handleMouseRoll (event :MouseEvent) :void
    {
        var draw :Boolean = (event == null || event.type == MouseEvent.ROLL_OVER) &&
            (_displayPageURL != null);

        with (_overlay.graphics) {
            clear();
            if (draw) {
                lineStyle(1, 0xFF4040);
                drawRect(0, 0, _loader.width - 1, _loader.height - 1);
            }
        }
    }

    /**
     * Take care of releasing resources when we unload.
     */
    protected function handleUnload (event :Event) :void
    {
        _sendTimer.stop();
        _displayTimer.stop();
        clearLoader();
    }

    /** The interface through which we make flickr API requests. */
    protected var _flickr :FlickrService;

    /** The interface through which we communicate with metasoy. */
    protected var _furni :FurniControl;

    /** Handles the countdown to dispatching the next photo. */
    protected var _sendTimer :Timer;

    /** Handles the countdown to showing the next photo. */
    protected var _displayTimer :Timer;

    /** The text entry area for tags. */
    protected var _tagEntry :TextField;

    /** The url of the photo page for the photo we're currently doing
     * a size lookup upon. */
    protected var _pageURL :String;

    /** The page url for the photo we're currently showing. */
    protected var _displayPageURL :String;

    /** A sprite drawn on top of everything, for use in drawing UI. */
    protected var _overlay :Sprite;

//    protected var _hasFocus :Boolean;
//
//    protected var _displayedTags :String = "";
//
//    protected var _enteredTags :String;
//
//    /** Whether or not we've shown one of our own at the current controlId. */
//    protected var _shownOwn :Boolean;

    /** Loads up photos for display. */
    protected var _loader :Loader;

    /** If this instance was used to do a tag search, this contains the
     * resultant photos which are queued up for display on other instances.  */
    protected var _photos :Array;

    /** The photos to display. */
    protected var _displayPhotos :Array = [];

    [Embed(source="flickr_logo.gif")]
    protected const LOGO :Class;
}
}
