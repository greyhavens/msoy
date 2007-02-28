package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Shape;

import flash.events.Event;

import flash.geom.Rectangle;

import mx.core.Application;
import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.VBox;

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
    public function TopPanel (ctx :WorldContext)
    {
        _ctx = ctx;
        percentWidth = 100;
        percentHeight = 100;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        _placeBox = new Canvas();
        _placeBox.autoLayout = false;
        _placeBox.includeInLayout = false;
        addChild(_placeBox);

        // set up a mask on the placebox
        _placeMask = new Shape();
        _placeBox.mask = _placeMask;
        _placeBox.rawChildren.addChild(_placeMask);

        // set up the control bar
        _controlBar = new ControlBar(ctx, this);
        _controlBar.includeInLayout = false;
        _controlBar.setStyle("bottom", 0);
        _controlBar.setStyle("left", 0);
        _controlBar.setStyle("right", 0);
        addChild(_controlBar);
        
        // clear out the application and install ourselves as the only child
        var app :Application = Application(Application.application);
        app.removeAllChildren();
        app.addChild(this);
        layoutPanels();

        app.stage.addEventListener(Event.RESIZE, stageResized);
    }

    protected function stageResized (event :Event) :void
    {
        layoutPanels();
    }

    /**
     * Get the flex container that is holding the PlaceView. This is useful
     * if you want to overlay things over the placeview or register to
     * receive flex-specific events.
     */
    public function getPlaceContainer () :Container
    {
        return _placeBox;
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
            _placeBox.addChildAt(disp, 0);
        } else {
            _placeBox.rawChildren.addChildAt(disp, 0);
        }

        updatePlaceViewSize();
    }

    /**
     * Clear the specified place view, or null to clear any.
     */
    public function clearPlaceView (view :PlaceView) :void
    {
        if ((_placeView != null) && (view == null || view == _placeView)) {
            var disp :DisplayObject = DisplayObject(_placeView);
            if (disp is UIComponent) {
                _placeBox.removeChild(disp);
            } else {
                _placeBox.rawChildren.removeChild(disp);
            }
            _placeView = null;
        }
    }

    public function setSidePanel (side :UIComponent) :void
    {
        clearSidePanel(null);
        _sidePanel = side;
        _sidePanel.includeInLayout = false;

        addChild(_sidePanel); // add to end
        layoutPanels();
    }

    /**
     * Clear the specified side panel, or null to clear any.
     */
    public function clearSidePanel (side :UIComponent) :void
    {
        if ((_sidePanel != null) && (side == null || side == _sidePanel)) {
            removeChild(_sidePanel);
            _sidePanel = null;
            layoutPanels();
        }
    }

    public function setBottomPanel (bottom :UIComponent) :void
    {
        clearBottomPanel(null);
        _bottomPanel = bottom;
        _bottomPanel.includeInLayout = false;

        addChild(_bottomPanel); // add to end
        layoutPanels();
    }
    
    public function clearBottomPanel (bottom :UIComponent) :void
    {
        if ((_bottomPanel != null) && (bottom == null || bottom == _bottomPanel)) {
            removeChild(_bottomPanel);
            _bottomPanel = null;
            layoutPanels();
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

    protected function layoutPanels () :void
    {
        var sidePanelWidth :int = getSidePanelWidth(),
            bottomPanelHeight :int = getBottomPanelHeight();
            
        _placeBox.setStyle("top", 0);
        _placeBox.setStyle("bottom", bottomPanelHeight + _controlBar.height);
        _placeBox.setStyle("left", sidePanelWidth);
        _placeBox.setStyle("right", 0);

        if (_sidePanel != null) {
            _sidePanel.setStyle("top", 0);
            _sidePanel.setStyle("bottom", bottomPanelHeight + _controlBar.height);
            _sidePanel.setStyle("left", 0);
            _sidePanel.width = SIDE_PANEL_WIDTH;
        }
        
        if (_bottomPanel != null) {    
            _bottomPanel.setStyle("bottom", _controlBar.height);
            _bottomPanel.setStyle("left", 0);
            _bottomPanel.width = _controlBar.width;
            _bottomPanel.height = BOTTOM_PANEL_HEIGHT;
        }
            
        updatePlaceViewSize();
    }

    protected function updatePlaceViewSize () :void
    {
        var w :int = stage.stageWidth - getSidePanelWidth();
        var h :int = stage.stageHeight - _controlBar.height
            - getBottomPanelHeight();

        _placeMask.graphics.clear();
        _placeMask.graphics.beginFill(0xFFFFFF);
        _placeMask.graphics.drawRect(0, 0, w, h);
        _placeMask.graphics.endFill();

        if (_placeView != null) {
            if (_placeView is UIComponent) {
                UIComponent(_placeView).setActualSize(w, h);

            } else if (_placeView is MsoyPlaceView) {
                MsoyPlaceView(_placeView).setPlaceSize(w, h);

            } else {
                Log.getLog(this).warning(
                    "PlaceView is not a MsoyPlaceView or an UIComponent.");
            }
        }
    }

    protected function getSidePanelWidth () :int
    {
        return (_sidePanel == null ? 0 : SIDE_PANEL_WIDTH);
    }
    
    protected function getBottomPanelHeight () :int
    {
        return (_bottomPanel == null ? 0 : BOTTOM_PANEL_HEIGHT);
    }
        
    /** The giver of life. */
    protected var _ctx :WorldContext;

    /** The current place view. */
    protected var _placeView :PlaceView;

    /** The box that will hold the placeview. */
    protected var _placeBox :Canvas;

    /** The mask configured on the placeview so that it doesn't overlap
     * our other components. */
    protected var _placeMask :Shape;

    /** The current side panel component. */
    protected var _sidePanel :UIComponent;

    /** The current bottom panel component. */
    protected var _bottomPanel :UIComponent;

    /** Control bar at the bottom of the window. */
    protected var _controlBar :ControlBar;
    
    /** The list of our friends. */
    protected var _friendsList :FriendsList;
    
    /** A label indicating the build stamp for the client. */
    protected var _buildStamp :Label;

    protected static const SIDE_PANEL_WIDTH :int = 350;
    
    protected static const BOTTOM_PANEL_HEIGHT :int = 50;
}
}
