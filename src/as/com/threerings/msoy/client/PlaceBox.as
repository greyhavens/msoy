//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.display.Shape;
import flash.geom.Point;

import mx.containers.Canvas;
import mx.core.UIComponent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.msoy.client.MsoyPlaceView;

/**
 * A component that holds our place views and sets up a mask to ensure that the place view does not
 * render outside the box's bounds.
 */
public class PlaceBox extends LayeredContainer
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
        setBaseLayer(disp);
        _placeView = view;

        if (_placeView is MsoyPlaceView) {
            MsoyPlaceView(_placeView).setPlaceSize(_mask.width, _mask.height);
        }
    }

    public function clearPlaceView (view :PlaceView) :void
    {
        if ((_placeView != null) && (view == null || view == _placeView)) {
            clearBaseLayer();
            _placeView = null;
        }
    }

    /**
     * @return true if there are glyphs under the specified point.  If the glyph extends 
     * InteractiveObject and the glyph sprite has mouseEnabled == false, it is not checked.
     */
    public function overlaysMousePoint (stageX :Number, stageY :Number) :Boolean
    {
        var stagePoint :Point = new Point(stageX, stageY);
        for (var ii :int = 0; ii < this.rawChildren.numChildren; ii ++) {
            var child :DisplayObject = this.rawChildren.getChildAt(ii);
            if (child == _placeView) {
                continue;
            }
            if ((!(child is InteractiveObject) || (child as InteractiveObject).mouseEnabled) && 
                    child.hitTestPoint(stageX, stageY, true)) {
                return true;
            }
        }
        return false;
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
