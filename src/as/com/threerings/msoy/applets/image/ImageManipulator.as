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

import flash.ui.Keyboard;

import flash.utils.ByteArray;

import mx.controls.ColorPicker;
import mx.controls.HSlider;
import mx.controls.Label;

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
 * Displays or allows editing of an image.
 */
public class ImageManipulator extends HBox
{
    public static const SIZE_KNOWN :String = EditCanvas.SIZE_KNOWN;

    public static const MAX_WIDTH :int = 300;
    public static const MAX_HEIGHT :int = 300;

    public function ImageManipulator (
        allowEdit :Boolean = false, cutWidth :Number = NaN, cutHeight :Number = NaN)
    {
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;

        setStyle("backgroundColor", 0xCCCCCC);
        _editor = new EditCanvas(MAX_WIDTH, MAX_HEIGHT);
        _editor.addEventListener(EditCanvas.SIZE_KNOWN, handleSizeKnown);

        addChild(_editor);
        if (allowEdit) {
            addChild(_controlBar = createControlBar());
        }
        setImage(null);

        if (!isNaN(cutWidth) && !isNaN(cutHeight)) {
            _editor.setForcedCrop(cutWidth, cutHeight);
            disableMode(EditCanvas.SELECT);
            setMode(EditCanvas.MOVE);
        } else {
            setMode(allowEdit ? EditCanvas.PAINT : EditCanvas.NONE);
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
        bar.width = 150;
        bar.horizontalScrollPolicy = ScrollPolicy.OFF;
        bar.verticalScrollPolicy = ScrollPolicy.OFF;

        // TODO: add a scrollbox

        _colorPicker = new ColorPicker();
        _colorPicker.addEventListener(ColorPickerEvent.CHANGE, handleColorPicked);

        var grid :Grid = new Grid();

        GridUtil.addRow(grid, _colorPicker, addModeBtn("eyedrop", EditCanvas.SELECT_COLOR));
        GridUtil.addRow(grid,
            addModeBtn("paint", EditCanvas.PAINT), addModeBtn("erase", EditCanvas.ERASE));
        GridUtil.addRow(grid,
            addModeBtn("select", EditCanvas.SELECT), addModeBtn("move", EditCanvas.MOVE));
        GridUtil.addRow(grid, new CommandButton("Crop", _editor.doCrop), [ 2, 1 ]);
        GridUtil.addRow(grid, _undo = new CommandButton("Undo", _editor.doUndo),
            _redo = new CommandButton("Redo", _editor.doRedo));

        KeyboardManager.setShortcut(_undo, 26/*should be: Keyboard.Z*/, Keyboard.CONTROL);
        KeyboardManager.setShortcut(_redo, 25/*should be: Keyboard.Y*/, Keyboard.CONTROL);

        bar.addChild(grid);

        _rotSlider = addSlider(bar, "Rotation", -180, 180, 0, _editor.setRotation,
            [ -180, -90, 0, 90, 180 ]);
        _scaleSlider = addSlider(bar, "Scale", .01, 10, 1, _editor.setScale);
        _zoomSlider = addSlider(bar, "Zoom", 1, 10, 1, _editor.setZoom);
        _brushSlider = addSlider(bar, "Brush", 1, 40, 10, _editor.setBrushSize,
            [ 1, 2, 5, 10, 20, 40 ]);

        _editor.setBrushSize(10);
        _editor.setBrushShape(true); // circular
        _editor.setPaintColor(_colorPicker.selectedColor);

        _editor.addEventListener(EditCanvas.UNDO_REDO_CHANGE, handleUndoRedoChange);
        handleUndoRedoChange(null); // check now

        _editor.addEventListener(EditCanvas.COLOR_SELECTED, handleEyeDropper);

        return bar;
    }

    protected function addSlider (
        container :Container, name :String, min :Number, max :Number, value :Number,
        changeHandler :Function, tickValues :Array = null) :HSlider
    {
        if (tickValues == null) {
            tickValues = [ value ];
        }

        var box :VBox = new VBox();
        box.horizontalScrollPolicy = ScrollPolicy.OFF;
        box.verticalScrollPolicy = ScrollPolicy.OFF;
        box.setStyle("verticalGap", 0);

        var hbox :HBox = new HBox();
        hbox.setStyle("horizontalGap", 0);

        var lbl :Label = new Label();
        lbl.setStyle("fontSize", 8);
        lbl.text = name;

        var slider :HSlider = new HSlider();
        slider.maxWidth = 130;
        slider.liveDragging = true;
        slider.minimum = min;
        slider.maximum = max;
        slider.value = value;
        slider.tickValues = tickValues;

        var changeListener :Function = function (event :Event) :void {
            changeHandler(slider.value);
        };

        slider.addEventListener(SliderEvent.CHANGE, changeListener);
        slider.addEventListener(FlexEvent.VALUE_COMMIT, changeListener);

        container.addChild(box);
        box.addChild(hbox);
        box.addChild(slider);

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
        hbox.addChild(lbl);
        hbox.addChild(but);

        return slider;
    }

    protected function addModeBtn (label :String, mode :int) :CommandButton
    {
        var but :CommandButton = new CommandButton(label, setMode, mode);
        but.data = mode;
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
            _zoomSlider.minimum = Math.min(1, Math.min(MAX_WIDTH / w, MAX_HEIGHT / h));
            _zoomSlider.value = 1;
            _rotSlider.value = 0;
            _scaleSlider.value = 1;
        }
    }

    protected var _controlBar :VBox;

    protected var _imageBox :Canvas;

    protected var _editor :EditCanvas;

    protected var _colorPicker :ColorPicker;
    protected var _rotSlider :HSlider;
    protected var _scaleSlider :HSlider;
    protected var _zoomSlider :HSlider;
    protected var _brushSlider :HSlider;

    protected var _undo :CommandButton;
    protected var _redo :CommandButton;

    protected var _buttons :Array = [];
}
}
