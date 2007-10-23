//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Shape;

import mx.containers.Canvas;
import mx.core.UIComponent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.msoy.client.MsoyPlaceView;

/**
 * A component that holds our place views and sets up a mask to ensure that the place view does not
 * render outside the box's bounds.
 */
public class PlaceBox extends Canvas
{
    public function PlaceBox ()
    {
        mask = (_mask = new Shape());
        rawChildren.addChild(_mask);
    }

    public function getPlaceView () :PlaceView
    {
        return _placeView;
    }

    public function usurpsControlBar () :Boolean
    {
        return (_placeView is MsoyPlaceView) && (_placeView as MsoyPlaceView).usurpsControlBar();
    }

    public function setPlaceView (view :PlaceView) :void
    {
        // throw an exception now if it's not a display object
        var disp :DisplayObject = DisplayObject(view);

        clearPlaceView(null);
        _placeView = view;

        Log.getLog(this).debug("Setting place view : " + disp);

        if (disp is UIComponent) {
            addChildAt(disp, 0);
        } else {
            rawChildren.addChildAt(disp, 0);
            if (_placeView is MsoyPlaceView) {
                MsoyPlaceView(_placeView).setPlaceSize(_mask.width, _mask.height);
            }
        }
    }

    public function clearPlaceView (view :PlaceView) :void
    {
        if ((_placeView != null) && (view == null || view == _placeView)) {
            var disp :DisplayObject = DisplayObject(_placeView);
            if (disp is UIComponent) {
                removeChild(disp);
            } else {
                rawChildren.removeChild(disp);
            }
            _placeView = null;
        }
    }

    /**
     * Adds a display object to overlay the main view as it changes.
     */
    public function addOverlay (overlay :DisplayObject) :void
    {
        if (overlay is UIComponent) {
            addChild(overlay);
        } else {
            rawChildren.addChild(overlay);
        }
    }

    /**
     * Removes a previously added overlay.
     */
    public function removeOverlay (overlay :DisplayObject) :void
    {
        if (overlay is UIComponent) {
            removeChild(overlay);
        } else {
            rawChildren.removeChild(overlay);
        }
    }

    /**
     * This must be called on when our size is changed to allow us update our PlaceView mask and
     * resize the PlaceView itself.
     */
    public function wasResized (width :int, height :int) :void
    {
        _mask.graphics.clear();
        _mask.graphics.beginFill(0xFFFFFF);
        _mask.graphics.drawRect(0, 0, width, height);
        _mask.graphics.endFill();

        for (var ii :int = 0; ii < this.rawChildren.numChildren; ii ++) {
            var child :DisplayObject = this.rawChildren.getChildAt(ii);

            if (child is UIComponent) {
                UIComponent(child).setActualSize(width, height);

            } else if (child is PlaceLayer) {
                PlaceLayer(child).setPlaceSize(width, height);

            } else if (child == _placeView) {
                Log.getLog(this).warning("PlaceView is not a MsoyPlaceView or an UIComponent.");

            } else {
                // don't over-police the contents of rawChildren
            }
        }
        }

    /** The mask configured on the PlaceView so that it doesn't overlap our other components. */
    protected var _mask :Shape;

    /** The current place view. */
    protected var _placeView :PlaceView;
}
}
