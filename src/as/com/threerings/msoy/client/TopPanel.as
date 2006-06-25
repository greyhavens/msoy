package com.threerings.msoy.client {

import mx.core.Application;
import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;

import mx.events.ResizeEvent;

import com.threerings.crowd.client.PlaceView;

public class TopPanel extends Canvas
{
    public var controlBar :ControlBar;

    public function TopPanel (ctx :MsoyContext, app :Application)
    {
        includeInLayout = false;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        controlBar = new ControlBar(ctx);
        addChild(controlBar);

        // clear out the application and install ourselves as the only child
        app.removeAllChildren();
        app.addChild(this);

        // listen for resizes
        app.addEventListener(ResizeEvent.RESIZE, didResize);
        configureSize(app);
    }

    public function setPlaceView (view :PlaceView) :void
    {
        clearPlaceView(null);
        _placeView = view;
        addChild(view as UIComponent);

        configureSize(parent as Container);
    }

    public function clearPlaceView (view :PlaceView) :void
    {
        if ((_placeView != null) && (view == null || view == _placeView)) {
            removeChild(_placeView as UIComponent);
            _placeView = null;
        }
    }

    protected function configureSize (container :Container) :void
    {
        // set our size to the same as the container
        width = container.width;
        height = container.height;

        // position the control bar
        var placeHeight :Number = height - controlBar.height;
        controlBar.move(0, placeHeight);
        controlBar.setActualSize(width, controlBar.height);

        // possibly position and size the place view
        if (_placeView != null) {
            var place :UIComponent = (_placeView as UIComponent);
            //place.setActualSize(width, placeHeight);

            // TODO: this is a hack
            (place as Object).setViewSize(width, placeHeight);
            place.move(0, 0);
        }
    }

    protected function didResize (event :ResizeEvent) :void
    {
        var container :Container = (event.currentTarget as Container);
        configureSize(container);
    }

    /** The current place view. */
    protected var _placeView :PlaceView;
}
}
