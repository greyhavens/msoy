//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.PixelSnapping;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Rectangle;

import flash.net.URLRequest;

import flash.system.ApplicationDomain;
import flash.system.LoaderContext;

import flash.utils.ByteArray;

import mx.containers.Canvas;

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
public class EditCanvas extends Canvas
{
    public static const SIZE_KNOWN :String = "SizeKnown";

    public function EditCanvas (maxW :int, maxH:int)
    {
        this.maxWidth = maxW;
        this.maxHeight = maxH;

        _editor = new Sprite();

        _editor.addChild(_imageLayer = new Sprite());
        _editor.addChild(_hudLayer = new Sprite());

        _backgroundLayer = new Sprite();

        _holder = new UIComponent();
        _holder.addChild(_editor);

//        var mask :Shape = new Shape();
//        mask.graphics.beginFill(0xFFFFFF);
//        mask.graphics.drawRect(0, 0, maxWidth, maxHeight);
//        mask.graphics.endFill();
//        _editor.mask = mask;
//        _editor.addChild(mask);
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

    public function setRotation (rotation :Number) :void
    {
        // rotate arond the center
    }

    public function setZoom (zoom :Number) :void
    {
        _holder.scaleX = zoom;
        _holder.scaleY = zoom;
        _holder.invalidateSize();
    }

    public function setScale (scale :Number) :void
    {
    }

    protected function sizeKnown (width :Number, height :Number) :void
    {
        _width = width;
        _height = height;

        _holder.width = width;
        _holder.height = height;

        // un-fucking believable
        this.width = Math.min(this.maxWidth, width);
        this.height = Math.min(this.maxHeight, height);

        dispatchEvent(new ValueEvent(SIZE_KNOWN, [ _width, _height ]));
    }

    protected function handleImageLoadComplete (event :Event) :void
    {
        var li :LoaderInfo = event.target as LoaderInfo;
        sizeKnown(li.width, li.height);
    }

    override protected function createChildren () :void
    {
//        var g :Graphics = _backgroundLayer.graphics;
//        var dark :Boolean;
//        const GRID_SIZE :int = 10;
//        for (var yy :int = 0; yy < maxHeight; yy += GRID_SIZE) {
//            dark = ((yy % (GRID_SIZE * 2)) == 0);
//            for (var xx :int = 0; xx < maxWidth; xx += GRID_SIZE) {
//                g.beginFill(dark ? 0x666666 : 0x999999);
//                g.drawRect(xx, yy, GRID_SIZE, GRID_SIZE);
//                g.endFill();
//                dark = !dark;
//            }
//        }
//        rawChildren.addChildAt(_backgroundLayer, 0);

        super.createChildren();

        addChild(_holder);
    }

    protected var _holder :UIComponent;

    protected var _editor :Sprite;

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
