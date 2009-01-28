//
// $Id$

package com.threerings.msoy.applets.image {

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

import mx.core.ScrollPolicy;

import mx.controls.scrollClasses.ScrollBar;

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
 * Dispatched when the scale has changed.
 * value - new scale
 */
[Event(name="ScaleChanged", type="com.threerings.util.ValueEvent")]

/**
 * Dispatched when the rotation has changed.
 * value - new rotation
 */
[Event(name="RotationChanged", type="com.threerings.util.ValueEvent")]

/**
 * Allows primitive editing of an image. Note that this is merely the model/view, the
 * controller is ImageManipulator.
 */
// TODO
// - If you paint a fat stroke right next to the scrollbar, you can actually paint onto
//   a portion of the image that's offscreen. Fix so that _curPaint has a mask of the currently
//   viewable area...?

// NOTES
// - when scaling / zooming, preserve current center
// - anchor working area to top-left when adjustments are made.
public class EditCanvas extends DisplayCanvas
{
    public static const SIZE_KNOWN :String = DisplayCanvas.SIZE_KNOWN;

    // Event constants
    public static const COLOR_SELECTED :String = "ColorSelected";
    public static const UNDO_REDO_CHANGE :String = "UndoRedoChange";
    public static const SELECTION_CHANGE :String = "SelChange";
    public static const SCALE_CHANGED :String = "ScaleChanged";
    public static const ROTATION_CHANGED :String = "RotationChanged";

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

    public function EditCanvas (maxW :int, maxH :int, sizeRestrict :SizeRestriction = null)
    {
        super(maxW, maxH);
        _sizeRestrict = sizeRestrict;

        width = maxW;
        height = maxH;
        horizontalScrollPolicy = ScrollPolicy.ON;
        verticalScrollPolicy = ScrollPolicy.ON;

        _captureLayer = new Sprite();
        _paintLayer = new Sprite();
        _hudLayer = new Sprite();

        _hudLayer.mouseEnabled = false;
        _captureLayer.mouseEnabled = false;
        _paintLayer.blendMode = BlendMode.LAYER;

        _crop = new Shape();
        _brushCursor = new Shape();
//        _eraseOutline = new Shape();
        _brushCursor.visible = false;
        _dropperCursor.visible = false;
//        _eraseOutline.visible = false;
        _moveCursor.visible = false;
        _moveCursor.mouseEnabled = false;
        _selectCursor.visible = false;
        _selectCursor.mouseEnabled = false;

        _captureLayer.addChild(_paintLayer);
        _baseLayer.addChild(_captureLayer);
        _baseLayer.addChild(_hudLayer);

        _hudLayer.addChild(_crop);
        _hudLayer.addChild(_dropperCursor);
        _hudLayer.addChild(_moveCursor);
        _hudLayer.addChild(_selectCursor);

        _paintLayer.addChild(_brushCursor);
//        _paintLayer.addChild(_eraseOutline);
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

    public function setDarkBackground (dark :Boolean) :void
    {
        _holder.setDarkBackground(dark);
    }

    /**
     * Update the working area size from numbers entered by the user.
     */
    public function updateWorkingSize (wid :Number, hei :Number) :void
    {
        _workingArea.width = wid;
        _workingArea.height = hei;
        setWorkingArea(_workingArea);
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
        if (forceFormat == null && _sizeRestrict == null && _bytes != null) {
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
        if (_bitmapData != null && _sizeRestrict == null) {
            return _bitmapData;
        }

        var bmp :BitmapData = new BitmapData(_workingArea.width, _workingArea.height, true, 0);
        var p :Point = _captureLayer.globalToLocal(_baseLayer.localToGlobal(_workingArea.topLeft));
        var matrix :Matrix = new Matrix(1, 0, 0, 1, -p.x, -p.y);

        // We have to have the brush on the image layer so that it participates in rotataions
        var brushVis :Boolean = _brushCursor.visible;
//        var eraseVis :Boolean = _eraseOutline.visible;
        _brushCursor.visible = false;
//        _eraseOutline.visible = false;
        // screenshot the image
        try {
            bmp.draw(_captureLayer, matrix);
        } finally {
            _brushCursor.visible = brushVis;
//            _eraseOutline.visible = eraseVis;
        }

        return bmp;
    }

    public function setRotation (rotation :Number) :void
    {
        if (rotation == _paintLayer.rotation) {
            return;
        }

        if (getTopOfUndo() is RotateAction) {
            // we already have a rotate there, just execute directly. This wasn't
            // supposed to be so hacky, but we don't want to undo each change of a dragging change
            _paintLayer.rotation = rotation;

        } else {
            var action :RotateAction = new RotateAction(rotation);
            action.doAction(_paintLayer);
            pushUndo(action);
        }
        paintLayerPositioned();
    }

    public function setZoom (zoom :Number) :void
    {
        _holder.setZoom(zoom);
    }

    public function setScale (scale :Number) :void
    {
        if (scale == _paintLayer.scaleX) {
            return;
        }

        if (getTopOfUndo() is ScaleAction) {
            // we already have a scale there, just execute directly. This wasn't
            // supposed to be so hacky, but we don't want to undo each change of a dragging change
            _paintLayer.scaleX = scale;
            _paintLayer.scaleY = scale;

        } else {
            var action :ScaleAction = new ScaleAction(scale);
            action.doAction(_paintLayer);
            pushUndo(action);
        }
        paintLayerPositioned();
        updateBrush();
    }

    override protected function updateCanvasSize () :void
    {
        // We DON'T call super

        var ww :Number = _imgWidth;
        var hh :Number = _imgHeight;

        // if there's a forced size, take that into account now
        if (_sizeRestrict != null && _sizeRestrict.forced != null) {
            ww = _sizeRestrict.forced.x;
            hh = _sizeRestrict.forced.y;
        }

        // recenter the image within the paint layer
        if (_image != null) {
            _image.x = ww / -2;
            _image.y = hh / -2;
        }

        _hGutter = Math.max(MIN_GUTTER, (this.maxWidth - ScrollBar.THICKNESS - ww) / 2);
        _vGutter = Math.max(MIN_GUTTER, (this.maxHeight - ScrollBar.THICKNESS - hh) / 2);
        _canvasWidth = ww + (2 * _hGutter);
        _canvasHeight = hh + (2 * _vGutter);
        _holder.width = _canvasWidth;
        _holder.height = _canvasHeight;

        // if there are max sizes, bound the selection size in, but do not recenter
        if (_sizeRestrict != null && _sizeRestrict.forced == null) {
            if (!isNaN(_sizeRestrict.maxWidth)) {
                ww = Math.min(ww, _sizeRestrict.maxWidth);
            }
            if (!isNaN(_sizeRestrict.maxHeight)) {
                hh = Math.min(hh, _sizeRestrict.maxHeight);
            }
        }

        // set up the working area
        var r :Rectangle = new Rectangle(_hGutter, _vGutter, ww, hh);
        setWorkingArea(r);

        // put the paint layer at the center???
        _paintLayer.x = _canvasWidth/2;
        _paintLayer.y = _canvasHeight/2;

        // color some layers so we can click on them
        paintLayerPositioned();

        // jiggle the canvas width. See notes in super.updateCanvasSize()
        this.width = this.maxWidth;
        this.height = this.maxHeight;
    }

    /**
     * Called after the paint layer's position is changed, updates mouse-grabbable
     * transparent drawn pixels.
     */
    protected function paintLayerPositioned () :void
    {
        setModified();

        var g :Graphics = _paintLayer.graphics;
        g.clear();
        g.beginFill(0xFFFFFF, 0);
        var p :Point;
        p = _paintLayer.globalToLocal(_baseLayer.localToGlobal(new Point(0, 0)));
        var startP :Point = p;
        g.moveTo(p.x, p.y);

        p = _paintLayer.globalToLocal(_baseLayer.localToGlobal(new Point(_canvasWidth, 0)));
        g.lineTo(p.x, p.y);

        p = _paintLayer.globalToLocal(_baseLayer.localToGlobal(
            new Point(_canvasWidth, _canvasHeight)));
        g.lineTo(p.x, p.y);

        p = _paintLayer.globalToLocal(_baseLayer.localToGlobal(new Point(0, _canvasHeight)));
        g.lineTo(p.x, p.y);

        g.lineTo(startP.x, startP.y);
        g.endFill();
    }

    protected function setWorkingArea (r :Rectangle) :void
    {
        // bound the working area into reasonable values.
        r.width = ImageUtil.normalizeImageDimension(r.width);
        r.height = ImageUtil.normalizeImageDimension(r.height);

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
        dispatchEvent(
            new ValueEvent(SELECTION_CHANGE, [ _workingArea.width, _workingArea.height ]));
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

//        // ERASE
//        on = (_mode == ERASE);
//        fn(MouseEvent.MOUSE_MOVE, handleEraseCursorMove);
//        fn(MouseEvent.ROLL_OVER, handleEraseCursorVis);
//        fn(MouseEvent.ROLL_OUT, handleEraseCursorVis);
//        _eraseOutline.visible = on;

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
        var radius :Number = (_brushSize/2) / _paintLayer.scaleX;
        if (_brushCircle) {
            g.drawCircle(0, 0, radius);
        } else {
            g.drawRect(-radius, -radius, radius * 2, radius * 2);
        }
        g.endFill();
//
//        g = _eraseOutline.graphics;
//        g.clear();
//        g.lineStyle(1, 0x0000FF, .5);
//        if (_brushCircle) {
//            g.drawCircle(0, 0, radius);
//        } else {
//            g.drawRect(-radius, -radius, radius * 2, radius * 2);
//        }
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

//    protected function handleEraseCursorMove (event :MouseEvent) :void
//    {
//        _eraseOutline.x = _brushCursor.x;
//        _eraseOutline.y = _brushCursor.y;
//    }
//
//    protected function handleEraseCursorVis (event :MouseEvent) :void
//    {
//        _eraseOutline.visible = (event.type == MouseEvent.ROLL_OVER);
//    }

    /**
     * The dropper cursor needs an extra special step to update its displayed color.
     */
    protected function handleDropperMove (event :MouseEvent) :void
    {
        var p :Point = layerPoint(_paintLayer, event);
        var value :uint = getDropperColor(p);
        var color :uint = (value & 0xFFFFFF);
        var alpha :Number = ((value >> 24) & 0xFF) / 255;
        _dropperCursor.setColor(color, alpha);
    }

    protected function handleDropperClick (event :MouseEvent) :void
    {
        var p :Point = layerPoint(_paintLayer, event);
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
        var matrix :Matrix = new Matrix(1, 0, 0, 1, -p.x, -p.y)
        bmp.draw(_paintLayer, matrix);

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
            g.lineStyle(_brushSize / _paintLayer.scaleX, _color, 1, false, LineScaleMode.NORMAL,
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
            const radius :Number = (_brushSize/2) / _paintLayer.scaleX;
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
        _cropPoint = snapPoint(layerPoint(_hudLayer, event));
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
            _crop.graphics.clear();
        }
    }

    protected function updateSelection (p :Point) :Rectangle
    {
        snapPoint(p);
        var r :Rectangle = new Rectangle(Math.min(p.x, _cropPoint.x), Math.min(p.y, _cropPoint.y),
            Math.abs(p.x - _cropPoint.x), Math.abs(p.y - _cropPoint.y));

        if (_sizeRestrict != null) {
            if (!isNaN(_sizeRestrict.maxWidth)) {
                r.width = Math.min(_sizeRestrict.maxWidth, r.width);
            }
            if (!isNaN(_sizeRestrict.maxHeight)) {
                r.height = Math.min(_sizeRestrict.maxHeight, r.height);
            }
        }

        _crop.graphics.clear();

        if (r.width == 0 || r.height == 0) {
            dispatchWorkingAreaSelection();
            return null;
        }

        _crop.x = r.x;
        _crop.y = r.y;

        var g :Graphics = _crop.graphics;
        g.lineStyle(1);
        GraphicsUtil.dashRect(g, 0, 0, r.width, r.height)

        dispatchEvent(new ValueEvent(SELECTION_CHANGE, [ r.width, r.height ]));

        return r;
    }

    /**
     * Modify the specified point to be snapped to the nearest pixel.
     * @return the same point.
     */
    protected function snapPoint (p :Point) :Point
    {
        p.x = Math.round(p.x);
        p.y = Math.round(p.y);
        return p;
    }

    protected function handleMoveStart (event :MouseEvent) :void
    {
        _movePoint = new Point(_paintLayer.x, _paintLayer.y);
        _paintLayer.startDrag(false);

        _paintLayer.stage.addEventListener(MouseEvent.MOUSE_UP, handleMoveEnd);
    }

    protected function handleMoveEnd (event :MouseEvent) :void
    {
        if (_movePoint == null) {
            return;
        }
        _paintLayer.stopDrag();
        _paintLayer.stage.removeEventListener(MouseEvent.MOUSE_UP, handleMoveEnd);
        _movePoint.x = _paintLayer.x - _movePoint.x;
        _movePoint.y = _paintLayer.y - _movePoint.y;
        paintLayerPositioned();
        pushUndo(new MoveAction(_movePoint));
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

    protected function getTopOfUndo () :Object
    {
        return (_undoStack.length == 0) ? null : _undoStack[_undoStack.length - 1];
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

        } else if (obj is PaintLayerAction) {
            var action :PaintLayerAction = obj as PaintLayerAction;
            var event :Event = action.doAction(_paintLayer, undo);
            paintLayerPositioned();
            updateBrush();
            if (event != null) {
                dispatchEvent(event);
            }
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

    /** If non-null, defines the maximum boundaries of our resulting image. */
    protected var _sizeRestrict :SizeRestriction;

    /** Layers that contain things. */
    protected var _captureLayer :Sprite;
    protected var _paintLayer :Sprite;
    protected var _hudLayer :Sprite;

    /** Paint layers. */
    protected var _undoStack :Array = [];

    protected var _redoStack :Array = [];

    /** The current paint layer. */
    protected var _curPaint :Shape;

    /** Used to draw the current selection while selecting. */
    protected var _crop :Shape;

    protected var _brushCursor :Shape;
//    protected var _eraseOutline :Shape;
    protected var _dropperCursor :DropperCursor = new DropperCursor();
    protected var _moveCursor :Sprite = new Sprite();
    protected var _selectCursor :Sprite = new Sprite();

    protected var _cursor :DisplayObject;
    protected var _cursorLayer :DisplayObject;

    protected var _cropPoint :Point;

    protected var _workingArea :Rectangle = new Rectangle();

    protected var _paintPoint :Point;
    protected var _movePoint :Point;

    protected var _paintInsertionOffset :int;

    protected var _mode :int;
    protected var _color :uint;
    protected var _brushSize :Number = 1;
    protected var _brushCircle :Boolean = true;

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
import flash.display.Sprite;

import flash.events.Event;

import flash.geom.Point;

import com.threerings.util.ValueEvent;

import com.threerings.msoy.applets.image.EditCanvas;

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

/* abstract */ interface PaintLayerAction
{
    /**
     * Do the specified action, return an event if it should then be dispatched.
     */
    function doAction (paintLayer :Sprite, undo :Boolean = false) :Event;
}

class MoveAction implements PaintLayerAction
{
    public function MoveAction (delta :Point)
    {
        _p = delta;
    }

    public function doAction (paintLayer :Sprite, undo :Boolean = false) :Event
    {
        var factor :int = undo ? -1 : 1;
        paintLayer.x += _p.x * factor;
        paintLayer.y += _p.y * factor;
        return null;
    }

    protected var _p :Point;
}

class RotateAction implements PaintLayerAction
{
    public function RotateAction (newRotation :Number)
    {
        _r = newRotation;
    }

    public function doAction (paintLayer :Sprite, undo :Boolean = false) :Event
    {
        // ignore the undo/redo and just assume it'll all be right
        var newRot :Number = _r;
        _r = paintLayer.rotation;
        paintLayer.rotation = newRot;
        return new ValueEvent(EditCanvas.ROTATION_CHANGED, newRot);
    }

    protected var _r :Number;
}

class ScaleAction implements PaintLayerAction
{
    public function ScaleAction (newScale :Number)
    {
        _s = newScale;
    }

    public function doAction (paintLayer :Sprite, undo :Boolean = false) :Event
    {
        // ignore the undo/redo and just assume it'll all be right
        var newScale :Number = _s;
        _s = paintLayer.scaleX;
        paintLayer.scaleX = newScale;
        paintLayer.scaleY = newScale;
        return new ValueEvent(EditCanvas.SCALE_CHANGED, newScale);
    }

    protected var _s :Number;
}
