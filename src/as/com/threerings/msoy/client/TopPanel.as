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
    /** The control bar. */
    public var controlBar :ControlBar;

    /**
     * Construct the top panel.
     */
    public function TopPanel (ctx :MsoyContext, app :Application)
    {
        _ctx = ctx;
        percentWidth = 100;
        percentHeight = 100;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        if (DeploymentConfig.devClient) {
            // set up the build stamp label
            _buildStamp = new Label();
            _buildStamp.mouseEnabled = false;
            _buildStamp.mouseChildren = false;
            _buildStamp.text = "Build: " + DeploymentConfig.buildTime;
            _buildStamp.setStyle("color", "#FF6633");
            _buildStamp.setStyle("fontSize", 12);
            _buildStamp.setStyle("fontWeight", "bold");
            addChild(_buildStamp);
        }

        // set up the control bar
        controlBar = new ControlBar(ctx);
        controlBar.setStyle("bottom", 0);
        controlBar.setStyle("left", 0);
        addChild(controlBar);

        // clear out the application and install ourselves as the only child
        app.removeAllChildren();
        app.addChild(this);
    }

    public function setPlaceView (view :PlaceView) :void
    {
        clearPlaceView(null);
        _placeView = view;

        var comp :UIComponent = (view as UIComponent);
        comp.setStyle("top", 0);
        comp.setStyle("left", 0);
        comp.setStyle("right", 0);
        comp.setStyle("bottom", ControlBar.HEIGHT);
        addChildAt(comp, 0);
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
