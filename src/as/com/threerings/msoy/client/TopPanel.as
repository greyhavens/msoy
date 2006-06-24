package com.threerings.msoy.client {

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;

import com.threerings.crowd.client.PlaceView;

public class TopPanel extends Canvas
{
    public var controlBar :ControlBar;

    public function TopPanel (ctx :MsoyContext)
    {
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        controlBar = new ControlBar(ctx);
        addChild(controlBar);
    }

    public function setPlaceView (view :PlaceView) :void
    {
        clearPlaceView(null);
        _placeView = view;
        addChild(view as UIComponent);
        invalidateSize();
    }

    public function clearPlaceView (view :PlaceView) :void
    {
        if ((_placeView != null) && (view == null || view == _placeView)) {
            removeChild(_placeView as UIComponent);
            _placeView = null;
        }
    }

    override protected function measure () :void
    {
        // take up 100% of our parent
        width = parent.width;
        height = parent.height;

        if (_placeView != null) {
            var placeHeight :Number = height - controlBar.height;
            var place :UIComponent = (_placeView as UIComponent);

            var placeScale :Number = placeHeight / (place.height/place.scaleY);
            place.scaleX = placeScale;
            place.scaleY = placeScale;
        }

        super.measure();
    }

    override protected function updateDisplayList (
            unscaledWidth :Number, unscaledHeight :Number) :void
    {
        var placeHeight :Number = unscaledHeight - controlBar.height;

        trace("TopPanel: " + unscaledHeight + "/ " + placeHeight);

        controlBar.move(0, placeHeight);

        if (_placeView != null) {
            var place :UIComponent = (_placeView as UIComponent);
            place.move(0, 0);
        }

        super.updateDisplayList(unscaledWidth, unscaledHeight);
    }

    /** The current place view. */
    protected var _placeView :PlaceView;
}
}
