//
// $Id$

package com.threerings.msoy.applets.image {

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.Graphics;
import flash.display.LoaderInfo;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.ErrorEvent;
import flash.events.Event;
import flash.events.IOErrorEvent;

import flash.utils.ByteArray;

import mx.controls.Spacer;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.VBox;

import mx.core.FlexLoader;
import mx.core.ScrollPolicy;

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
        _imageBox = new Canvas();
        _imageBox.maxWidth = MAX_WIDTH;
        _imageBox.maxHeight = MAX_HEIGHT;
        _imageBox.addChild(_editor = new EditCanvas());
        _editor.addEventListener(EditCanvas.SIZE_KNOWN, dispatchEvent);

        var mask :Shape = new Shape();
        mask.graphics.beginFill(0xFFFFFF);
        mask.graphics.drawRect(0, 0, MAX_WIDTH, MAX_HEIGHT);
        mask.graphics.endFill();
        _editor.mask = mask;
        _imageBox.rawChildren.addChild(mask);

        addChild(_imageBox);
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

        // TODO add zoom in and out buttons
        bar.addChild(new CommandButton("+", changeZoom, true));
        bar.addChild(new CommandButton("-", changeZoom, false));

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

    protected function changeZoom (zoomIn :Boolean) :void
    {
        if (zoomIn) {
            _editor.scaleX += .05;
            _editor.scaleY += .05;
        } else {
            _editor.scaleX -= .05;
            _editor.scaleY -= .05;
        }
    }

    protected var _controlBar :VBox;

    protected var _imageBox :Canvas;

    protected var _editor :EditCanvas;

    protected var _buttons :Array = [];

    protected static const SELECT :int = 0;
    protected static const MOVE :int = 1;
}
}
