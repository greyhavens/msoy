//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.geom.ColorTransform;

import mx.core.UIComponent;

import mx.states.State;

import mx.styles.StyleManager;

import com.threerings.msoy.client.DeploymentConfig;

import com.threerings.flex.LoadedAsset;

/** The image used to be the buttons skin. Is automatically lightened/darkened/offset. */
[Style(name="image")]

/** A color with which to highlight the button. Color names may be used. */
[Style(name="highlight")]

/** The highlight alpha, a Number between 0 and 1. */
[Style(name="highlightAlpha")]

/**
 * A stateful button skin. Uses one image and modifies colors and offsets to indicate state.
 */
public class ImageButtonSkin extends UIComponent
{
    public function ImageButtonSkin ()
    {
        // register our states
        states = [ "up", "over", "down", "disabled", "selectedUp", "selectedOver", "selectedDown",
            "selectedDisabled" ].map(
            function (stName :String, ... rest) :State {
                var s :State = new State();
                s.name = stName;
                return s;
            });
    }

    override public function styleChanged (styleProp :String) :void
    {
        super.styleChanged(styleProp);
        switch (styleProp) {
        case "image":
            readImageFromStyle();
            break;

        case "highlight":
        case "highlightAlpha":
            readHightlightFromStyle();
            break;
        }
    }

    override public function stylesInitialized () :void
    {
        super.stylesInitialized();
        readImageFromStyle();
        readHightlightFromStyle();
    }

    override public function set currentState (newState :String) :void
    {
        super.currentState = newState;
        updateDisplayedState();
    }

    override public function invalidateSize () :void
    {
        super.invalidateSize();

        if (_image is LoadedAsset && parent != null) {
            parent.width = LoadedAsset(_image).measuredWidth;
            parent.height = LoadedAsset(_image).measuredHeight;
        }
    }

    /**
     * Re-read the highlight out of our style specification.
     */
    protected function readHightlightFromStyle () :void
    {
        graphics.clear();
        var color :uint = StyleManager.getColorName(getStyle("highlight"));
        if (color != StyleManager.NOT_A_COLOR) {
            var alpha :Number = getStyle("highlightAlpha") || 1;
            graphics.beginFill(color, alpha);
            graphics.drawRoundRect(0, 0, width, height, 4, 4);
            graphics.endFill();
        }
    }

    /**
     * Re-read the image out of our style specification.
     */
    protected function readImageFromStyle () :void
    {
        // the "image" style may be a class or a DisplayObject
        var rsrc :* = getStyle("image");

        if (rsrc is Class) {
            // instantiate it
            rsrc = new (Class(rsrc))();

        } else if (rsrc is String) {
            // If it's a string, treat it like an URL
            rsrc = new LoadedAsset(DeploymentConfig.serverURL + rsrc);
        }

        setImage(rsrc as DisplayObject);
    }

    protected function setImage (image :DisplayObject) :void
    {
        if (_image != null) {
            removeChild(_image);
        }
        _image = image;
        if (_image != null) {
            addChild(_image);
        }

        invalidateSize();
        updateDisplayedState();
    }

    /**
     * Update the state we're displaying with this skin.
     */
    protected function updateDisplayedState () :void
    {
        var state :String = currentState;
        if (state == null || _image == null) {
            return; // not yet set up
        }

        state = state.toLowerCase();
        if (state.indexOf("disabled") != -1) {
            // Darken on 'disabled' states
            _image.transform.colorTransform = new ColorTransform(0.5, 0.5, 0.5);
        } else if (state.indexOf("over") != -1 || state.indexOf("down") != -1) {
            // Brighten on 'over' and 'down' states
            _image.transform.colorTransform = new ColorTransform(1.25, 1.25, 1.25);
        } else {
            _image.transform.colorTransform = new ColorTransform();
        }

        // Translate if pressed
        if (state.indexOf("down") != -1 || state.indexOf("selected") != -1) {
            _image.y = 1;
        } else {
            _image.y = 0;
        }
    }

    override protected function measure () :void
    {
        if (_image == null) {
            super.measure();

        } else {
            measuredWidth = _image.width;
            measuredHeight = _image.height + 1;
        }
    }

    /** The image we're using for all states of the skin. */
    protected var _image :DisplayObject;
}
}
