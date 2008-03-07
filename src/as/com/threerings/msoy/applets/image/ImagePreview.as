//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Graphics;
import flash.display.LoaderInfo;
import flash.display.Sprite;

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;

import flash.utils.ByteArray;

import mx.controls.Image;

import mx.containers.HBox;
import mx.containers.VBox;

import mx.core.FlexLoader;
import mx.core.ScrollPolicy;

import com.adobe.images.JPGEncoder;
import com.adobe.images.PNGEncoder;

import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.ScrollBox;

/**
 * Dispatched when the size of the image is known.
 */
[Event(name="SizeKnown", type="com.threerings.util.ValueEvent")]

/**
 * Displays an image. I think this could be the base class for our in-line image editing.
 */
public class ImagePreview extends HBox
{
    public static const SIZE_KNOWN :String = "SizeKnown";

    public function ImagePreview (cutWidth :Number = NaN, cutHeight :Number = NaN)
    {
        if (!isNaN(cutWidth) && !isNaN(cutHeight)) {
            // TODO: set up the cutter-outter
        }

        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;

        setStyle("backgroundColor", 0xCCCCCC);
        _imageBox = new HBox();
        _imageBox.maxWidth = 300;
        _imageBox.maxHeight = 300;
        _image = new Image();
        _imageBox.addChild(_image);
        _imageBox.rawChildren.addChild(_overlay = createControlOverlay());
        addChild(_imageBox);
        addChild(_controlBar = createControlBar());
        setImageSource(null);
    }

    public function setBitmap (bitmapData :BitmapData) :void
    {
        _bytes = null;
        setImageSource(new Bitmap(bitmapData));
        dispatchEvent(new ValueEvent(SIZE_KNOWN, [ bitmapData.width, bitmapData.height ]));
    }

    public function setImage (bytes :ByteArray) :void
    {
        _bytes = bytes;
        if (bytes == null) {
            setImageSource(null);

        } else {
            // TODO: maybe subclass the Image control and make it able to load bytes?
            // There is a Base64Image in flexlib, but it a) does things wrong, and
            // b) should accept a ByteArray. Base64 decoding is a fucking separate operation, guys.
            var l :FlexLoader = new FlexLoader();
            l.contentLoaderInfo.addEventListener(Event.COMPLETE, handleFlexLoaderComplete);
            l.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, handleFlexLoaderError);
            l.loadBytes(bytes);
        }
    }

    /**
     * Get the currently selected portion of the Image, as a ByteArray.
     *
     * @param asJpg if true, encode the image as a JPG if not already encoded.
     * @param quality (only applicable if asJpg is true) the quality setting for jpg encoding.
     */
    public function getImage (asJpg :Boolean = false, quality :Number = 50) :ByteArray
    {
        if (_bytes != null) {
            return _bytes;
        }

        var src :Object = _image.source;
        if (src is Bitmap) {
            var bmpData :BitmapData = Bitmap(src).bitmapData;
            // TODO: image cropping, etc?

            if (asJpg) {
                return (new JPGEncoder(quality)).encode(bmpData);

            } else {
                return PNGEncoder.encode(bmpData);
            }
        }
        return null;
    }

    protected function handleFlexLoaderError (event :ErrorEvent) :void
    {
        trace("Unhandled error: " + event);
    }

    /**
     */
    protected function handleFlexLoaderComplete (event :Event) :void
    {
        setImageSource(LoaderInfo(event.target).loader);
        dispatchEvent(new ValueEvent(SIZE_KNOWN, [ event.target.width, event.target.height ]));
    }

    protected function setImageSource (source :Object) :void
    {
        _image.source = source;

        var showControls :Boolean = (source != null);
        _controlBar.includeInLayout = showControls;
        _controlBar.visible = showControls;
    }

    protected function createControlOverlay () :Sprite
    {
        var overlay :Sprite = new Sprite();
//        var g :Graphics = overlay.graphics;
//        g.lineStyle(1, 0xFF0000);
//        g.drawRect(0, 0, 100, 100);

        return overlay;
    }

    protected function createControlBar () :VBox
    {
        var bar :VBox = new VBox();
        bar.width = 100;
        bar.horizontalScrollPolicy = ScrollPolicy.OFF;
        bar.verticalScrollPolicy = ScrollPolicy.OFF;

        // TODO: add a scrollbox

        bar.addChild(addMode("select", SELECT));
        bar.addChild(addMode("move", MOVE));

        // TODO add zoom in and out buttons

        return bar;
    }

    protected function addMode (label :String, mode :int) :CommandButton
    {
        var but :CommandButton = new CommandButton(label, setMode, mode);
        but.data = mode;
        but.toggle = true;
        _buttons.push(but);
        return but;
    }

    protected function setMode (mode :int) :void
    {
        for each (var but :CommandButton in _buttons) {
            but.selected = (mode == but.data);
        }
    }

    /** The control displaying the image, for now. */
    protected var _image :Image;

    /** The raw bytes of the image we're showing. */
    protected var _bytes :ByteArray;

    protected var _controlBar :VBox;

    protected var _imageBox :HBox;

    protected var _overlay :Sprite;

    protected var _buttons :Array = [];

    protected static const SELECT :int = 0;
    protected static const MOVE :int = 1;
}
}
