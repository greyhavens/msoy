//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.display.Shape;
import flash.geom.Point;
import flash.geom.Rectangle;

import mx.core.UIComponent;

import com.threerings.util.Log;

import com.threerings.display.DisplayUtil;

import com.threerings.crowd.client.PlaceView;
import com.threerings.msoy.client.MsoyPlaceView;
import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.room.client.RoomMetrics;

/**
 * A component that holds our place views and sets up a mask to ensure that the place view does not
 * render outside the box's bounds.
 */
public class PlaceBox extends LayeredContainer
{
    /** The layer priority of help text bubbles. */
    public static const LAYER_HELP_BUBBLES :int = 5;

    /** The layer priority of the loading spinner. */
    public static const LAYER_ROOM_SPINNER :int = 10;

    /** The layer priority of the AVRG panel. */
    public static const LAYER_AVRG_PANEL :int = 33;

    /** The layer priority of the scrolling chat. */
    public static const LAYER_CHAT_SCROLL :int = 20;

    /** The layer priority of the occupant List. */
    public static const LAYER_CHAT_LIST :int = 25;

    /** The layer priority of non-moving chat messages. */
    public static const LAYER_CHAT_STATIC :int = 30;

    /** The layer priority of history chat messages. */
    public static const LAYER_CHAT_HISTORY :int = 35;

    /** The layer priority of the trophy award, avatar intro, and chat tip. */
    public static const LAYER_TRANSIENT :int = 50;

    /** The layer priority of the clickable featured place overlay. */
    public static const LAYER_FEATURED_PLACE :int = 100;

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

    public function clearPlaceView (view :PlaceView) :Boolean
    {
        if ((_placeView != null) && (view == null || view == _placeView)) {
            clearBaseLayer();
            _placeView = null;
            return true;
        }
        return false;
    }

    public function setRoomBounds (r :Rectangle) :void
    {
        _roomBounds = r;
        layoutPlaceView();
    }

    /**
     * @return true if there are glyphs under the specified point.  If the glyph extends
     * InteractiveObject and the glyph sprite has mouseEnabled == false, it is not checked.
     */
    public function overlaysMousePoint (stageX :Number, stageY :Number) :Boolean
    {
        var stagePoint :Point = new Point(stageX, stageY);
        for (var ii :int = 0; ii < numChildren; ii ++) {
            var child :DisplayObject = unwrap(getChildAt(ii));
            if (child == _placeView) {
                continue;
            }
            // note that we want hitTestPoint() to be able to modify the value of the
            // child's mouseEnabled property, so do not reorder the following statements
            // in a fit of over-optimization
            if (!child.hitTestPoint(stageX, stageY, true)) {
                continue;
            }
            if (!(child is InteractiveObject) || (child as InteractiveObject).mouseEnabled) {
                return true;
            }
        }
        return false;
    }

    /**
     * This must be called on when our size is changed to allow us update our PlaceView mask and
     * resize the PlaceView itself.
     */
    override public function setActualSize (width :Number, height :Number) :void
    {
        super.setActualSize(width, height);

        // any PlaceLayer layers get informed of the size change
        for (var ii :int = 0; ii < numChildren; ii ++) {
            var child :DisplayObject = unwrap(getChildAt(ii));
            if (child == _placeView) {
                continue; // we'll handle this later
            } else if (child is PlaceLayer) {
                PlaceLayer(child).setPlaceSize(width, height);
            }
        }

        layoutPlaceView();
    }

    protected function layoutPlaceView () :void
    {
        var w :Number = this.width;
        var h :Number = this.height;
        if (_roomBounds != null) {
            if (CLEAN_BOUNDS) {
                var p :Point = DisplayUtil.fitRectInRect(_roomBounds, new Rectangle(0, 0, w, h));
                _base.x = Math.max(0, p.x);
                _base.y = Math.max(0, p.y);
                // TODO: enforce a min size?
                w = Math.min(_roomBounds.width, w);
                h = Math.min(_roomBounds.height, h);

            } else {
                _base.x = _roomBounds.x;
                _base.y = _roomBounds.y;
                w = _roomBounds.width;
                h = _roomBounds.height;
            }

        } else if (_placeView is RoomObjectView) {
            var metrics :RoomMetrics = RoomObjectView(_placeView).layout.metrics;
            if (metrics.sceneWidth >= w) {
                _base.x = 0;
            } else {
                _base.x = (w - metrics.sceneWidth) / 2;
                w = metrics.sceneWidth;
            }
            if (metrics.sceneHeight >= h) {
                _base.y = 0;
            } else {
                _base.y = (h - metrics.sceneHeight) / 2;
                h = metrics.sceneHeight;
            }

        } else {
            _base.x = 0;
            _base.y = 0;
        }

        // now inform the place view of its new size
        if (_placeView is UIComponent) {
            UIComponent(_placeView).setActualSize(w, h);
        } else if (_placeView is PlaceLayer) {
            PlaceLayer(_placeView).setPlaceSize(w, h);
        } else if (_placeView != null) {
            Log.getLog(this).warning("PlaceView is not a PlaceLayer or an UIComponent.");
        }

        _mask.graphics.clear();
        _mask.graphics.beginFill(0xFFFFFF);
        //_mask.graphics.drawRect(_base.x, _base.y, w, h);
        _mask.graphics.drawRect(0, 0, this.width, this.height);
        _mask.graphics.endFill();

        // TODO: bubble chat can currently overflow a restricted placeview size.
        // Fixing it was turning rabbit-holey, so I'm punting.
    }

    /** The mask configured on the PlaceView so that it doesn't overlap our other components. */
    protected var _mask :Shape;

    /** The current place view. */
    protected var _placeView :PlaceView;

    protected var _roomBounds :Rectangle;

    protected static const CLEAN_BOUNDS :Boolean = true;
}
}
