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
import flash.events.MouseEvent;

import flash.geom.Matrix;
import flash.geom.Point;
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

import com.threerings.flash.GraphicsUtil;

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

    /** Mode constants. */
    public static const PAINT :int = 0;
    public static const SELECT :int = 1;
    public static const MOVE :int = 2;

    public function EditCanvas (maxW :int, maxH:int)
    {
        this.maxWidth = maxW;
        this.maxHeight = maxH;

        _editor = new Sprite();

        _backgroundLayer = new Sprite();
        _imageLayer = new Sprite();
        _scaleLayer = new Sprite();
        _rotLayer = new Sprite();
        _unRotLayer = new Sprite();
        _paintLayer = new Sprite();
        _hudLayer = new Sprite();

        _cropSprite = new Sprite();

        _unRotLayer.addChild(_imageLayer);
        _unRotLayer.addChild(_paintLayer);
        _rotLayer.addChild(_unRotLayer);
        _scaleLayer.addChild(_rotLayer);

        _editor.addChild(_backgroundLayer);
        _editor.addChild(_scaleLayer);
        _editor.addChild(_hudLayer);

        _hudLayer.addChild(_cropSprite);
        _cropSprite.mouseEnabled = false;

        _holder = new UIComponent();
        _holder.addChild(_editor);

//        var mask :Shape = new Shape();
//        mask.graphics.beginFill(0xFFFFFF);
//        mask.graphics.drawRect(0, 0, maxWidth, maxHeight);
//        mask.graphics.endFill();
//        _editor.mask = mask;
//        _editor.addChild(mask);
    }

    public function setMode (mode :int) :void
    {
        _mode = mode;
        configureMode();
    }

    public function getMode () :int
    {
        return _mode;
    }

    public function setPaintColor (color :uint) :void
    {
        _color = color;
    }

    public function getPaintColor () :uint
    {
        return _color;
    }

    public function doCrop () :void
    {
        if (_crop != null) {
            setImage(getImage());
        }
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
        _paintLayer.graphics.clear();
        _hudLayer.graphics.clear();
        clearSelection();

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
        configureMode();
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

        var bmp :BitmapData;
        if (_bitmapData != null && _crop == null) {
            bmp = _bitmapData;

        } else {
            var matrix :Matrix = null;
            if (_crop == null) {
                bmp = new BitmapData(_width, _height);
            } else {
                bmp = new BitmapData(_crop.width, _crop.height);
                matrix = new Matrix(1, 0, 0, 1, -_crop.x, -_crop.y);
            }
            // screenshot the image
            bmp.draw(_scaleLayer, matrix);
        }

        if (asJpg) {
            return (new JPGEncoder(quality)).encode(bmp);
        } else {
            return PNGEncoder.encode(bmp);
        }
    }

    public function setRotation (rotation :Number) :void
    {
        _rotLayer.rotation = rotation;
    }

    public function setZoom (zoom :Number) :void
    {
        _holder.scaleX = zoom;
        _holder.scaleY = zoom;
        _holder.invalidateSize();
    }

    public function setScale (scale :Number) :void
    {
        _scaleLayer.scaleX = scale;
        _scaleLayer.scaleY = scale;
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

        _rotLayer.x = _width/2;
        _rotLayer.y = _height/2;
        _unRotLayer.x = _width/-2;
        _unRotLayer.y = _height/-2;

        // color some layers so we can click on them
        var g :Graphics = _paintLayer.graphics;
        g.beginFill(0xFFFFFF, 0);
        g.drawRect(0, 0, _width, _height);
        g.endFill();

        g = _hudLayer.graphics;
        g.beginFill(0xFFFFFF, 0);
        g.drawRect(0, 0, _width, _height);
        g.endFill();

        configureMode();
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

    protected function configureMode () :void
    {
        var fn :Function;

        // PAINT
        fn = (_mode == PAINT) ? _paintLayer.addEventListener : _paintLayer.removeEventListener;
        fn(MouseEvent.CLICK, handlePaintPixel);
        _paintLayer.mouseEnabled = (_mode == PAINT);

        // SELECT
        fn = (_mode == SELECT) ? _hudLayer.addEventListener : _hudLayer.removeEventListener;
        fn(MouseEvent.MOUSE_DOWN, handleSelectStart);
        _hudLayer.mouseEnabled = (_mode == SELECT);

        // MOVE
        // TODO
    }

    // Editing operations

    protected function handlePaintPixel (event :MouseEvent) :void
    {
        const x :int = Math.round(event.localX);
        const y :int = Math.round(event.localY);

        // TODO: there doesn't seem to be a way to actually draw just 1 pixel.
        // (If you are zoomed, this 1 pixel is actually larger than a pixel)
        var g :Graphics = _paintLayer.graphics;
        g.lineStyle(1, _color);
        g.drawRect(x, y, 1, 1);
    }

    protected function handleSelectStart (event :MouseEvent) :void
    {
        _cropPoint = new Point(event.localX, event.localY);
        updateSelection(event.localX, event.localY);

        _hudLayer.addEventListener(MouseEvent.MOUSE_MOVE, handleSelectUpdate);
        _hudLayer.addEventListener(MouseEvent.MOUSE_UP, handleSelectEnd);
        _hudLayer.addEventListener(MouseEvent.MOUSE_OUT, handleSelectEnd);
    }

    protected function handleSelectUpdate (event :MouseEvent) :void
    {
        updateSelection(event.localX, event.localY);
    }

    protected function handleSelectEnd (event :MouseEvent) :void
    {
        updateSelection(event.localX, event.localY);

        _hudLayer.removeEventListener(MouseEvent.MOUSE_MOVE, handleSelectUpdate);
        _hudLayer.removeEventListener(MouseEvent.MOUSE_UP, handleSelectEnd);
        _hudLayer.removeEventListener(MouseEvent.MOUSE_OUT, handleSelectEnd);
    }

    protected function updateSelection (x :Number, y :Number) :void
    {
        _crop = new Rectangle(Math.min(x, _cropPoint.x), Math.min(y, _cropPoint.y),
            Math.abs(x - _cropPoint.x), Math.abs(y - _cropPoint.y));

        if (_crop.width == 0 || _crop.height == 0) {
            clearSelection();
            return;
        }

        _cropSprite.x = _crop.x;
        _cropSprite.y = _crop.y;

        var g :Graphics = _cropSprite.graphics;
        g.clear();
        g.lineStyle(1);
        GraphicsUtil.dashRect(g, 0, 0, _crop.width, _crop.height)
    }

    protected function clearSelection () :void
    {
        _cropSprite.graphics.clear();
        _crop = null;
    }

    protected var _holder :UIComponent;

    protected var _editor :Sprite;

    /** Layers that contain things. */
    protected var _backgroundLayer :Sprite;
    protected var _imageLayer :Sprite;
    protected var _paintLayer :Sprite;
    protected var _hudLayer :Sprite;

    /** Layers used to affect the rotation/zoom/etc. */
    protected var _scaleLayer :Sprite;
    protected var _rotLayer :Sprite;
    protected var _unRotLayer :Sprite;

    /** Sprites used to represent bits. */
    protected var _cropSprite :Sprite;

    protected var _bitmapData :BitmapData;

    protected var _crop :Rectangle;

    protected var _cropPoint :Point;

    protected var _bytes :ByteArray;

    protected var _image :DisplayObject;

    protected var _width :int;

    protected var _height :int;

    protected var _mode :int;

    protected var _color :uint;
}
}
