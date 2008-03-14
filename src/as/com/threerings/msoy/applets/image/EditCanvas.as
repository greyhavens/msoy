//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.CapsStyle;
import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.JointStyle;
import flash.display.LineScaleMode;
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
    public static const NONE :int = -1;
    public static const PAINT :int = 0;
    public static const ERASE :int = 1;
    public static const SELECT :int = 2;
    public static const MOVE :int = 3;

    public function EditCanvas (maxW :int, maxH:int)
    {
        this.maxWidth = maxW;
        this.maxHeight = maxH;

        _editor = new Sprite();

        _imageLayer = new Sprite();
        _scaleLayer = new Sprite();
        _rotLayer = new Sprite();
        _unRotLayer = new Sprite();
        _paintLayer = new Sprite();
        _fillLayer = new Shape();
        _hudLayer = new Sprite();

        _crop = new Sprite();
        _crop.mouseEnabled = false;
        _brush = new Shape();
        _brush.visible = false;

        _unRotLayer.addChild(_imageLayer);
        _unRotLayer.addChild(_fillLayer);
        _unRotLayer.addChild(_paintLayer);
        _rotLayer.addChild(_unRotLayer);
        _scaleLayer.addChild(_rotLayer);

        _editor.addChild(_scaleLayer);
        _editor.addChild(_hudLayer);

        _hudLayer.addChild(_crop);

        _paintLayer.addChild(_brush);

        _holder = new ImageHolder(_editor);
        addChild(_holder);

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
        updateBrush();
    }

    public function getPaintColor () :uint
    {
        return _color;
    }

    public function setBrushSize (size :Number) :void
    {
        _brushSize = size;
        updateBrush();
    }

    public function getBrushSize () :Number
    {
        return _brushSize;
    }

    public function setForcedCrop (wid :Number, hei :Number) :void
    {
        _forceCrop = true;
        _cropPoint = new Point(0, 0);
        updateSelection(wid, hei);
        _cropPoint = null;
    }

    public function doCrop () :void
    {
        if (_cropRect != null) {
            setImage(getRawImage());
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
        _fillLayer.graphics.clear();
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
        // TODO: this should probably be removed unless we're in preview-only mode?
        if (_bytes != null && _cropRect == null) {
            return _bytes;
        }

        var bmp :BitmapData = getRawImage();
        if (asJpg) {
            return (new JPGEncoder(quality)).encode(bmp);
        } else {
            return PNGEncoder.encode(bmp);
        }
    }

    public function getRawImage () :BitmapData
    {
        if (_bitmapData != null && _cropRect == null) {
            return _bitmapData;
        }

        var bmp :BitmapData;
        var matrix :Matrix = null;
        if (_cropRect == null) {
            bmp = new BitmapData(_width, _height, true, 0);
        } else {
            bmp = new BitmapData(_cropRect.width, _cropRect.height, true, 0);
            matrix = new Matrix(1, 0, 0, 1, -_cropRect.x, -_cropRect.y);
        }

        // We have to have the brush on the image layer so that it participates in rotataions
        var brushVis :Boolean = _brush.visible;
        _brush.visible = false;
        // screenshot the image
        bmp.draw(_scaleLayer, matrix);
        _brush.visible = brushVis;

        return bmp;
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

    protected function configureMode () :void
    {
        var fn :Function;
        var on :Boolean;

        // PAINT || ERASE
        on = (_mode == PAINT) || (_mode == ERASE);
        fn = on ? _paintLayer.addEventListener : _paintLayer.removeEventListener;
        fn(MouseEvent.ROLL_OVER, handleShowBrush);
        fn(MouseEvent.ROLL_OUT, handleShowBrush);
        fn(MouseEvent.ROLL_OVER, handlePaintEnter);
        fn(MouseEvent.MOUSE_DOWN, handlePaintStart);
        fn(MouseEvent.MOUSE_MOVE, handlePaintMove);
        fn(MouseEvent.MOUSE_UP, handlePaintEnd);
        _paintLayer.mouseEnabled = on;

        // SELECT
        on = (_mode == SELECT);
        fn = on ? _hudLayer.addEventListener : _hudLayer.removeEventListener;
        fn(MouseEvent.MOUSE_DOWN, handleSelectStart);
        fn(MouseEvent.MOUSE_UP, handleSelectEnd);
        fn(MouseEvent.MOUSE_OUT, handleSelectEnd);
        _hudLayer.mouseEnabled = on;

        // MOVE
        on = (_mode == MOVE);
        fn = on ? _crop.addEventListener : _crop.removeEventListener;
        fn(MouseEvent.MOUSE_DOWN, handleCropSelect);
        fn(MouseEvent.MOUSE_UP, handleCropUp);
        _crop.mouseEnabled = on;
    }

    protected function updateBrush () :void
    {
        var g :Graphics = _brush.graphics;
        g.clear();
        g.beginFill(_color);
        g.drawCircle(0, 0, _brushSize/2);
        g.endFill();
    }

    // Editing operations

    protected function handleShowBrush (event :MouseEvent) :void
    {
        _brush.visible = (event.type == MouseEvent.ROLL_OVER) && (_mode == PAINT);
    }

    protected function handlePaintEnter (event :MouseEvent) :void
    {
        if (event.buttonDown) {
            handlePaintStart(event);
        }
    }

    protected function handlePaintStart (event :MouseEvent) :void
    {
        setPainted();

        // paint the brush stamp
        var g :Graphics = _fillLayer.graphics;
        g.beginFill(_color);
        g.drawCircle(event.localX, event.localY, _brushSize/2);
        g.endFill();

        // set up lining
        g = _paintLayer.graphics;
        g.lineStyle(_brushSize, _color, 1, false, LineScaleMode.NORMAL, CapsStyle.ROUND,
            JointStyle.ROUND);
        g.moveTo(event.localX, event.localY);
        _paintLayer.addEventListener(MouseEvent.MOUSE_MOVE, handlePaintLine);
        _paintLayer.addEventListener(MouseEvent.ROLL_OUT, handlePaintEnd);

//        const x :int = Math.round(event.localX);
//        const y :int = Math.round(event.localY);
//
//        // TODO: there doesn't seem to be a way to actually draw just 1 pixel.
//        // (If you are zoomed, this 1 pixel is actually larger than a pixel)
//        var g :Graphics = _paintLayer.graphics;
//        g.lineStyle(1, _color);
//        g.drawRect(x, y, 1, 1);
    }

    protected function handlePaintMove (event :MouseEvent) :void
    {
        _brush.x = event.localX;
        _brush.y = event.localY;
    }

    protected function handlePaintLine (event :MouseEvent) :void
    {
        _paintLayer.graphics.lineTo(event.localX, event.localY);
    }

    protected function handlePaintEnd (event :MouseEvent) :void
    {
        handlePaintLine(event);
        _paintLayer.removeEventListener(MouseEvent.MOUSE_MOVE, handlePaintLine);
        _paintLayer.removeEventListener(MouseEvent.ROLL_OUT, handlePaintEnd);
    }

    protected function handleSelectStart (event :MouseEvent) :void
    {
        _cropPoint = new Point(event.localX, event.localY);
        updateSelection(event.localX, event.localY);

        _hudLayer.addEventListener(MouseEvent.MOUSE_MOVE, handleSelectUpdate);
    }

    protected function handleSelectUpdate (event :MouseEvent) :void
    {
        updateSelection(event.localX, event.localY);
    }

    protected function handleSelectEnd (event :MouseEvent) :void
    {
        if (_cropPoint != null) {
            updateSelection(event.localX, event.localY);
            _cropPoint = null;

            _hudLayer.removeEventListener(MouseEvent.MOUSE_MOVE, handleSelectUpdate);
        }
    }

    protected function updateSelection (x :Number, y :Number) :void
    {
        _cropRect = new Rectangle(Math.min(x, _cropPoint.x), Math.min(y, _cropPoint.y),
            Math.abs(x - _cropPoint.x), Math.abs(y - _cropPoint.y));

        if (_cropRect.width == 0 || _cropRect.height == 0) {
            clearSelection();
            return;
        }

        _crop.x = _cropRect.x;
        _crop.y = _cropRect.y;

        var g :Graphics = _crop.graphics;
        g.clear();
        g.lineStyle(1);
        GraphicsUtil.dashRect(g, 0, 0, _cropRect.width, _cropRect.height)
        g.lineStyle(0, 0, 0);
        g.beginFill(0xFFFFFF, 0);
        g.drawRect(0, 0, _cropRect.width, _cropRect.height);
        g.endFill();
    }

    protected function clearSelection () :void
    {
        if (_forceCrop) { // just reset it
            _crop.x = 0;
            _crop.y = 0;
            _cropRect.x = 0;
            _cropRect.y = 0;

        } else { // actually clear it
            _crop.graphics.clear();
            _cropRect = null;
        }
    }

    protected function handleCropSelect (event :MouseEvent) :void
    {
        _crop.startDrag(false,
            new Rectangle(0, 0, _width - _cropRect.width, _height - _cropRect.height));
    }

    protected function handleCropUp (event :MouseEvent) :void
    {
        _crop.stopDrag();
        _cropRect.x = _crop.x;
        _cropRect.y = _crop.y;
    }

    /** 
     * Sets that we've painted on the image.
     */
    protected function setPainted () :void
    {
        // we just clear the objects that might be used to short-cut a return object
        _bitmapData = null;
        _bytes = null;
    }

    protected var _holder :ImageHolder;

    protected var _editor :Sprite;

    /** Layers that contain things. */
    protected var _imageLayer :Sprite;
    protected var _paintLayer :Sprite;
    protected var _fillLayer :Shape; // an additional paintLayer just for fills
    protected var _hudLayer :Sprite;

    /** Layers used to affect the rotation/zoom/etc. */
    protected var _scaleLayer :Sprite;
    protected var _rotLayer :Sprite;
    protected var _unRotLayer :Sprite;

    /** Sprites used to represent bits. */
    protected var _crop :Sprite;
    protected var _brush :Shape;

    protected var _cropRect :Rectangle;
    protected var _cropPoint :Point;

    protected var _paintPoint :Point;

    protected var _bitmapData :BitmapData;
    protected var _bytes :ByteArray;

    protected var _image :DisplayObject;

    protected var _width :int;
    protected var _height :int;

    protected var _mode :int;
    protected var _color :uint;
    protected var _brushSize :Number;
    protected var _forceCrop :Boolean = false;
}
}

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Sprite;

import mx.core.UIComponent;

class ImageHolder extends UIComponent
{
    public function ImageHolder (toBeHeld :DisplayObject)
    {
        _background = new Sprite();
        addChild(_background);
        addChild(toBeHeld);
    }

    override public function setActualSize (w :Number, h :Number) :void
    {
        super.setActualSize(w, h);

        var g :Graphics = _background.graphics;
        g.clear();
        var dark :Boolean;
        const GRID_SIZE :int = 10;
        for (var yy :int = 0; yy < w; yy += GRID_SIZE) {
            dark = ((yy % (GRID_SIZE * 2)) == 0);
            for (var xx :int = 0; xx < h; xx += GRID_SIZE) {
                g.beginFill(dark ? 0x666666 : 0x999999);
                g.drawRect(xx, yy, GRID_SIZE, GRID_SIZE);
                g.endFill();
                dark = !dark;
            }
        }
    }

    protected var _background :Sprite;
}
