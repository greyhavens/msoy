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

    public function setPlaceView (view :PlaceView) :void
    {
        // throw an exception now if it's not a display object
        var disp :DisplayObject = DisplayObject(view);

        clearPlaceView(null);
        _placeView = view;

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
     * This must be called on when our size is changed to allow us update our PlaceView mask and
     * resize the PlaceView itself.
     */
    public function wasResized (width :int, height :int) :void
    {
        _mask.graphics.clear();
        _mask.graphics.beginFill(0xFFFFFF);
        _mask.graphics.drawRect(0, 0, width, height);
        _mask.graphics.endFill();

        if (_placeView != null) {
            if (_placeView is UIComponent) {
                UIComponent(_placeView).setActualSize(width, height);
            } else if (_placeView is MsoyPlaceView) {
                MsoyPlaceView(_placeView).setPlaceSize(width, height);
            } else {
                Log.getLog(this).warning("PlaceView is not a MsoyPlaceView or an UIComponent.");
            }
        }
    }

    /** The mask configured on the PlaceView so that it doesn't overlap our other components. */
    protected var _mask :Shape;

    /** The current place view. */
    protected var _placeView :PlaceView;
}
}
