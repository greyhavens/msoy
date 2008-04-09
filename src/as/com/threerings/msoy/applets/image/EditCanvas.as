//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.BlendMode;
import flash.display.CapsStyle;
import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.JointStyle;
import flash.display.LineScaleMode;
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

import mx.core.ScrollPolicy;

import com.adobe.images.JPGEncoder; 
import com.adobe.images.PNGEncoder;

import com.threerings.util.ValueEvent;

import com.threerings.flash.GraphicsUtil;

/** 
 * Dispatched when a color is selected.
 */
[Event(name="ColorSelected", type="com.threerings.util.ValueEvent")]

/** 
 * Dispatched when the undo/redo status has changed.
 */
[Event(name="UndoRedoChange", type="com.threerings.util.ValueEvent")]

/**
 * Allows primitive editing of an image. Note that this is merely the model/view, the
 * controller is ImageManipulator.
 */
// TODO/Notes:
// - If you paint a fat stroke right next to the scrollbar, you can actually paint onto
//   a portion of the image that's offscreen. Fix so that _curPaint has a mask of the currently
//   viewable area...?
//
// - erasing needs to work on a drawn area or the cropping snapshot will pick up the eraser strokes
public class EditCanvas extends DisplayCanvas
{
    public static const SIZE_KNOWN :String = DisplayCanvas.SIZE_KNOWN;

    public static const COLOR_SELECTED :String = "ColorSelected";

    public static const UNDO_REDO_CHANGE :String = "UndoRedoChange";

    /** Formatting constants. */
    public static const IMAGE_FORMAT_JPG :String = "jpg";
    public static const IMAGE_FORMAT_PNG :String = "png";

    /** Mode constants. */
    public static const NONE :int = -1;
    public static const PAINT :int = 0;
    public static const ERASE :int = 1;
    public static const SELECT_COLOR :int = 2;
    public static const SELECT :int = 3;
    public static const MOVE :int = 4;

    public function EditCanvas (maxW :int, maxH :int, editMode :Boolean)
    {
        super(maxW, maxH);
        if (editMode) {
            width = maxW;
            height = maxH;
            horizontalScrollPolicy = ScrollPolicy.ON;
            verticalScrollPolicy = ScrollPolicy.ON;
        }

        _paintLayer = new Sprite();
        _scaleLayer = new Sprite();
        _rotLayer = new Sprite();
        _unRotLayer = new Sprite();
        _hudLayer = new Sprite();

        _crop = new Sprite();
        _crop.mouseEnabled = false;
        _brush = new Shape();
        _brush.visible = false;
        _dropper.visible = false;

        _paintLayer.blendMode = BlendMode.LAYER;

        _unRotLayer.addChild(_paintLayer);
        _rotLayer.addChild(_unRotLayer);
        _scaleLayer.addChild(_rotLayer);

        _baseLayer.addChild(_scaleLayer);
        _baseLayer.addChild(_hudLayer);

        _hudLayer.addChild(_crop);

        _paintLayer.addChild(_brush);
        _paintLayer.addChild(_dropper);
        _paintInsertionOffset = _paintLayer.numChildren;
    }

    public function canUndo () :Boolean
    {
        return _undoStack.length > 0;
    }

    public function canRedo () :Boolean
    {
        return _redoStack.length > 0;
    }

    public function doUndo () :void
    {
        var layer :Shape = _undoStack.pop() as Shape;
        _paintLayer.removeChild(layer);
        _redoStack.push(layer);
        fireUndoRedoChange();
    }

    public function doRedo () :void
    {
        var layer :Shape = _redoStack.pop() as Shape;
        _paintLayer.addChildAt(layer, _paintLayer.numChildren - _paintInsertionOffset);
        _undoStack.push(layer);
        fireUndoRedoChange();
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

    public function setBrushShape (circle :Boolean) :void
    {
        _brushCircle = circle;
        updateBrush();
    }

    public function setForcedCrop (wid :Number, hei :Number) :void
    {
        _forceCrop = true;
        _cropPoint = new Point(0, 0);
        updateSelection(new Point(wid, hei));
        _cropPoint = null;
    }

    public function doCrop () :void
    {
        if (_cropRect != null) {
            setImage(getRawImage());
        }
    }

    /**
     * @inheritDoc
     */
    override public function clearImage () :void
    {
        super.clearImage();

        _paintPoint = null;
        setScale(1);
        setZoom(1);
        setRotation(0);

        // remove all paint layers
        for each (var layer :Shape in _undoStack) {
            _paintLayer.removeChild(layer);
        }
        _undoStack.length = 0;
        _redoStack.length = 0;
        fireUndoRedoChange();

        _paintLayer.graphics.clear();
        _paintLayer.x = 0;
        _paintLayer.y = 0;
        _hudLayer.graphics.clear();
        clearSelection();
    }

    /**
     * @inheritDoc
     */
    override public function setImage (image :Object) :void
    {
        super.setImage(image);

        if (_image != null) {
            _image.x = GUTTER;
            _image.y = GUTTER;
        }

        configureMode();
    }

    /**
     * Get the image back out of the editor.
     */
    public function getImage (forceFormat :String = null, formatArg :Object = null) :Array
    {
        // see if we can skip re-encoding
        // TODO: this should probably be removed unless we're in preview-only mode?
        if (forceFormat == null && _bytes != null && _cropRect == null) {
            return [ _bytes ];
        }

        var bmp :BitmapData = getRawImage();
        if (forceFormat == IMAGE_FORMAT_JPG) {
            var quality :Number = (formatArg == null) ? 50 : Number(formatArg);
            return [ (new JPGEncoder(quality)).encode(bmp), IMAGE_FORMAT_JPG ];
        } else {
            return [ PNGEncoder.encode(bmp), IMAGE_FORMAT_PNG ];
        }
    }

    public function getRawImage () :BitmapData
    {
        if (_bitmapData != null && _cropRect == null) {
            return _bitmapData;
        }

        var bmp :BitmapData;
        var matrix :Matrix = new Matrix(_scaleLayer.scaleX, 0, 0, _scaleLayer.scaleY);
        if (_cropRect == null) {
            bmp = new BitmapData(_imgWidth, _imgHeight, true, 0);
        } else {
            bmp = new BitmapData(_cropRect.width, _cropRect.height, true, 0);
            matrix.tx = -_cropRect.x;
            matrix.ty = -_cropRect.y;
        }

        // We have to have the brush on the image layer so that it participates in rotataions
        var brushVis :Boolean = _brush.visible;
        var dropperVis :Boolean = _dropper.visible;
        _brush.visible = false;
        _dropper.visible = false;
        // screenshot the image
        try {
            bmp.draw(_scaleLayer, matrix);
        } finally {
            _brush.visible = brushVis;
            _dropper.visible = dropperVis;
        }

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
        _scale = scale;
        _scaleLayer.scaleX = scale;
        _scaleLayer.scaleY = scale;
        updateBrush();
    }

    override protected function updateCanvasSize () :void
    {
        // We DON'T call super

        const canvWidth :int = _imgWidth + (2 * GUTTER);
        const canvHeight :int = _imgHeight + (2 * GUTTER);

        _holder.width = canvWidth;
        _holder.height = canvHeight;

        _rotLayer.x = canvWidth/2;
        _rotLayer.y = canvHeight/2;
        _unRotLayer.x = canvWidth/-2;
        _unRotLayer.y = canvHeight/-2;

        // color some layers so we can click on them
        var g :Graphics = _paintLayer.graphics;
        g.clear();
        g.beginFill(0xFFFFFF, 0);
        g.drawRect(0, 0, canvWidth, canvHeight);
        g.endFill();

        g = _hudLayer.graphics;
        g.clear();
        g.beginFill(0xFFFFFF, 0);
        g.drawRect(0, 0, canvWidth, canvHeight);
        g.endFill();

        // jiggle the canvas width. See notes in super.updateCanvasSize()
        this.width = this.maxWidth;
        this.height = this.maxHeight;

        // TODO: needed?
        //configureMode();
    }

    protected function configureMode () :void
    {
        endCurrentPaint();

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
        _brush.blendMode = (_mode == ERASE) ? BlendMode.ERASE : BlendMode.NORMAL;
        // special hack to go to painting directly from eyedropping
        if (on && _dropper.visible) {
            _dropper.visible = false;
            _brush.visible = true;
            _brush.x = _dropper.x;
            _brush.y = _dropper.y;
        }

        // SELECT
        on = (_mode == SELECT);
        fn = on ? _hudLayer.addEventListener : _hudLayer.removeEventListener;
        fn(MouseEvent.MOUSE_DOWN, handleSelectStart);
        fn(MouseEvent.MOUSE_UP, handleSelectEnd);
        fn(MouseEvent.MOUSE_OUT, handleSelectEnd);
        _hudLayer.mouseEnabled = on;

        // MOVE
        on = (_mode == MOVE);
        fn = on ? _paintLayer.addEventListener : _paintLayer.removeEventListener;
        fn(MouseEvent.MOUSE_DOWN, handleCropSelect);
        fn(MouseEvent.MOUSE_UP, handleCropUp);
        //_crop.mouseEnabled = on;

        // SELECT_COLOR
        on = (_mode == SELECT_COLOR);
        fn = on ? _paintLayer.addEventListener : _paintLayer.removeEventListener;
        fn(MouseEvent.CLICK, handleDropperClick);
        fn(MouseEvent.MOUSE_MOVE, handleDropperMove)
        fn(MouseEvent.ROLL_OVER, handleShowDropper);
        fn(MouseEvent.ROLL_OUT, handleShowDropper);

        // and finally:
        _paintLayer.mouseEnabled = (_mode == PAINT) || (_mode == ERASE) || (_mode == SELECT_COLOR) ||
            (_mode == MOVE);
    }

    protected function updateBrush () :void
    {
        var g :Graphics = _brush.graphics;
        g.clear();
        g.beginFill(_color);
        const radius :Number = (_brushSize/2) / _scale;
        if (_brushCircle) {
            g.drawCircle(0, 0, radius);
        } else {
            g.drawRect(-radius, -radius, radius * 2, radius * 2);
        }
        g.endFill();
    }

    protected function layerPoint (layer :DisplayObject, event :MouseEvent) :Point
    {
        return layer.globalToLocal(new Point(event.stageX, event.stageY));
    }

    // Editing operations

    protected function handleShowDropper (event :MouseEvent) :void
    {
        _dropper.visible = (event.type == MouseEvent.ROLL_OVER) && (_mode == SELECT_COLOR);
    }

    protected function handleDropperMove (event :MouseEvent) :void
    {
        var p :Point = layerPoint(_paintLayer, event);
        _dropper.x = p.x;
        _dropper.y = p.y;

        p = layerPoint(_scaleLayer, event);
        var value :uint = getDropperColor(p);
        var color :uint = (value & 0xFFFFFF);
        var alpha :Number = ((value >> 24) & 0xFF) / 255;
        _dropper.setColor(color, alpha);
    }

    protected function handleDropperClick (event :MouseEvent) :void
    {
        var p :Point = layerPoint(_scaleLayer, event);
        var value :uint = getDropperColor(p);
        var alpha :uint = (value >> 24) & 0xFF;

        if (alpha != 0) {
            var newColor :uint = value & 0xFFFFFF;
            setPaintColor(newColor);
            dispatchEvent(new ValueEvent(COLOR_SELECTED, newColor));
        }
    }

    protected function getDropperColor (p :Point) :uint
    {
        // paint into a 1x1 bitmapdata and see what color we get
        var bmp :BitmapData = new BitmapData(1, 1, true, 0)
        var matrix :Matrix = new Matrix(_scaleLayer.scaleX, 0, 0, _scaleLayer.scaleY, -p.x, -p.y)
        bmp.draw(_scaleLayer, matrix);

        return bmp.getPixel32(0, 0);
    }

    protected function handleShowBrush (event :MouseEvent) :void
    {
        _brush.visible = (event.type == MouseEvent.ROLL_OVER) &&
            ((_mode == PAINT) || (_mode == ERASE));
    }

    protected function handlePaintMove (event :MouseEvent) :void
    {
        var p :Point = layerPoint(_paintLayer, event);
        _brush.x = p.x;
        _brush.y = p.y;
    }

    protected function handlePaintEnter (event :MouseEvent) :void
    {
        if (event.buttonDown && _curPaint != null) {
            handlePaintStart(event);
        } else {
            endCurrentPaint();
        }
    }

    protected function handlePaintStart (event :MouseEvent) :void
    {
        setPainted();

        if (_curPaint == null) {
            // create a new paintlayer
            _curPaint =  new Shape();
            if (_mode == ERASE) {
                _curPaint.blendMode = BlendMode.ERASE;
            }
            _paintLayer.addChildAt(_curPaint, _paintLayer.numChildren - _paintInsertionOffset);
        }

        _paintPoint = layerPoint(_paintLayer, event);
        _paintLayer.addEventListener(MouseEvent.MOUSE_MOVE, handlePaintLine);
        _paintLayer.addEventListener(MouseEvent.ROLL_OUT, handlePaintEnd);
    }

    protected function handlePaintLine (event :MouseEvent) :void
    {
        var g :Graphics = _curPaint.graphics;
        if (_paintPoint != null) {
            g.lineStyle(_brushSize / _scale, _color, 1, false, LineScaleMode.NORMAL,
                _brushCircle ? CapsStyle.ROUND : CapsStyle.SQUARE,
                _brushCircle ? JointStyle.ROUND : JointStyle.BEVEL);
            g.moveTo(_paintPoint.x, _paintPoint.y);
            _paintPoint = null;
        }

        var p :Point = layerPoint(_paintLayer, event);
        g.lineTo(p.x, p.y);
    }

    protected function handlePaintEnd (event :MouseEvent) :void
    {
        if (_curPaint == null) {
            return; // we never started..

        } else if (_paintPoint == null) {
            handlePaintLine(event);

        } else {
            // there was never any line drawn, so we just stamp the brush
            var g :Graphics = _curPaint.graphics;
            g.beginFill(_color);
            const radius :Number = (_brushSize/2) / _scale;
            var p :Point = layerPoint(_paintLayer, event);
            if (_brushCircle) {
                g.drawCircle(p.x, p.y, radius);
            } else {
                g.drawRect(p.x - radius, p.y - radius, radius * 2, radius * 2);
            }
            g.endFill();
            _paintPoint = null;
        }
        _paintLayer.removeEventListener(MouseEvent.MOUSE_MOVE, handlePaintLine);
        _paintLayer.removeEventListener(MouseEvent.ROLL_OUT, handlePaintEnd);

        // if the button is still down, we don't end the current paint.
        if (!event.buttonDown) {
            endCurrentPaint();
        }
    }

    protected function endCurrentPaint () :void
    {
        if (_curPaint != null) {
            _undoStack.push(_curPaint);
            _redoStack.length = 0;
            fireUndoRedoChange();
            _curPaint = null;
        }
    }

    protected function handleSelectStart (event :MouseEvent) :void
    {
        _cropPoint = layerPoint(_hudLayer, event);
        updateSelection(_cropPoint);

        _hudLayer.addEventListener(MouseEvent.MOUSE_MOVE, handleSelectUpdate);
    }

    protected function handleSelectUpdate (event :MouseEvent) :void
    {
        updateSelection(layerPoint(_hudLayer, event));
    }

    protected function handleSelectEnd (event :MouseEvent) :void
    {
        if (_cropPoint != null) {
            updateSelection(layerPoint(_hudLayer, event));
            _cropPoint = null;

            _hudLayer.removeEventListener(MouseEvent.MOUSE_MOVE, handleSelectUpdate);
        }
    }

    protected function updateSelection (p :Point) :void
    {
        _cropRect = new Rectangle(Math.min(p.x, _cropPoint.x), Math.min(p.y, _cropPoint.y),
            Math.abs(p.x - _cropPoint.x), Math.abs(p.y - _cropPoint.y));

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
        _paintLayer.startDrag(false);
    }

    protected function handleCropUp (event :MouseEvent) :void
    {
        _paintLayer.stopDrag();
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

    protected function fireUndoRedoChange () :void
    {
        dispatchEvent(new ValueEvent(UNDO_REDO_CHANGE, null));
    }

    override protected function getImageLayer () :Sprite
    {
        return _paintLayer;
    }

    /** Layers that contain things. */
    protected var _paintLayer :Sprite;
    protected var _hudLayer :Sprite;

    /** Paint layers. */
    protected var _undoStack :Array = [];

    protected var _redoStack :Array = [];

    /** The current paint layer. */
    protected var _curPaint :Shape;

    /** Layers used to affect the rotation/zoom/etc. */
    protected var _scaleLayer :Sprite;
    protected var _rotLayer :Sprite;
    protected var _unRotLayer :Sprite;

    /** Sprites used to represent bits. */
    protected var _crop :Sprite;
    protected var _brush :Shape;
    protected var _dropper :DropperCursor = new DropperCursor();

    protected var _cropRect :Rectangle;
    protected var _cropPoint :Point;

    protected var _paintPoint :Point;

    protected var _scale :Number = 1;

    protected var _paintInsertionOffset :int;

    protected var _mode :int;
    protected var _color :uint;
    protected var _brushSize :Number = 1;
    protected var _brushCircle :Boolean = true;
    protected var _forceCrop :Boolean = false;

    /** The number of pixels around the image that we provide as "working area". */
    protected static const GUTTER :int = 150;
}
}

import flash.display.Graphics;
import flash.display.Shape;

class DropperCursor extends Shape
{
    public function setColor (color :uint, alpha :Number = 1) :void
    {
        var max :uint = Math.max(color & 0xFF, (color >> 8) & 0xFF);
        max = Math.max(max, (color >> 16) & 0xFF);

        var g :Graphics = graphics;
        g.clear();
        g.beginFill(color, alpha);
        g.lineStyle(1, (max > 127) ? 0x000000 : 0xFFFFFF);
        g.drawRect(0, 10, 20, 20);
        g.endFill();
    }
}
