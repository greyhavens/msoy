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

import mx.styles.CSSStyleDeclaration;
import mx.styles.StyleManager;

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
 * Dispatched when the selection area has changed.
 * value - an Array of [width, height]
 */
[Event(name="SelChange", type="com.threerings.util.ValueEvent")]

/**
 * Allows primitive editing of an image. Note that this is merely the model/view, the
 * controller is ImageManipulator.
 */
// TODO/Notes:
// - If you paint a fat stroke right next to the scrollbar, you can actually paint onto
//   a portion of the image that's offscreen. Fix so that _curPaint has a mask of the currently
//   viewable area...?
public class EditCanvas extends DisplayCanvas
{
    public static const SIZE_KNOWN :String = DisplayCanvas.SIZE_KNOWN;

    // Event constants
    public static const COLOR_SELECTED :String = "ColorSelected";
    public static const UNDO_REDO_CHANGE :String = "UndoRedoChange";
    public static const SELECTION_CHANGE :String = "SelChange";

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

    public function EditCanvas (maxW :int, maxH :int)
    {
        super(maxW, maxH);
        width = maxW;
        height = maxH;
        horizontalScrollPolicy = ScrollPolicy.ON;
        verticalScrollPolicy = ScrollPolicy.ON;

        _paintLayer = new Sprite();
        _scaleLayer = new Sprite();
        _rotLayer = new Sprite();
        _unRotLayer = new Sprite();
        _hudLayer = new Sprite();

        _hudLayer.mouseEnabled = false;

        _crop = new Sprite();
        _crop.mouseEnabled = false;
        _brushCursor = new Shape();
        _brushCursor.visible = false;
        _dropperCursor.visible = false;
        _moveCursor.visible = false;
        _moveCursor.mouseEnabled = false;
        _selectCursor.visible = false;
        _selectCursor.mouseEnabled = false;

        _paintLayer.blendMode = BlendMode.LAYER;

        _unRotLayer.addChild(_paintLayer);
        _rotLayer.addChild(_unRotLayer);
        _scaleLayer.addChild(_rotLayer);

        _baseLayer.addChild(_scaleLayer);
        _baseLayer.addChild(_hudLayer);

        _hudLayer.addChild(_crop);
        _hudLayer.addChild(_dropperCursor);
        _hudLayer.addChild(_moveCursor);
        _hudLayer.addChild(_selectCursor);

        _paintLayer.addChild(_brushCursor);
        _paintInsertionOffset = _paintLayer.numChildren;

        // set up some of our custom cursors
        iconToCursor(_moveCursor, ".moveButton");
        iconToCursor(_selectCursor, ".selectButton");

        // we always listen for cursor moves
        _paintLayer.addEventListener(MouseEvent.MOUSE_MOVE, handleCursorMove);
        _paintLayer.addEventListener(MouseEvent.ROLL_OVER, handleCursorVis);
        _paintLayer.addEventListener(MouseEvent.ROLL_OUT, handleCursorVis);
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
        doUndoRedo(true);
    }

    public function doRedo () :void
    {
        doUndoRedo(false);
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

    /**
     * Update the working area size from numbers entered by the user.
     */
    public function updateWorkingSize (wid :Number, hei :Number) :void
    {
        _workingArea.x += Math.floor((_workingArea.width - wid)/2);
        _workingArea.width = wid;

        _workingArea.y += Math.floor((_workingArea.height - hei)/2);
        _workingArea.height = hei;

        updateEditCanvasSize(false);
    }

    public function doCrop () :void
    {
        setImage(getRawImage());
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
        while (_paintLayer.numChildren > _paintInsertionOffset) {
            _paintLayer.removeChildAt(0);
        }
        _undoStack.length = 0;
        _redoStack.length = 0;
        fireUndoRedoChange();

        _paintLayer.graphics.clear();
        _paintLayer.x = 0;
        _paintLayer.y = 0;
        clearSelection();
        setWorkingArea(new Rectangle());
    }

    /**
     * @inheritDoc
     */
    override public function setImage (image :Object) :void
    {
        super.setImage(image);

        configureMode();
    }

    /**
     * Get the image back out of the editor.
     */
    public function getImage (forceFormat :String = null, formatArg :Object = null) :Array
    {
        // see if we can skip re-encoding
        // TODO: this should probably be removed unless we're in preview-only mode?
        if (forceFormat == null && _bytes != null) {
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
        if (_bitmapData != null) {
            return _bitmapData;
        }

        var bmp :BitmapData = new BitmapData(_workingArea.width, _workingArea.height, true, 0);
        var matrix :Matrix = new Matrix(_scaleLayer.scaleX, 0, 0, _scaleLayer.scaleY,
            -_workingArea.x, -_workingArea.y);

        // It appears that bmp.draw() renders slightly differently than screen display.
        // An element with blendMode == ERASE will actually show up if portions of it are not
        // erasing something. Placing drawn transparent pixels behind it doesn't fix things,
        // but putting transparent image pixels behind it does work.
        var eraseBlocker :Bitmap = new Bitmap(new BitmapData(1, 1, true, 0));
        eraseBlocker.scaleX = _canvasWidth;
        eraseBlocker.scaleY = _canvasHeight;
        eraseBlocker.rotation = -_rotLayer.rotation;
        var p :Point = _paintLayer.globalToLocal(_scaleLayer.localToGlobal(new Point()));
        eraseBlocker.x = p.x;
        eraseBlocker.y = p.y;
        _paintLayer.addChildAt(eraseBlocker, 0);

        // We have to have the brush on the image layer so that it participates in rotataions
        var brushVis :Boolean = _brushCursor.visible;
        _brushCursor.visible = false;
        // screenshot the image
        try {
            bmp.draw(_scaleLayer, matrix);
        } finally {
            _brushCursor.visible = brushVis;
            _paintLayer.removeChild(eraseBlocker);
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

        // TODO: combine with below...
        _holder.width = _canvasWidth * _scale;
        _holder.height = _canvasHeight * _scale;
        // complexity is more complex than we are
    }

    override protected function updateCanvasSize () :void
    {
        // We DON'T call super

        updateEditCanvasSize(true);

        // recenter the image
        if (_image != null) {
            _image.x = _imgWidth / -2;
            _image.y = _imgHeight / -2;
        }

        // jiggle the canvas width. See notes in super.updateCanvasSize()
        this.width = this.maxWidth;
        this.height = this.maxHeight;
    }

    protected function updateEditCanvasSize (basedOnImage :Boolean) :void
    {
        var ww :Number = _imgWidth;
        var hh :Number = _imgHeight;
        if (!basedOnImage) {
            ww = Math.max(ww, _workingArea.width);
            hh = Math.max(hh, _workingArea.height);
        }

        _hGutter = Math.max(MIN_GUTTER, (this.maxWidth - ww) / 2);
        _vGutter = Math.max(MIN_GUTTER, (this.maxHeight - hh) / 2);

        _canvasWidth = ww + (2 * _hGutter);
        _canvasHeight = hh + (2 * _vGutter);

        _holder.width = _canvasWidth;
        _holder.height = _canvasHeight;

        _rotLayer.x = _canvasWidth/2;
        _rotLayer.y = _canvasHeight/2;
        _unRotLayer.x = _canvasWidth/-2;
        _unRotLayer.y = _canvasHeight/-2;

        setWorkingArea(new Rectangle(_hGutter, _vGutter,
            basedOnImage ? ww : _workingArea.width,
            basedOnImage ? hh : _workingArea.height));

        // TODO: we actually want to maybe position the paint layer?

        // put the paint layer at the center???
        _paintLayer.x = _canvasWidth/2;
        _paintLayer.y = _canvasHeight/2;

        // color some layers so we can click on them
        paintLayerPositioned();
    }

    protected function paintLayerPositioned () :void
    {
        var g :Graphics = _paintLayer.graphics;
        g.clear();
        g.beginFill(0xFFFFFF, 0);
        //g.beginFill(0xFF0000, .1);
        g.drawRect(-_paintLayer.x, -_paintLayer.y, _canvasWidth, _canvasHeight);
        g.endFill();
    }

    protected function setWorkingArea (r :Rectangle) :void
    {
        _workingArea = r;

        var g :Graphics = _hudLayer.graphics;
        g.clear();

        g.lineStyle(1, 0x000000);
        GraphicsUtil.dashRect(g, r.x, r.y, r.width, r.height);

        g.lineStyle(0, 0, 0);
        g.beginFill(0xF9F9F9, .5);
        g.drawRect(0, 0, _canvasWidth, r.y);
        if (r.height > 0) {
            g.drawRect(0, r.y, r.x, r.height);
            g.drawRect(r.x + r.width, r.y, _canvasWidth - (r.x + r.width), r.height);
        }
        g.drawRect(0, r.y + r.height, _canvasWidth, _canvasHeight - (r.y + r.height));
        g.endFill();

        dispatchWorkingAreaSelection();
    }

    protected function dispatchWorkingAreaSelection () :void
    {
        dispatchEvent(new ValueEvent(SELECTION_CHANGE, [ _workingArea.width, _workingArea.height ]));
    }

    /**
     * Helper function to turn an icon specified in a style selector into a cursor.
     */
    protected function iconToCursor (cursor :Sprite, selector :String) :void
    {
        var style :CSSStyleDeclaration = StyleManager.getStyleDeclaration(selector);
        var icon :DisplayObject = new (style.getStyle("upIcon") as Class)() as DisplayObject;
        icon.x = 10;
        icon.y = -10;
        cursor.addChild(icon);
    }

    /**
     * Configure interaction based on the currently selected mode.
     */
    protected function configureMode () :void
    {
        endCurrentPaint();

        var fn :Function;
        var on :Boolean;

        // PAINT || ERASE
        on = (_mode == PAINT) || (_mode == ERASE);
        fn = on ? _paintLayer.addEventListener : _paintLayer.removeEventListener;
        fn(MouseEvent.ROLL_OVER, handlePaintEnter);
        fn(MouseEvent.MOUSE_DOWN, handlePaintStart);
        fn(MouseEvent.MOUSE_UP, handlePaintEnd);
        // special hack to go to painting directly from eyedropping
        if (on && _dropperCursor.visible) {
            _dropperCursor.visible = false;
            _brushCursor.visible = true;
            copyLocation(_dropperCursor, _brushCursor);
        }

        // SELECT
        on = (_mode == SELECT);
        fn = on ? _paintLayer.addEventListener : _paintLayer.removeEventListener;
        fn(MouseEvent.MOUSE_DOWN, handleSelectStart);
        fn(MouseEvent.MOUSE_UP, handleSelectEnd);

        // MOVE
        on = (_mode == MOVE);
        fn = on ? _paintLayer.addEventListener : _paintLayer.removeEventListener;
        fn(MouseEvent.MOUSE_DOWN, handleMoveStart);
        fn(MouseEvent.MOUSE_UP, handleMoveEnd);

        // SELECT_COLOR
        on = (_mode == SELECT_COLOR);
        fn = on ? _paintLayer.addEventListener : _paintLayer.removeEventListener;
        fn(MouseEvent.CLICK, handleDropperClick);
        fn(MouseEvent.MOUSE_MOVE, handleDropperMove)

        // set up the new cursor
        _cursor = cursorForMode();
    }

    protected function cursorForMode () :DisplayObject
    {
        switch (_mode) {
        case PAINT:
        case ERASE:
            _brushCursor.blendMode = (_mode == ERASE) ? BlendMode.ERASE : BlendMode.NORMAL;
            return _brushCursor;

        case SELECT:
            return _selectCursor;

        case MOVE:
            return _moveCursor;

        case SELECT_COLOR:
            return _dropperCursor;

        default:
            return null;
        }
    }

    protected function updateBrush () :void
    {
        var g :Graphics = _brushCursor.graphics;
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

    /**
     * Returns the point of the specified MouseEvent, local to the specified layer.
     */
    protected function layerPoint (layer :DisplayObject, event :MouseEvent) :Point
    {
        return layer.globalToLocal(new Point(event.stageX, event.stageY));
    }

    /**
     * Copy the screen location from one object to another, even if they're on different
     * layers.
     */
    protected function copyLocation (from :DisplayObject, to :DisplayObject) :void
    {
        var p :Point = from.parent.localToGlobal(new Point(from.x, from.y));
        p = to.parent.globalToLocal(p);
        to.x = p.x;
        to.y = p.y;
    }

    // Editing operations

    /**
     * Take care of hiding/showing the cursor when the mouse enters the canvas.
     */
    protected function handleCursorVis (event :MouseEvent) :void
    {
        if (_cursor != null) {
            _cursor.visible = (event.type == MouseEvent.ROLL_OVER);
        }
    }

    /**
     * Update the cursor position as the mouse moves around the canvas.
     */
    protected function handleCursorMove (event :MouseEvent) :void
    {
        if (_cursor != null) {
            var p :Point = layerPoint(_cursor.parent, event);
            _cursor.x = p.x;
            _cursor.y = p.y;
        }
    }

    /**
     * The dropper cursor needs an extra special step to update its displayed color.
     */
    protected function handleDropperMove (event :MouseEvent) :void
    {
        var p :Point = layerPoint(_scaleLayer, event);
        var value :uint = getDropperColor(p);
        var color :uint = (value & 0xFFFFFF);
        var alpha :Number = ((value >> 24) & 0xFF) / 255;
        _dropperCursor.setColor(color, alpha);
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

    /**
     * Returns the color under the specified point.
     */
    protected function getDropperColor (p :Point) :uint
    {
        // paint into a 1x1 bitmapdata and see what color we get
        var bmp :BitmapData = new BitmapData(1, 1, true, 0)
        var matrix :Matrix = new Matrix(_scaleLayer.scaleX, 0, 0, _scaleLayer.scaleY, -p.x, -p.y)
        bmp.draw(_scaleLayer, matrix);

        return bmp.getPixel32(0, 0);
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
        if (_curPaint == null) {
            // create a new paintlayer
            _curPaint =  new Shape();
            _curPaint.blendMode = _brushCursor.blendMode; // copy the ERASE mode, if applicable
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
            pushUndo(_curPaint);
            _curPaint = null;
        }
    }

    protected function handleSelectStart (event :MouseEvent) :void
    {
        _cropPoint = layerPoint(_hudLayer, event);
        updateSelection(_cropPoint);

        _paintLayer.addEventListener(MouseEvent.MOUSE_MOVE, handleSelectUpdate);
        _paintLayer.addEventListener(MouseEvent.ROLL_OUT, handleSelectEnd);
    }

    protected function handleSelectUpdate (event :MouseEvent) :void
    {
        updateSelection(layerPoint(_hudLayer, event));
    }

    protected function handleSelectEnd (event :MouseEvent) :void
    {
        if (_cropPoint == null) {
            return;
        }

        _paintLayer.removeEventListener(MouseEvent.MOUSE_MOVE, handleSelectUpdate);
        _paintLayer.removeEventListener(MouseEvent.ROLL_OUT, handleSelectEnd);

        var r :Rectangle = updateSelection(layerPoint(_hudLayer, event));
        _cropPoint = null;

        if (r != null) {
            setWorkingArea(r);
            clearSelection();
        }
    }

    protected function updateSelection (p :Point) :Rectangle
    {
        var r :Rectangle = new Rectangle(Math.min(p.x, _cropPoint.x), Math.min(p.y, _cropPoint.y),
            Math.abs(p.x - _cropPoint.x), Math.abs(p.y - _cropPoint.y));

        if (r.width == 0 || r.height == 0) {
            dispatchWorkingAreaSelection();
            clearSelection();
            return null;
        }

        _crop.x = r.x;
        _crop.y = r.y;

        var g :Graphics = _crop.graphics;
        g.clear();
        g.lineStyle(1);
        GraphicsUtil.dashRect(g, 0, 0, r.width, r.height)

        dispatchEvent(new ValueEvent(SELECTION_CHANGE, [ r.width, r.height ]));

        return r;
    }

    protected function clearSelection () :void
    {
        if (_forceCrop) { // just reset it
            _crop.x = 0;
            _crop.y = 0;

        } else { // actually clear it
            _crop.graphics.clear();
        }
    }

    protected function handleMoveStart (event :MouseEvent) :void
    {
        _movePoint = new Point(_paintLayer.x, _paintLayer.y);
        _paintLayer.startDrag(false);
    }

    protected function handleMoveEnd (event :MouseEvent) :void
    {
        _paintLayer.stopDrag();
        _movePoint.x = _paintLayer.x - _movePoint.x;
        _movePoint.y = _paintLayer.y - _movePoint.y;
        paintLayerPositioned();
        pushUndo([ _paintLayer, _movePoint ]);
        _movePoint = null;
    }

    /** 
     * Sets that we've painted on the image.
     */
    protected function setModified () :void
    {
        // we just clear the objects that might be used to short-cut a return object
        _bitmapData = null;
        _bytes = null;
    }

    protected function pushUndo (undoObject :Object) :void
    {
        setModified();

        _undoStack.push(undoObject);
        _redoStack.length = 0;
        fireUndoRedoChange();
    }

    protected function doUndoRedo (undo :Boolean) :void
    {
        // shift the object from one stack to another
        var obj :Object = (undo ? _undoStack : _redoStack).pop();
        (undo ? _redoStack : _undoStack).push(obj);

        // make the change
        if (obj is Shape) {
            var paint :Shape = obj as Shape;
            if (undo){
                _paintLayer.removeChild(paint);
            } else {
                _paintLayer.addChildAt(paint, _paintLayer.numChildren - _paintInsertionOffset);
            }

        } else if (obj is Array) {
            var layer :DisplayObject = obj[0] as DisplayObject;
            var offset :Point = obj[1] as Point;
            if (undo) {
                layer.x -= offset.x;
                layer.y -= offset.y;
            } else {
                layer.x += offset.x;
                layer.y += offset.y;
            }
            paintLayerPositioned();
        }

        // notify watchers that the undo/redo stacks have changed
        fireUndoRedoChange();
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

    protected var _brushCursor :Shape;
    protected var _dropperCursor :DropperCursor = new DropperCursor();
    protected var _moveCursor :Sprite = new Sprite();
    protected var _selectCursor :Sprite = new Sprite();

    protected var _cursor :DisplayObject;
    protected var _cursorLayer :DisplayObject;

    protected var _cropPoint :Point;

    protected var _workingArea :Rectangle = new Rectangle();

    protected var _paintPoint :Point;
    protected var _movePoint :Point;

    protected var _scale :Number = 1;

    protected var _paintInsertionOffset :int;

    protected var _mode :int;
    protected var _color :uint;
    protected var _brushSize :Number = 1;
    protected var _brushCircle :Boolean = true;
    protected var _forceCrop :Boolean = false;

    /** The size of the horizontal gutter. */
    protected var _hGutter :int;

    /** The size of the vertical gutter. */
    protected var _vGutter :int;

    /** The overall canvas width, sans scaling. */
    protected var _canvasWidth :int;

    /** The overall canvas height, sans scaling. */
    protected var _canvasHeight :int;

    /** The minimum number of pixels around the image that we provide as "working area". */
    protected static const MIN_GUTTER :int = 150;
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
        g.drawRect(10, -10, 20, 20);
        g.endFill();
    }
}
