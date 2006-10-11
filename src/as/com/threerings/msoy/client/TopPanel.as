package com.threerings.msoy.client {

import mx.core.Application;
import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;
import mx.containers.HBox;

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

        // set up the control bar
        controlBar = new ControlBar(ctx);
        controlBar.includeInLayout = false;
        controlBar.setStyle("bottom", 0);
        controlBar.setStyle("left", 0);
        addChild(controlBar);

        if (DeploymentConfig.devClient) {
            // set up the build stamp label
            _buildStamp = new Label();
            _buildStamp.includeInLayout = false;
            _buildStamp.mouseEnabled = false;
            _buildStamp.mouseChildren = false;
            _buildStamp.text = "Build: " + DeploymentConfig.buildTime;
            _buildStamp.setStyle("color", "#F7069A");
            _buildStamp.setStyle("fontSize", 12);
            _buildStamp.setStyle("fontWeight", "bold");
            _buildStamp.setStyle("bottom", 0);
            addChild(_buildStamp);
        }

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
        comp.setStyle("right", _sideAttachment);
        comp.setStyle("bottom", ControlBar.HEIGHT);
        addChildAt(comp, 0);
    }

    /**
     * Clear the specified place view, or null to clear any.
     */
    public function clearPlaceView (view :PlaceView) :void
    {
        if ((_placeView != null) && (view == null || view == _placeView)) {
            removeChild(_placeView as UIComponent);
            _placeView = null;
        }
    }

    public function setSidePanel (side :UIComponent) :void
    {
        clearSidePanel(null);
        _sidePanel = side;
        _sidePanel.includeInLayout = false;

        setSideAttachment(_sidePanel.width);
        _sidePanel.addEventListener(ResizeEvent.RESIZE, sideResized);

        _sidePanel.setStyle("top", 0);
        _sidePanel.setStyle("bottom", ControlBar.HEIGHT);
        _sidePanel.setStyle("right", 0);

        addChild(_sidePanel); // add to end
    }

    /**
     * Clear the specified side panel, or null to clear any.
     */
    public function clearSidePanel (side :UIComponent) :void
    {
        if ((_sidePanel != null) && (side == null || side == _sidePanel)) {
            removeChild(_sidePanel);
            _sidePanel.removeEventListener(ResizeEvent.RESIZE, sideResized);
            _sidePanel = null;
            setSideAttachment(0);
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

    protected function sideResized (event :ResizeEvent) :void
    {
        setSideAttachment(_sidePanel.width);
    }

    protected function setSideAttachment (rightSpace :int) :void
    {
        _sideAttachment = rightSpace;
        if (_placeView != null) {
            UIComponent(_placeView).setStyle("right", _sideAttachment);
        }
    }

    /** The giver of life. */
    protected var _ctx :MsoyContext;

//    /** Where we hold the world and other bits. */
//    protected var _worldContainer :HBox;

    /** The current place view. */
    protected var _placeView :PlaceView;

    /** The current side panel component. */
    protected var _sidePanel :UIComponent;

    protected var _sideAttachment :int = 0;

    /** The list of our friends. */
    protected var _friendsList :FriendsList;
    
    /** A label indicating the build stamp for the client. */
    protected var _buildStamp :Label;
}
}
