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
        _masked = this;
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

        // TODO: why is this type-check here? surely when the place view changes it needs to be
        // laid out regardless of type
        if (_placeView is MsoyPlaceView) {
            layoutPlaceView();
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
     * Informs the place box of whether the client has been minimized (to make room for GWT). Note
     * that this assumes setActualSize will be called right afterwards, so does not do the updates
     * therein.
     * TODO: it would be nicer if LayeredContainer had an abstract update method required to be
     * called after all setters rather than one setter that does an update
     */
    public function setMinimized (minimized :Boolean) :void
    {
        _minimized = minimized;
    }

    /**
     * This must be called on when our size is changed to allow us update our PlaceView mask and
     * resize the PlaceView itself.
     */
    override public function setActualSize (width :Number, height :Number) :void
    {
        super.setActualSize(width, height);

        if (!(_placeView is RoomObjectView)) {
            _mask.graphics.clear();
            _mask.graphics.beginFill(0xFFFFFF);
            _mask.graphics.drawRect(0, 0, this.width, this.height);
            _mask.graphics.endFill();
        }

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
        _base.x = 0;
        _base.y = 0;
        if (_roomBounds != null) {
            // TODO: this is weird, why are we setting x & y of _base, but w & h of _placeView?
            // TODO: forensics on this feature, I'm not sure if anyone is using it
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

        }

        // now inform the place view of its new size
        if (_placeView is UIComponent) {
            UIComponent(_placeView).setActualSize(w, h);
            maskBox();
        } else if (_placeView is RoomObjectView) {

            // center the room view, adding margins if minimized
            var view :RoomObjectView = RoomObjectView(_placeView);
            const margin :Number = 20;
            view.setPlaceSize(w - margin * 2, h - margin * 2);
            var metrics :RoomMetrics = view.layout.metrics;
            var sceneHeight :Number = metrics.sceneHeight * view.scaleY;
            var sceneWidth :Number = metrics.sceneWidth * view.scaleX;
            view.y = margin + (h - sceneHeight) / 2;
            view.x = margin + Math.max((w - sceneWidth) / 2, 0);

            // mask it so that avatars and items don't bleed out or bounds
            _mask.graphics.clear();
            _mask.graphics.beginFill(0xFFFFFF);
            _mask.graphics.drawRect(view.x, view.y, sceneWidth, sceneHeight);
            _mask.graphics.endFill();

            maskBase();

        } else if (_placeView is PlaceLayer) {
            PlaceLayer(_placeView).setPlaceSize(w, h);
            maskBox();
        } else if (_placeView != null) {
            Log.getLog(this).warning("PlaceView is not a PlaceLayer or an UIComponent.");
        }

        // TODO: bubble chat can currently overflow a restricted placeview size.
        // Fixing it was turning rabbit-holey, so I'm punting.
    }

    protected function setMasked (disp :DisplayObject) :void
    {
        if (_masked == disp) {
            return;
        }
        _masked.mask = null;
        _masked = disp;
        _masked.mask = _mask;
    }

    protected function maskBox () :void
    {
        setMasked(this);
    }

    protected function maskBase () :void
    {
        // TODO: could we just make place view directly?
        // we expect the base to be a flex wrapper around the place view, fail if not
        if (_base == _placeView) {
            throw new Error("Masking base with unexpected place view type");
        }

        setMasked(_base);
    }

    /** The mask configured on the box or view so that it doesn't overlap outside components. */
    protected var _mask :Shape = new Shape();

    /** The object currently being masked, either this or _placeView. */
    protected var _masked :DisplayObject;

    /** The current place view. */
    protected var _placeView :PlaceView;

    protected var _roomBounds :Rectangle;

    protected var _minimized :Boolean;

    protected static const CLEAN_BOUNDS :Boolean = true;
}
}
