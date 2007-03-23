//
// $Id$

package {

import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.Shape;
import flash.display.SimpleButton;
import flash.display.Sprite;

import flash.filters.ColorMatrixFilter;

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

[SWF(width="500", height="530")]
public class PhotoBox extends Sprite
{
    public function PhotoBox ()
    {
        // be prepared to clean up after ourselves...
        root.loaderInfo.addEventListener(Event.UNLOAD, handleUnload, false, 0, true);

        // configure our conrol
        _furni = new FurniControl(this);
        _furni.addEventListener(ControlEvent.MESSAGE_RECEIVED, handleMessageReceived);
        _furni.addEventListener(ControlEvent.ACTION_TRIGGERED, handleActionTriggered);
        _furni.addEventListener(ControlEvent.GOT_CONTROL, handleGotControl);

        // Set up the flickr service
        // This is my (Ray Greenwell)'s personal Flickr key!!
        _flickr = new FlickrService("7aa4cc43b7fd51f0f118b0022b7ab13e")
        _flickr.addEventListener(FlickrResultEvent.PHOTOS_SEARCH,
            handlePhotoSearchResult);
        _flickr.addEventListener(FlickrResultEvent.PHOTOS_GET_SIZES,
            handlePhotoUrlKnown);

        // try to set up our UI
        var configureNow :Boolean = false;
        try {
            _width = root.loaderInfo.width;
            _height = root.loaderInfo.height;
            configureNow = true; // but do it outside the try/catch...

        } catch (err :Error) {
            // we couldn't access the width yet, wait until we can
            root.loaderInfo.addEventListener(Event.COMPLETE, handleLoaded, false, 0, true);
        }

        if (configureNow) {
            configureUI();
        }
    }

    /**
     * Waits until we're fully loaded to configure our UI.
     */
    protected function handleLoaded (event :Event) :void
    {
        _width = root.loaderInfo.width;
        _height = root.loaderInfo.height;
        configureUI();
    }

    /**
     * Configure the UI. Called from the constructor.
     */
    protected function configureUI () :void
    {
        var bar :DisplayObject = DisplayObject(new BAR());
        bar.width = _width;
        bar.y = (_height - BAR_HEIGHT);
        addChild(bar);

        var logo :DisplayObject = DisplayObject(new LOGO());
        logo.x = PAD;
        logo.y = bar.y + ((BAR_HEIGHT - logo.height) / 2);
        addChild(logo);

        var stop :DisplayObject = DisplayObject(new STOP());
        var downStop :DisplayObject = DisplayObject(new STOP());
        downStop.x = 1;
        downStop.y = 1;

        _stopButton = new SimpleButton(stop, stop, downStop, stop);
        _stopButton.x = logo.width + (PAD * 2);
        _stopButton.y = bar.y + ((BAR_HEIGHT - _stopButton.height) / 2);
        _stopButton.addEventListener(MouseEvent.CLICK, handleStopPressed);
        addChild(_stopButton);

        var tagWidths :int = (_width - (PAD * 5) - logo.width - _stopButton.width) / 2;

        var format :TextFormat = new TextFormat();
        format.size = 16;

        _tagEntry = new TextField();
        _tagEntry.defaultTextFormat = format;
        _tagEntry.text = PROMPT;
        _tagEntry.height = _tagEntry.textHeight + 4;
        _tagEntry.x = _width - PAD - tagWidths;
        _tagEntry.y = logo.y;
        _tagEntry.width = tagWidths;

        _tagDisplay = new TextField();
        _tagDisplay.defaultTextFormat = format;
        _tagDisplay.selectable = false;
        _tagDisplay.height = _tagEntry.height;
        _tagDisplay.x = logo.width + _stopButton.width + (PAD * 3);
        _tagDisplay.y = logo.y;
        _tagDisplay.width = tagWidths;

        var entrySkin :DisplayObject = DisplayObject(new ENTRY_SKIN());
        entrySkin.x = _tagEntry.x;
        entrySkin.y = _tagEntry.y;
        entrySkin.width = tagWidths;
        entrySkin.height = _tagEntry.height;

        var dispSkin :DisplayObject = DisplayObject(new DISPLAY_SKIN());
        dispSkin.x = _tagDisplay.x;
        dispSkin.y = _tagDisplay.y;
        dispSkin.width = tagWidths;
        dispSkin.height = _tagDisplay.height;

        addChild(dispSkin);
        addChild(_tagDisplay);
        addChild(entrySkin);
        addChild(_tagEntry);

        _tagEntry.addEventListener(FocusEvent.FOCUS_IN, handleTagEntryFocus);
        _tagEntry.addEventListener(FocusEvent.FOCUS_OUT, handleTagEntryFocus);
        _tagEntry.addEventListener(KeyboardEvent.KEY_DOWN, handleTagEntryKey);

        _loader = new Loader();
        _loader.mouseEnabled = true;
        _loader.mouseChildren = true;
        _loader.addEventListener(MouseEvent.CLICK, handleClick);
        _loader.addEventListener(MouseEvent.ROLL_OVER, handleMouseRoll);
        _loader.addEventListener(MouseEvent.ROLL_OUT, handleMouseRoll);
        addChild(_loader);

        _overlay = new Sprite();
        addChild(_overlay);

        // configure our 'stopped' status
        updateStopped();

        // request control, or pretend we're it
        if (_furni.isConnected()) {
            _furni.requestControl();

        } else {
            // fake that we got control
            handleGotControl(null);
        }
    }

    /**
     * Handle focus received to our tag entry area.
     */
    protected function handleTagEntryFocus (event :FocusEvent) :void
    {
        if (_readyForNewTags && !_isStopped) {
            if (event.type == FocusEvent.FOCUS_IN) {
                _tagEntry.text = "";

            } else {
                _tagEntry.text = PROMPT;
            }
        }
    }

    /**
     * Handle a user-generated keypress.
     */
    protected function handleTagEntryKey (event :KeyboardEvent) :void
    {
        if (event.keyCode == Keyboard.ENTER) {
            var tags :String = _tagEntry.text;
            tags = tags.replace(/\s+/g, ","); // replace spaces with commas
            tags = tags.replace(/,+/g, ","); // prune consecutive commas
            tags = tags.replace(/^,/, ""); // remove spurious comma at start
            tags = tags.replace(/,$/, ""); // remove spurious comma at end

            // unfocus the tag entry area
            // (This seems to work even when we're in a security boundary)
            stage.focus = null; // will trigger unfocus event

            _ourTags = tags;
            _ourPhotos = null;
            _readyForNewTags = true;
            _flickr.photos.search("", tags, "all");

        } else {
            // the user is entering stuff, clear everything out
            _ourPhotos = null;
            _ourTags = null;
            _readyForNewTags = false;
        }
    }

    /**
     * Handle the results of a tag search.
     */
    protected function handlePhotoSearchResult (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure searching for photos " +
                "[" + evt.data.error.errorMessage + "]");
            return;
        }

        // if the tags have since been cleared, throw away these results
        if (_ourTags == null) {
            return;
        }

        // save the metadata about photos
        _ourPhotos = (evt.data.photos as PagedPhotoList).photos;

        if (!_furni.isConnected()) {
            // if we're not connected, just get the next URL immediatly
            getNextPhotoUrl();
        }
    }

    /**
     * Get the next URL for photos that we ourselves have found via tags.
     */
    protected function getNextPhotoUrl () :void
    {
        if (_ourPhotos == null || _ourPhotos.length == 0) {
            _ourPhotos = null;
            return;
        }

        var photo :Photo = (_ourPhotos.shift() as Photo);
        _ourPageURL = "http://www.flickr.com/photos/" + photo.ownerId + "/" + 
            photo.id;
        _flickr.photos.getSizes(photo.id);
    }

    /**
     * Handle data arriving as a result of a getSizes() request.
     */
    protected function handlePhotoUrlKnown (evt :FlickrResultEvent) :void
    {
        if (!evt.success) {
            trace("Failure getting photo sizes " +
                "[" + evt.data.error.errorMessage + "]");
            return;
        }

        // if either of these are null, the user has started searching
        // on new tags...
        if (_ourTags == null || _ourPhotos == null) {
            return;
        }

        var sizes :Array = (evt.data.photoSizes as Array);
        var p :PhotoSize = getMediumPhotoSource(sizes);
        if (p != null) {
            // yay! We've looked-up our next photo item
            _ourReadyPhoto = [ p.source, p.width, p.height, _ourPageURL, _ourTags ];

            if (_furni.isConnected()) {
                // send a message to the instance in control..
                _furni.sendMessage("queue", _ourReadyPhoto);

            } else {
                // just freaking show it
                showPhoto(_ourReadyPhoto);
            }
        }
    }

    /**
     * Handle a command to show a photo from the entity that's in control.
     */
    protected function handleActionTriggered (event :ControlEvent) :void
    {
        switch (event.name) {
        case "show":
            showPhoto(event.value as Array);
            break;

        case "send_photos":
            // hey, the instance in control wants us to send our goodies!
            if (_ourReadyPhoto != null) {
                _furni.sendMessage("queue", _ourReadyPhoto);

            } else {
                getNextPhotoUrl();
            }
        }
    }

    /**
     * Show the photo specified.
     */
    protected function showPhoto (photo :Array) :void
    {
        clearLoader();
        _showingPhoto = photo;

        // if it's our personal photo, clear it 
        if ((_ourReadyPhoto != null) && (_showingPhoto[0] == _ourReadyPhoto[0])) {
            _ourReadyPhoto = null;
        }

        if (!_isStopped) {
            enactShowPhoto();
        }
    }

    /**
     * Actually show the _showingPhoto. This is like the opposite of clearLoader.
     */
    protected function enactShowPhoto () :void
    {
        if (_showingPhoto == null) {
            return;
        }

        var url :String = String(_showingPhoto[0]);
        var imgWidth :Number = Number(_showingPhoto[1]);
        var imgHeight :Number = Number(_showingPhoto[2]);
        // _showingPhoto[3] is the page url at flickr
        _tagDisplay.text = String(_showingPhoto[4]);

        _loader.x = (_width - imgWidth) / 2;
        _loader.y = (_height - BAR_HEIGHT - imgHeight);
        _overlay.x = _loader.x;
        _overlay.y = _loader.y;
        _loader.load(new URLRequest(url));
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
        _tagDisplay.text = "";
        _showingPhoto = null;
        handleMouseRoll(null);
    }

    /**
     * Update the look of UI elements based on _stopped.
     */
    protected function updateStopped () :void
    {
        if (_isStopped) {
            _stopButton.filters = null;
            _tagEntry.text = "";
            _tagEntry.type = TextFieldType.DYNAMIC;

        } else {
            _stopButton.filters = [ new ColorMatrixFilter([
                1/3, 1/3, 1/3, 0, 0,
                1/3, 1/3, 1/3, 0, 0,
                1/3, 1/3, 1/3, 0, 0,
                0, 0, 0, 1, 0])
            ];
            _tagEntry.type = TextFieldType.INPUT;
        }

        _tagEntry.type = _isStopped ? TextFieldType.DYNAMIC : TextFieldType.INPUT;
        _tagEntry.selectable = !_isStopped;
    }

    /**
     * Given an array of PhotoSize objects, return the source url
     * for the medium size photo.
     */
    protected function getMediumPhotoSource (sizes :Array) :PhotoSize
    {
        for each (var p :PhotoSize in sizes) {
            if (p.label == "Medium") {
                return p;
            }
        }

        return null;
    }

    /**
     * Handle a press of the stop button.
     */
    protected function handleStopPressed (event :MouseEvent) :void
    {
        _isStopped = !_isStopped;
        updateStopped();

        if (_isStopped) {
            clearLoader();
        } else {
            enactShowPhoto();
        }

        event.updateAfterEvent();
    }

    /**
     * Handle a click on the Loader.
     */
    protected function handleClick (event :MouseEvent) :void
    {
        if (_showingPhoto == null) {
            return;
        }
        try {
            flash.net.navigateToURL(new URLRequest(String(_showingPhoto[3])));
        } catch (err :Error) {
            trace("Oh my gosh: " + err);
        }
    }

    protected function handleMouseRoll (event :MouseEvent) :void
    {
        var draw :Boolean = (event == null || event.type == MouseEvent.ROLL_OVER) &&
            (_showingPhoto != null);

        with (_overlay.graphics) {
            clear();
            if (draw) {
                lineStyle(1, 0xFF4040);
                drawRect(0, 0, _loader.width, _loader.height);
            }
        }
    }

    /**
     * Take care of releasing resources when we unload.
     */
    protected function handleUnload (event :Event) :void
    {
        if (_ctrlTimer != null) {
            _ctrlTimer.stop();
            _ctrlTimer = null;
        }
        clearLoader();
    }

    // ============ Methods only used on the instance in "control"

    protected function handleGotControl (event :ControlEvent) :void
    {
        // set up the control timer, only used by the one in control...
        _ctrlTimer = new Timer(7000); // 7 seconds
        _ctrlTimer.addEventListener(TimerEvent.TIMER, handleCtrlTimer);
        _ctrlTimer.start();

        handleCtrlTimer(); // kick things off
    }

    protected function handleCtrlTimer (event :TimerEvent = null) :void
    {
        // if we're not even connected
        // call this by hand..
        if (!_furni.isConnected()) {
            getNextPhotoUrl();
            return;
        }

        if (_displayPhotos == null || _displayPhotos.length == 0) {
            // send out a message to all other boxes that we're
            // ready for their next photo
            _displayPhotos = null;
            _furni.triggerAction("send_photos");
            return;
        }

        // otherwise, trigger an action to show the next photo
        var nextPhoto :Array = (_displayPhotos.shift() as Array);
        _furni.triggerAction("show", nextPhoto)
    }

    /**
     * Handle a message event from other instances of this photobox
     * running on other clients.
     */
    protected function handleMessageReceived (event :ControlEvent) :void
    {
        if (!_furni.hasControl()) {
            // ignore messages from the ones not in control.
            return;
        }

        if (event.name == "queue") {
            var photoInfo :Array  = (event.value as Array);

            if (_displayPhotos == null) {
                // show it immediately, create the array as a marker
                // to not show the next one immediately.
                _displayPhotos = [];
                _furni.triggerAction("show", photoInfo);

                // reset the timer so that the next photo is 7 seconds away
                _ctrlTimer.reset();
                _ctrlTimer.start();

            } else {
                // we'll save that for later
                _displayPhotos.push(photoInfo);
            }
        }
    }

    /** The interface through which we communicate with metasoy. */
    protected var _furni :FurniControl;

    /** The interface through which we make flickr API requests. */
    protected var _flickr :FlickrService;

    /** The little 'stop' button. */
    protected var _stopButton :SimpleButton;

    /** If true, we're stopped and shouldn't show photos. */
    protected var _isStopped :Boolean;

    /** The text area to display tags. */
    protected var _tagDisplay :TextField;

    /** The text entry area for tags. */
    protected var _tagEntry :TextField;

    /** Loads up photos for display. */
    protected var _loader :Loader;

    /** A sprite drawn on top of everything, for use in drawing UI. */
    protected var _overlay :Sprite;

    /** Are we ready for new tags? */
    protected var _readyForNewTags :Boolean = true;

    /** The tags we've entered, associated with ourPhotos. */
    protected var _ourTags :String;

    /** The high-level metadata for the result set of photos from our
     * tag search. */
    protected var _ourPhotos :Array;

    /** The url of the photo page for the photo we're currently doing
     * a size lookup upon. */
    protected var _ourPageURL :String;

    /** The full data for the next photo we'd like to show. */
    protected var _ourReadyPhoto :Array;

    /** The photo we're currently showing. */
    protected var _showingPhoto :Array;

    /** Our actual width/height, determined at runtime. */
    protected var _width :int;
    protected var _height :int;

    //=========================

    /** Timer used by the instance in control to coordinate the others. */
    protected var _ctrlTimer :Timer;

    /** The photos to display. */
    protected var _displayPhotos :Array;

    protected static const PROMPT :String = "<Click to enter tags>";

    [Embed(source="flickr_logo_small.png")]
    protected const LOGO :Class;

    [Embed(source="skins.swf#background")]
    protected const BAR :Class;

    [Embed(source="stop.png")]
    protected const STOP :Class;

    [Embed(source="skins.swf#textbox")]
    protected const ENTRY_SKIN :Class;

    [Embed(source="skins.swf#plaque")]
    protected const DISPLAY_SKIN :Class;

    protected static const BAR_HEIGHT :int = 30;

    /** The amount of padding between elements on the bar. */
    protected static const PAD :int = 10;
}
}
