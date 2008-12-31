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

import flash.events.IOErrorEvent;
import flash.events.ErrorEvent;
import flash.events.Event;

import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import flash.utils.ByteArray;

import mx.core.UIComponent;

import mx.containers.Canvas;

import com.threerings.util.ValueEvent;

import com.threerings.flash.LoaderUtil;

/**
 * Dispatched when the size of the image is known.
 */
[Event(name="SizeKnown", type="com.threerings.util.ValueEvent")]

public class DisplayCanvas extends Canvas
{
    public static const SIZE_KNOWN :String = "SizeKnown";

    public function DisplayCanvas (maxW :int, maxH :int)
    {
        this.maxWidth = maxW;
        this.maxHeight = maxH;

        _baseLayer = new Sprite();

        _holder = new ImageHolder(_baseLayer);
        var ho :UIComponent = new UIComponent();
        ho.addChild(_holder.background);
        ho.includeInLayout = false;
        addChild(ho);
        addChild(_holder);
    }

    /**
     * Clear any currently displayed image.
     */
    public function clearImage () :void
    {
        _bytes = null;
        _bitmapData = null;
        _imgWidth = 0;
        _imgHeight = 0;

        if (_image != null) {
            getImageLayer().removeChild(_image);
            if (_image is Loader) {
                LoaderUtil.unload(Loader(_image));
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
        if ((image is URLRequest) || (image is ByteArray)) {
            var notBytes :Boolean = (image is URLRequest);
            var loader :Loader = new Loader();
            loader.contentLoaderInfo.addEventListener(Event.COMPLETE, handleImageLoadComplete);
            loader.contentLoaderInfo.addEventListener(IOErrorEvent.IO_ERROR, handleImageLoadError);
            var lc :LoaderContext = new LoaderContext(notBytes, new ApplicationDomain(null));
            if (notBytes) {
                loader.load(image as URLRequest, lc);
            } else {
                _bytes = image as ByteArray;
                loader.loadBytes(_bytes, lc);
            }
            image = loader;
        }

        // by now, we should have transformed it into a DisplayObject of some sort
        if (!(image is DisplayObject)) {
            throw new Error("Unknown image source: " + image);
        }

        // set up the image
        _image = image as DisplayObject;
        getImageLayer().addChildAt(_image, 0);

        // see if we can report the size now (otherwise, we'll know when the loader completes)
        if (image is Bitmap) {
            var bmp :BitmapData = (image as Bitmap).bitmapData;
            if (bmp != null) {
                imageSizeKnown(bmp.width, bmp.height);
            }
        }
    }

    protected function handleImageLoadError (event :ErrorEvent) :void
    {
        // nada for now.
        // TODO: generate some event so that if the remixer was expecting an image, we know
        // it didn't happen..
    }

    protected function handleImageLoadComplete (event :Event) :void
    {
        var li :LoaderInfo = event.target as LoaderInfo;
        imageSizeKnown(li.width, li.height);
    }

    protected function imageSizeKnown (width :Number, height :Number) :void
    {
        _imgWidth = width;
        _imgHeight = height;

        updateCanvasSize();

        dispatchEvent(new ValueEvent(SIZE_KNOWN, [ _imgWidth, _imgHeight ]));
    }

    protected function updateCanvasSize () :void
    {
        _holder.width = _imgWidth;
        _holder.height = _imgHeight;

        // FUCK YOU FLEX, YOU GIANT PIECE OF SHIT
        // I set the child's size, now this container should adjust.
        // None of the following works:
//        invalidateSize();
//        invalidateDisplayList();
//        invalidateProperties();
//        validateNow();

        // Oh fucking boise, we can just do this manually.
        this.width = Math.min(this.maxWidth, _imgWidth);
        this.height = Math.min(this.maxHeight, _imgHeight);
    }

    /**
     * Get layer to which we should add the image.
     */
    protected function getImageLayer () :Sprite
    {
        return _baseLayer;
    }

    protected var _baseLayer :Sprite;

    protected var _holder :ImageHolder;

    protected var _bitmapData :BitmapData;
    protected var _bytes :ByteArray;

    protected var _image :DisplayObject;

    protected var _imgWidth :int;
    protected var _imgHeight :int;
}
}

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;

import mx.core.UIComponent;

class ImageHolder extends UIComponent
{
    public function ImageHolder (toBeHeld :DisplayObject)
    {
        // you'd think I'd need these, but using them makes the scrollbars not
        // appear if we're bigger than the canvas. Maybe it's actually making the
        // canvas bigger but its parent doesn't add scrollbars.
        // Of course, I shouldn't even have to fucking size the canvas... see the
        // note above in updateCanvasSize
//        setStyle("left", 0);
//        setStyle("top", 0);
//        setStyle("right", 0);
//        setStyle("bottom", 0);

        _background = new Shape();
        addChild(toBeHeld);

        _mask = new Shape();
        this.mask = _mask;
        addChild(_mask);
    }

    public function get background () :DisplayObject
    {
        return _background;
    }

    public function setZoom (zoom :Number) :void
    {
        scaleX = zoom;
        scaleY = zoom;
        invalidateSize();
    }

    public function setDarkBackground (dark :Boolean) :void
    {
        _darkBackground = dark;
        setActualSize(width, height); // refresh
    }

    override public function setActualSize (w :Number, h :Number) :void
    {
        super.setActualSize(w, h);

        // update our size in the mask
        var g :Graphics = _mask.graphics;
        g.clear();
        g.beginFill(0xFFFFFF);
        g.drawRect(0, 0, w / scaleX, h / scaleY);
        g.endFill();

        var darkColor :uint = _darkBackground ? 0x333333 : 0x999999;
        var lightColor :uint = _darkBackground ? 0x666666 : 0xCCCCCC;

        // draw the checkerboard on the background, which isn't even
        // technically our child
        g = _background.graphics;
        g.clear();
        var dark :Boolean;
        var darkY :Boolean = true;
        const GRID_SIZE :Number = 10 * scaleX;
        for (var yy :Number = 0; yy < h; yy += GRID_SIZE) {
            dark = darkY;
            darkY = !darkY;
            for (var xx :Number = 0; xx < w; xx += GRID_SIZE) {
                g.beginFill(dark ? darkColor : lightColor);
                g.drawRect(xx, yy, Math.min(GRID_SIZE, w - xx), Math.min(GRID_SIZE, h - yy));
                g.endFill();
                dark = !dark;
            }
        }
    }

    protected var _background :Shape;

    protected var _mask :Shape;

    protected var _darkBackground :Boolean;
}
