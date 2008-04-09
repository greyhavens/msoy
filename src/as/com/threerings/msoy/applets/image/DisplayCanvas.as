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

import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import flash.utils.ByteArray;

import mx.core.UIComponent;

import mx.containers.Canvas;
import mx.containers.VBox;

import com.threerings.util.ValueEvent;

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
                imageSizeKnown(bmp.width, bmp.height);
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
            getImageLayer().addChildAt(_image, 0);
        } else {
            throw new Error("Unknown image source: " + image);
        }
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

import com.threerings.util.Log;

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

    override public function setActualSize (w :Number, h :Number) :void
    {
        super.setActualSize(w, h);

        // update our size in the mask
        var g :Graphics = _mask.graphics;
        g.clear();
        g.beginFill(0xFFFFFF);
        g.drawRect(0, 0, w, h);
        g.endFill();

        // draw the checkerboard on the background, which isn't even
        // technically our child
        g = _background.graphics;
        g.clear();
        var dark :Boolean;
        const GRID_SIZE :int = 10;
        for (var yy :int = 0; yy < h; yy += GRID_SIZE) {
            dark = ((yy % (GRID_SIZE * 2)) == 0);
            for (var xx :int = 0; xx < w; xx += GRID_SIZE) {
                g.beginFill(dark ? DARK_BKG : LIGHT_BKG);
                g.drawRect(xx, yy, GRID_SIZE, GRID_SIZE);
                g.endFill();
                dark = !dark;
            }
        }
    }

    protected var _background :Shape;

    protected var _mask :Shape;

    protected static const DARK_BKG :uint = 0x999999; //0xE3E3E3;
    protected static const LIGHT_BKG :uint = 0xCCCCCC; //0xF3F3F3;
}
