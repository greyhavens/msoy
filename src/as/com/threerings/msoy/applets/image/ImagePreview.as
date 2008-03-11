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

import mx.controls.HSlider;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.VBox;

import mx.core.ScrollPolicy;

import mx.events.SliderEvent;

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


    public function ImagePreview (cutWidth :Number = NaN, cutHeight :Number = NaN)
    {
        if (!isNaN(cutWidth) && !isNaN(cutHeight)) {
            // TODO: set up the cutter-outter
        }

        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;

        setStyle("backgroundColor", 0xCCCCCC);
        _editor = new EditCanvas(300, 300);
        _editor.addEventListener(EditCanvas.SIZE_KNOWN, dispatchEvent);

        addChild(_editor);
        addChild(_controlBar = createControlBar());
        setImage(null);
    }

    public function setImage (image :Object) :void
    {
        _editor.setImage(image);

        var showControls :Boolean = (image != null);
        _controlBar.includeInLayout = showControls;
        _controlBar.visible = showControls;
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
        bar.width = 100;
        bar.horizontalScrollPolicy = ScrollPolicy.OFF;
        bar.verticalScrollPolicy = ScrollPolicy.OFF;

        // TODO: add a scrollbox

        bar.addChild(addMode("select", SELECT));
        bar.addChild(addMode("move", MOVE));

        // TODO: reset zoom?
        bar.addChild(_zoomSlider = new HSlider());
        _zoomSlider.liveDragging = true;
        _zoomSlider.minimum = .01;
        _zoomSlider.maximum = 10;
        _zoomSlider.value = 1;
        _zoomSlider.tickValues = [ 1 ];
        _zoomSlider.addEventListener(SliderEvent.CHANGE, handleZoomChange);

        // TODO: reset rotation
        bar.addChild(_rotSlider = new HSlider());
        _rotSlider.liveDragging = true;
        _rotSlider.minimum = -180;
        _rotSlider.maximum = 180;
        _rotSlider.value = 0;
        _rotSlider.tickValues = [ 0 ];
        _rotSlider.addEventListener(SliderEvent.CHANGE, handleRotChange);

        return bar;
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
    }

    protected function handleZoomChange (event :SliderEvent) :void
    {
        _editor.setZoom(event.value);
    }

    protected function handleRotChange (event :SliderEvent) :void
    {
        _editor.setRotation(event.value);
    }

    protected var _controlBar :VBox;

    protected var _imageBox :Canvas;

    protected var _editor :EditCanvas;

    protected var _zoomSlider :HSlider;
    protected var _rotSlider :HSlider;

    protected var _buttons :Array = [];

    protected static const SELECT :int = 0;
    protected static const MOVE :int = 1;
}
}
