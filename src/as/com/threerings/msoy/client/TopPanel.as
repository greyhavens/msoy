package com.threerings.msoy.client {

import mx.core.Application;
import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;

import mx.controls.Label;

import mx.events.ResizeEvent;

import com.threerings.util.ArrayUtil;
import com.threerings.util.Name;

import com.threerings.crowd.client.PlaceView;

public class TopPanel extends Canvas
{
    public var controlBar :ControlBar;

    public function TopPanel (ctx :MsoyContext, app :Application)
    {
        _ctx = ctx;
        includeInLayout = false;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        // set up the build stamp label
        _buildStamp = new Label();
        _buildStamp.mouseEnabled = false;
        _buildStamp.mouseChildren = false;
        _buildStamp.text = "Build: " + DeploymentConfig.buildTime;
        _buildStamp.setStyle("color", "#FF6633");
        addChild(_buildStamp);

        // set up the control bar
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

        addChildAt(view as UIComponent, 0);

        configureSize(parent as Container);
    }

    public function clearPlaceView (view :PlaceView) :void
    {
        if ((_placeView != null) && (view == null || view == _placeView)) {
            removeChild(_placeView as UIComponent);
            _placeView = null;
        }
    }

    public function showFriends (show :Boolean) :void
    {
        if (show) {
            // lazy-init the friendslist
            if (_friendsList == null) {
                _friendsList = new FriendsList(_ctx);
            }
            // put the pals list atop everything else
            addChild(_friendsList);

        } else {
            if (_friendsList != null) {
                removeChild(_friendsList);
            }
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
            try {
                (place as Object).setViewSize(width, placeHeight);
            } catch (err :ReferenceError) {
                Log.getLog(this).warning("placeView does not have a " +
                    "setViewSize method. We need to make this standard anyway.");
            }
            place.move(0, 0);
        }
    }

    protected function didResize (event :ResizeEvent) :void
    {
        var container :Container = (event.currentTarget as Container);
        configureSize(container);
    }

    /** The giver of life. */
    protected var _ctx :MsoyContext;

    /** The current place view. */
    protected var _placeView :PlaceView;

    /** The list of our friends. */
    protected var _friendsList :FriendsList;

    /** A label indicating the build stamp for the client. */
    protected var _buildStamp :Label;
}
}
