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

import flash.utils.ByteArray;

import mx.controls.ColorPicker;
import mx.controls.HSlider;
import mx.controls.Label;

import mx.containers.Canvas;
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
import com.threerings.flex.ScrollBox;

/**
 * Dispatched when the size of the image is known.
 */
[Event(name="SizeKnown", type="com.threerings.util.ValueEvent")]

/**
 * Displays an image. I think this could be the base class for our in-line image editing.
 */
public class ImagePreview extends HBox
{
    public static const SIZE_KNOWN :String = EditCanvas.SIZE_KNOWN;

    public static const MAX_WIDTH :int = 300;
    public static const MAX_HEIGHT :int = 300;

    public function ImagePreview (
        allowEdit :Boolean = false, cutWidth :Number = NaN, cutHeight :Number = NaN)
    {
        if (!isNaN(cutWidth) && !isNaN(cutHeight)) {
            // TODO: set up the cutter-outter
        }

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
        setMode(allowEdit ? EditCanvas.PAINT : EditCanvas.NONE);
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
     */
    public function getImage (asJpg :Boolean = false, quality :Number = 50) :ByteArray
    {
        return _editor.getImage(asJpg, quality);
    }

    protected function createControlBar () :VBox
    {
        var bar :VBox = new VBox();
        bar.setStyle("paddingLeft", 0);
        bar.setStyle("paddingRight", 0);
        bar.width = 100;
        bar.horizontalScrollPolicy = ScrollPolicy.OFF;
        bar.verticalScrollPolicy = ScrollPolicy.OFF;

        // TODO: add a scrollbox

        var picker :ColorPicker = new ColorPicker();
        picker.addEventListener(ColorPickerEvent.CHANGE, handleColorPicked);

        bar.addChild(picker);
        bar.addChild(addMode("paint", EditCanvas.PAINT));
        bar.addChild(addMode("select", EditCanvas.SELECT));
        bar.addChild(addMode("move", EditCanvas.MOVE));

        bar.addChild(new CommandButton("Crop", _editor.doCrop));

        _rotSlider = addSlider(bar, "Rotation", -180, 180, 0, _editor.setRotation,
            [ -180, -90, 0, 90, 180 ]);
        _scaleSlider = addSlider(bar, "Scale", .01, 10, 1, _editor.setScale);
        _zoomSlider = addSlider(bar, "Zoom", 1, 10, 1, _editor.setZoom);
        _brushSlider = addSlider(bar, "Brush", 1, 40, 10, _editor.setBrushSize,
            [ 1, 2, 5, 10, 20, 40 ]);

        _editor.setBrushSize(10);
        _editor.setPaintColor(picker.selectedColor);

        // TEMP:
        _scaleSlider.enabled = false;

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

    protected function addMode (label :String, mode :int) :CommandButton
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

    protected function handleColorPicked (event :ColorPickerEvent) :void
    {
        _editor.setPaintColor(event.color);
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

    protected var _rotSlider :HSlider;
    protected var _scaleSlider :HSlider;
    protected var _zoomSlider :HSlider;
    protected var _brushSlider :HSlider;

    protected var _buttons :Array = [];
}
}
