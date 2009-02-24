//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObject;
import flash.geom.ColorTransform;

import mx.core.UIComponent;

import mx.states.State;

import com.threerings.msoy.client.DeploymentConfig;

import com.threerings.flash.MediaContainer;
import com.threerings.util.ValueEvent;

/** The image used to be the buttons skin. Is automatically lightened/darkened/offset. */
[Style(name="image")]

/**
 * A stateful button skin. Uses one image and modifies colors and offsets to indicate state.
 */
public class ImageButtonSkin extends UIComponent
{
    public function ImageButtonSkin ()
    {
        // register our states
        states = [ addState("up"), addState("over"), addState("down"), addState("disabled"),
            addState("selectedUp"), addState("selectedOver"), addState("selectedDown"),
            addState("selectedDisabled") ];
    }

    override public function styleChanged (styleProp :String) :void
    {
        super.styleChanged(styleProp);
        if (styleProp == "image") {
            readImageFromStyle();
        }
    }

    override public function stylesInitialized () :void
    {
        super.stylesInitialized();
        readImageFromStyle();
    }

    override public function set currentState (newState :String) :void
    {
        super.currentState = newState;
        updateDisplayedState();
    }

    /**
     * Re-read the image out of our style specification.
     */
    protected function readImageFromStyle () :void
    {
        // the "image" style may be a class or a DisplayObject
        var rsrc :* = getStyle("image");

        if (rsrc is String) {
            // If it's a string, treat it like an URL
            rsrc = new MediaContainer(DeploymentConfig.serverURL + rsrc);
            rsrc.addEventListener(MediaContainer.SIZE_KNOWN, function (event :ValueEvent) :void {
                parent.width = event.value[0];
                parent.height = event.value[1];
                setImage(rsrc as DisplayObject);
            });
        } else {
            if (rsrc is Class) {
                rsrc = new (Class(rsrc))();
            }

            setImage(rsrc as DisplayObject);
        }
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

    /** Convenience to create a State. */
    protected function addState (name :String) :State
    {
        var s :State = new State();
        s.name = name;
        return s;
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
