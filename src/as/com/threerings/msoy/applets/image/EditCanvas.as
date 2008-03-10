//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.PixelSnapping;
import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Rectangle;

import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import flash.utils.ByteArray;

import mx.core.UIComponent;

import com.adobe.images.JPGEncoder; 
import com.adobe.images.PNGEncoder;

import com.threerings.util.ValueEvent;

/** 
 * Dispatched when the size of the image is known.
 */
[Event(name="SizeKnown", type="com.threerings.util.ValueEvent")]

/**
 * Allows primitive editing of an image.
 */
public class EditCanvas extends UIComponent
{
    public static const SIZE_KNOWN :String = "SizeKnown";

    public function EditCanvas ()
    {
        var editor :Sprite = new Sprite();

        editor.addChild(_backgroundLayer = new Sprite());
        editor.addChild(_imageLayer = new Sprite());
        editor.addChild(_hudLayer = new Sprite());

        addChild(editor);
    }

    /**
     * Clear any currently displayed image.
     */
    public function clearImage () :void
    {
        _bytes = null;
        _bitmapData = null;
        _width = 0;
        _height = 0;
        _imageLayer.graphics.clear();
        _hudLayer.graphics.clear();
        if (_image != null) {
            _imageLayer.removeChild(_image);
            if (_image is Loader) {
                var loader :Loader = _image as Loader;
                try {
                    loader.close();
                } catch (err :Error) {
                }
                loader.unload();
            }
            _image = null;
        }

        this.width = 0;
        this.height = 0;
    }

    /**
     * Set the image to display.
     *
     * @param image may be a Bitmap, BitmapData, ByteArray, Class, URL (string), URLRequest
     */
    public function setImage (image :Object) :void
    {
        clearImage();
        if (image == null) {
            return;
        }

        if (image is String) {
            image = new URLRequest(image as String);
        } else if (image is Class) {
            image = new (image as Class)();
        }
        // no else here
        if (image is BitmapData) {
            // TODO: explore PixelSnapping options
            image = new Bitmap(image as BitmapData, PixelSnapping.ALWAYS, true);
        }
        if (image is Bitmap) {
            var bmp :BitmapData = (image as Bitmap).bitmapData;
            if (bmp != null) {
                sizeKnown(bmp.width, bmp.height);
            }

        } else if ((image is URLRequest) || (image is ByteArray)) {
            var notBytes :Boolean = (image is URLRequest);
            var loader :Loader = new Loader();
            loader.contentLoaderInfo.addEventListener(Event.COMPLETE, handleImageLoadComplete);
            // TODO: error listeners
            var lc :LoaderContext = new LoaderContext(notBytes, new ApplicationDomain(null));
            if (notBytes) {
                loader.load(image as URLRequest, lc);
            } else {
                _bytes = image as ByteArray;
                loader.loadBytes(_bytes, lc);
            }
            image = loader;
        }
        if (image is DisplayObject) {
            _image = image as DisplayObject;
            _imageLayer.addChild(_image);
        } else {
            throw new Error("Unknown image source: " + image);
        }
    }

    /**
     * Get the image back out of the editor.
     */
    public function getImage (asJpg :Boolean = false, quality :Number = 50) :ByteArray
    {
        // see if we can skip re-encoding
        if (_bytes != null && _crop == null) {
            return _bytes;
        }

        // TODO: cropping!

        var bmp :BitmapData = _bitmapData;
        if (bmp == null) {
            // screenshot the image
            bmp = new BitmapData(_width, _height);
            bmp.draw(_imageLayer);
        }

        if (asJpg) {
            return (new JPGEncoder(quality)).encode(bmp);
        } else {
            return PNGEncoder.encode(bmp);
        }
    }

    protected function sizeKnown (width :Number, height :Number) :void
    {
        _width = width;
        _height = height;

        this.width = width;
        this.height = height;

        dispatchEvent(new ValueEvent(SIZE_KNOWN, [ _width, _height ]));
    }

    protected function handleImageLoadComplete (event :Event) :void
    {
        var li :LoaderInfo = event.target as LoaderInfo;
        sizeKnown(li.width, li.height);
    }

    protected var _backgroundLayer :Sprite;

    protected var _imageLayer :Sprite;

    protected var _hudLayer :Sprite;

    protected var _bitmapData :BitmapData;

    protected var _crop :Rectangle;

    protected var _bytes :ByteArray;

    protected var _image :DisplayObject;

    protected var _width :int;

    protected var _height :int;
}
}
