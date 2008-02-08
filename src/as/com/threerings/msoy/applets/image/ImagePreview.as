//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.LoaderInfo;

import flash.events.Event;

import flash.utils.ByteArray;

import mx.controls.Image;

import mx.containers.VBox;

import mx.core.FlexLoader;

import com.adobe.images.JPGEncoder;
import com.adobe.images.PNGEncoder;

/**
 * Displays an image. I think this could be the base class for our in-line image editing.
 */
public class ImagePreview extends VBox
{
    public function ImagePreview ()
    {
        _image = new Image();
        addChild(_image);
    }

    public function setBitmap (bitmapData :BitmapData) :void
    {
        _bytes = null;
        _image.source = new Bitmap(bitmapData);
    }

    public function setImage (bytes :ByteArray) :void
    {
        _bytes = bytes;
        if (bytes == null) {
            _image.source  = null;

        } else {
            // TODO: maybe subclass the Image control and make it able to load bytes?
            // There is a Base64Image in flexlib, but it a) does things wrong, and
            // b) should accept a ByteArray. Base64 decoding is a fucking separate operation, guys.
            var l :FlexLoader = new FlexLoader();
            // TODO: errors?
            l.contentLoaderInfo.addEventListener(Event.COMPLETE, handleFlexLoaderComplete);
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

    /**
     */
    protected function handleFlexLoaderComplete (event :Event) :void
    {
        _image.source = LoaderInfo(event.target).loader;
    }

    /** The control displaying the image, for now. */
    protected var _image :Image;

    /** The raw bytes of the image we're showing. */
    protected var _bytes :ByteArray;
}
}
