//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Graphics;
import flash.display.LoaderInfo;
import flash.display.Sprite;

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;
import flash.events.TextEvent;

import flash.ui.Keyboard;

import flash.utils.ByteArray;

import mx.controls.ColorPicker;
import mx.controls.HSlider;
import mx.controls.Label;
import mx.controls.Text;
import mx.controls.TextInput;

import mx.containers.Canvas;
import mx.containers.Grid;
import mx.containers.HBox;
import mx.containers.VBox;

import mx.core.Container;
import mx.core.ScrollPolicy;

import mx.events.ColorPickerEvent;
import mx.events.FlexEvent;
import mx.events.SliderEvent;

import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.GridUtil;
import com.threerings.flex.KeyboardManager;
import com.threerings.flex.ScrollBox;

/**
 * Dispatched when the size of the image is known.
 */
[Event(name="SizeKnown", type="com.threerings.util.ValueEvent")]

/**
 * Dispatched when the image manipulator is closed.
 * value- true or false, indicating save or cancel.
 */
[Event(name="close", type="com.threerings.util.ValueEvent")]

/**
 * Displays or allows editing of an image.
 */
public class ImageManipulator extends HBox
{
    public static const SIZE_KNOWN :String = EditCanvas.SIZE_KNOWN;

    public static const CLOSE :String = Event.CLOSE;

    public function ImageManipulator (
        maxW :int = 400, maxH :int = 400, cutWidth :Number = NaN, cutHeight :Number = NaN)
    {
        this.maxWidth = maxW;
        this.maxHeight = maxH;
        this.width = maxW;
        this.height = maxH;
        this.minWidth = 100;
        this.minHeight = 100;
        _maxWidth = maxW - CONTROL_BAR_WIDTH;
        _maxHeight = maxH;

        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;

        setStyle("backgroundColor", 0xDCDCDC);
        _editor = new EditCanvas(_maxWidth, _maxHeight);
        _editor.addEventListener(EditCanvas.SIZE_KNOWN, handleSizeKnown);

        addChild(_controlBar = createControlBar());
        addChild(_editor);
        setImage(null);

        if (!isNaN(cutWidth) && !isNaN(cutHeight)) {
            _editor.setForcedCrop(cutWidth, cutHeight);
            disableMode(EditCanvas.SELECT);
            setMode(EditCanvas.MOVE);
        } else {
            setMode(EditCanvas.PAINT);
        }
    }

    public function setImage (image :Object) :void
    {
        _editor.setImage(image);

        if (_controlBar != null) {
            var showControls :Boolean = (image != null);
            _controlBar.includeInLayout = showControls;
            _controlBar.visible = showControls;
        }
    }

    /**
     * Get the currently selected portion of the Image, as a ByteArray.
     *
     * @param asJpg if true, encode the image as a JPG if not already encoded.
     * @param quality (only applicable if asJpg is true) the quality setting for jpg encoding.
     *
     * @return an Array with the ByteArray as the first element, and if there's a forced
     * extension then that's the 2nd element.
     */
    public function getImage (forceFormat :String = null, formatArg :Object = null) :Array
    {
        return _editor.getImage(forceFormat, formatArg);
    }

    protected function createControlBar () :VBox
    {
        var bar :VBox = new VBox();
        bar.setStyle("paddingLeft", 0);
        bar.setStyle("paddingRight", 0);
        bar.width = CONTROL_BAR_WIDTH;
        bar.horizontalScrollPolicy = ScrollPolicy.OFF;
        bar.verticalScrollPolicy = ScrollPolicy.OFF;

        // TODO: add a scrollbox?

        createPositionControls(bar);
        createPaintControls(bar);
        createUndoControls(bar);

        var buts :HBox = new HBox();
        buts.percentWidth = 100;
        buts.setStyle("backgroundColor", 0x9AA1AA);
        buts.setStyle("paddingTop", 8);
        buts.setStyle("paddingBottom", 8);
        buts.addChild(new CommandButton("Save", doClose, true));
        buts.addChild(new CommandButton("Cancel", doClose, false));
        bar.addChild(buts);

        _editor.setBrushSize(10);
        _editor.setBrushShape(true); // circular
        _editor.setPaintColor(_colorPicker.selectedColor);

        _editor.addEventListener(EditCanvas.UNDO_REDO_CHANGE, handleUndoRedoChange);
        handleUndoRedoChange(null); // check now

        _editor.addEventListener(EditCanvas.COLOR_SELECTED, handleEyeDropper);

        return bar;
    }

    protected function createPositionControls (bar :VBox) :void
    {
        bar.addChild(createControlHeader("Position Image"));

        var box :HBox = new HBox();
        box.addChild(addModeBtn("move", EditCanvas.MOVE));
        box.addChild(createTip("Move, Scale, and Rotate the Image to fit the area"));
        bar.addChild(box);

        _scaleSlider = addSlider(bar, "Scale Image", .01, 10, 1, _editor.setScale);
        _rotSlider = addSlider(bar, "Rotate Image", -180, 180, 0, _editor.setRotation,
            [ -180, -90, 0, 90, 180 ]);

        // TODO: this will change to a different UI
        _zoomSlider = addSlider(bar, "Zoom", 1, 8, 1, _editor.setZoom,
            [ 1, 2, 3, 4, 5, 6, 7, 8 ], 1);

        box = new HBox();
        var crop :CommandButton = new CommandButton(null, _editor.doCrop);
        crop.styleName = "cropButton";
        box.addChild(crop);
        box.addChild(createTip("Crop the image to the selected region"));
        bar.addChild(box);

        box = new HBox();
        box.addChild(addModeBtn("select", EditCanvas.SELECT));

        var innerBox :HBox = new HBox();
        innerBox.setStyle("horizontalGap", 0);
        innerBox.addChild(_selectionWidth = new TextInput());
        var lbl :Label = new Label();
        lbl.text = "x";
        innerBox.addChild(lbl);
        innerBox.addChild(_selectionHeight = new TextInput());
        _selectionWidth.restrict = "0-9";
        _selectionHeight.restrict = "0-9";
        _selectionWidth.maxChars = 4;
        _selectionHeight.maxChars = 4;
        _selectionWidth.maxWidth = 40;
        _selectionHeight.maxWidth = 40;


        box.addChild(innerBox);

        bar.addChild(box);

        _editor.addEventListener(EditCanvas.SELECTION_CHANGE, handleSelectionChange);
        _selectionWidth.addEventListener(Event.CHANGE, handleSelectionTyped);
        _selectionHeight.addEventListener(Event.CHANGE, handleSelectionTyped);
    }

    protected function createPaintControls (bar :VBox) :void
    {
        bar.addChild(createControlHeader("Erase and Paint"));

        var box :HBox = new HBox();
        box.addChild(addModeBtn("eraser", EditCanvas.ERASE));
        box.addChild(createTip("Erase around the image. Paint your own touches!"));
        bar.addChild(box);

        box = new HBox();
        box.addChild(addModeBtn("brush", EditCanvas.PAINT));
        box.addChild(addModeBtn("eyedropper", EditCanvas.SELECT_COLOR));
        _colorPicker = new ColorPicker();
        _colorPicker.addEventListener(ColorPickerEvent.CHANGE, handleColorPicked);
        box.addChild(_colorPicker);
        bar.addChild(box);

        _brushSlider = addSlider(bar, "Brush Size", 1, 40, 10, _editor.setBrushSize,
            [ 1, 2, 5, 10, 20, 40 ]);
    }

    protected function createUndoControls (bar :VBox) :void
    {
        bar.addChild(createControlHeader("Undo Mistakes"));

        var box :HBox = new HBox();
        box.addChild(_undo = new CommandButton(null, _editor.doUndo));
        box.addChild(_redo = new CommandButton(null, _editor.doRedo));
        bar.addChild(box);

        _undo.styleName = "undoButton";
        _redo.styleName = "redoButton";
        KeyboardManager.setShortcut(_undo, 26/*should be: Keyboard.Z*/, Keyboard.CONTROL);
        KeyboardManager.setShortcut(_redo, 25/*should be: Keyboard.Y*/, Keyboard.CONTROL);
    }

    protected function createControlHeader (title :String) :HBox
    {
        var box :HBox = new HBox();
        box.percentWidth = 100;
        box.setStyle("backgroundColor", 0x6AB6E7);
        box.setStyle("horizontalAlign", "center");

        var lbl :Label = new Label();
        lbl.setStyle("color", 0xFFFFFF);
        lbl.setStyle("fontWeight", "bold");
        lbl.text = title;

        box.addChild(lbl);

        return box;
    }

    protected function createTip (text :String) :Text
    {
        var tip :Text = new Text();
        tip.selectable = false;
        tip.width = 100; // TODO: constant
        tip.setStyle("fontSize", 8);
        tip.text = text;
        return tip;
    }

    protected function addSlider (
        container :Container, name :String, min :Number, max :Number, value :Number,
        changeHandler :Function, tickValues :Array = null, snapInterval :Number = 0) :HSlider
    {
        if (tickValues == null) {
            tickValues = [ value ];
        }

        var hbox :HBox = new HBox();
        hbox.setStyle("horizontalGap", 0);
        hbox.setStyle("verticalAlign", "middle");

        var box :VBox = new VBox();
        box.horizontalScrollPolicy = ScrollPolicy.OFF;
        box.verticalScrollPolicy = ScrollPolicy.OFF;
        box.setStyle("horizontalAlign", "center");
        box.setStyle("verticalGap", 0);

        var lbl :Label = new Label();
        lbl.setStyle("fontSize", 8);
        lbl.text = name;

        var slider :HSlider = new HSlider();
        slider.maxWidth = 100;
        slider.liveDragging = true;
        slider.minimum = min;
        slider.maximum = max;
        slider.value = value;
        slider.tickValues = tickValues;
        slider.snapInterval = snapInterval;

        var changeListener :Function = function (event :Event) :void {
            changeHandler(slider.value);
        };

        slider.addEventListener(SliderEvent.CHANGE, changeListener);
        slider.addEventListener(FlexEvent.VALUE_COMMIT, changeListener);

        box.addChild(lbl);
        box.addChild(slider);

        hbox.addChild(box);
        container.addChild(hbox);

        if (snapInterval == 0) {
            var but :CommandButton = new CommandButton("Snap", function () :void {
                // snap it to the closest tickValue
                var closeValue :Number = value;
                var closeness :Number = Number.MAX_VALUE;

                const curValue :Number = slider.value;
                for each (var tickVal :Number in tickValues) {
                    var diff :Number = Math.abs(tickVal - curValue);
                    if (diff < closeness) {
                        closeness = diff;
                        closeValue = tickVal;
                    }
                }

                slider.value = closeValue;
            });
            but.scaleY = .8;
            but.scaleX = .8;
            hbox.addChild(but);
        }

        return slider;
    }

    protected function addModeBtn (styleBase :String, mode :int) :CommandButton
    {
        var but :CommandButton = new CommandButton(null, setMode, mode);
        but.data = mode;
        but.styleName = styleBase + "Button";
        but.toggle = true;
        _buttons.push(but);
        return but;
    }

    protected function setMode (mode :int) :void
    {
        for each (var but :CommandButton in _buttons) {
            but.selected = (mode == but.data);
        }
        _editor.setMode(mode);
    }

    protected function disableMode (mode :int) :void
    {
        for each (var but :CommandButton in _buttons) {
            if (mode == but.data) {
                but.enabled = false;
            }
        }
    }

    protected function handleUndoRedoChange (event :Event) :void
    {
        _undo.enabled = _editor.canUndo();
        _redo.enabled = _editor.canRedo();
    }

    protected function handleEyeDropper (event :ValueEvent) :void
    {
        _colorPicker.selectedColor = uint(event.value);
        setMode(EditCanvas.PAINT);
    }

    protected function handleColorPicked (event :ColorPickerEvent) :void
    {
        _editor.setPaintColor(event.color);
        setMode(EditCanvas.PAINT);
    }

    protected function handleSizeKnown (event :ValueEvent) :void
    {
        // redispatch
        dispatchEvent(event);

        // at the minimum zoom level we want the longest side to just fit
        if (_controlBar != null) {
            var w :Number = event.value[0];
            var h :Number = event.value[1];
            if (_zoomSlider != null) {
                _zoomSlider.minimum = Math.min(1, Math.min(_maxWidth / w, _maxHeight / h));
                _zoomSlider.value = 1;
            }
            _rotSlider.value = 0;
            _scaleSlider.value = 1;
        }
    }

    protected function doClose (save :Boolean) :void
    {
        dispatchEvent(new ValueEvent(CLOSE, save));
    }

    /**
     * Update the selection when a user types a new value.
     */
    protected function handleSelectionTyped (event :Event) :void
    {
        var w :Number = parseInt(_selectionWidth.text);
        var h :Number = parseInt(_selectionHeight.text);
        if (isNaN(w)) {
            w = 0;
        }
        if (isNaN(h)) {
            h = 0;
        }
        _editor.updateWorkingSize(w, h);
    }

    /**
     * Update the displayed selection size.
     */
    protected function handleSelectionChange (event :ValueEvent) :void
    {
        _selectionWidth.text = event.value[0];
        _selectionHeight.text = event.value[1]
    }

    protected static const CONTROL_BAR_WIDTH :int = 150;

    protected var _maxWidth :int;
    protected var _maxHeight :int;

    protected var _controlBar :VBox;

    protected var _imageBox :Canvas;

    protected var _editor :EditCanvas;

    protected var _colorPicker :ColorPicker;
    protected var _rotSlider :HSlider;
    protected var _scaleSlider :HSlider;
    protected var _zoomSlider :HSlider;
    protected var _brushSlider :HSlider;

    protected var _selectionWidth :TextInput;
    protected var _selectionHeight :TextInput;

    protected var _undo :CommandButton;
    protected var _redo :CommandButton;

    protected var _buttons :Array = [];
}
}
