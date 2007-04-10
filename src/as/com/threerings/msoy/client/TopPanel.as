//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Shape;

import flash.events.Event;

import flash.geom.Rectangle;

import flash.system.Capabilities;

import mx.core.Application;
import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;
import mx.controls.scrollClasses.ScrollBar;

import mx.events.ResizeEvent;

import com.threerings.util.ArrayUtil;
import com.threerings.util.MessageBundle;
import com.threerings.util.Name;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.client.LocationObserver;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.data.GameObject;

import com.threerings.msoy.chat.client.ChatContainer;

import com.threerings.msoy.game.client.FloatingTableDisplay;

public class TopPanel extends Canvas 
    implements LocationObserver
{
    /**
     * Construct the top panel.
     */
    public function TopPanel (ctx :WorldContext)
    {
        _ctx = ctx;
        _ctx.getLocationDirector().addLocationObserver(this);
        percentWidth = 100;
        percentHeight = 100;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        _headerBar = new HeaderBar(ctx);
        _headerBar.includeInLayout = false;
        _headerBar.setStyle("top", 0);
        _headerBar.setStyle("left", 0);
        _headerBar.setStyle("right", 0);
        addChild(_headerBar);

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

        // show a subtle build-stamp
        var buildStamp :Label = new Label();
        buildStamp.includeInLayout = false;
        buildStamp.mouseEnabled = false;
        buildStamp.mouseChildren = false;
        buildStamp.text = "Build: " + DeploymentConfig.buildTime + "  " + Capabilities.version;
        buildStamp.setStyle("color", "#F7069A");
        buildStamp.setStyle("fontSize", 8);
        buildStamp.setStyle("bottom", ControlBar.HEIGHT);
        buildStamp.setStyle("right", ScrollBar.THICKNESS);
        addChild(buildStamp);

        // clear out the application and install ourselves as the only child
        var app :Application = Application(Application.application);
        app.removeAllChildren();
        app.addChild(this);
        layoutPanels();

        app.stage.addEventListener(Event.RESIZE, stageResized);
    }

    /**
     * Ensures that we are running a sufficiently new version of Flash, returning true if so. If
     * not, it displays a message to the user indicating that they need to upgrade their Flash
     * player and returns false.
     */
    public function verifyFlashVersion () :Boolean
    {
        // the version looks like "LNX 9,0,31,0"
        try {
            var bits :Array = Capabilities.version.split(" ");
            if (bits.length < 2) {
                throw new Error("Failed to split on space");
            }
            bits = (bits[1] as String).split(",");
            if (bits.length < 3) {
                throw new Error("Failed to split on comma");
            }

            // check the major and minor version numbers
            if (int(bits[0]) >= MIN_FLASH_VERSION && int(bits[2]) >= MIN_FLASH_REVISION) {
                return true;
            }

            // display an error and fail
            var panel :DisconnectedPanel = new DisconnectedPanel(_ctx);
            panel.setMessage(MessageBundle.tcompose(
                                 "m.min_flash_version", bits[0], bits[2],
                                 MIN_FLASH_VERSION, MIN_FLASH_REVISION), true);
            setPlaceView(panel);
            return false;

        } catch (error :Error) {
            trace("Choked checking version [version=" + Capabilities.version +
                  ", error=" + error + ".");
            // ah well, whatever, let 'em in and hope for the best
        }
        return true;
    }

    // from LocationObserver
    public function locationMayChange (placeId :int) :Boolean
    {
        // currently there are no side panel types that should survive a place change
        clearSidePanel(null);
        return true;
    }

    // from LocationObserver
    public function locationDidChange (place :PlaceObject) :void
    {
        // if we just moved to a game lobby make sure the current floating table display is cleared
        if (place is GameObject) {
            clearTableDisplay();
        }
    }

    // from LocationObserver
    public function locationChangeFailed (placeId :int, reason :String) :void
    {
        // NOOP
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

    /**
     * Returns a reference to our ControlBar component.
     */
    public function getControlBar () :ControlBar
    {
        return _controlBar;
    }

    /**
     * Returns a referenc eto our HeaderBar component
     */
    public function getHeaderBar () :HeaderBar
    {
        return _headerBar;
    }

    public function setSidePanel (side :UIComponent) :void
    {
        clearSidePanel(null);
        _sidePanel = side;
        _sidePanel.includeInLayout = false;
        _sidePanel.width = side.width;

        if (_tableDisp != null) {
            _tableDisp.x += _sidePanel.width;
        }

        addChild(_sidePanel); // add to end
        layoutPanels();
    }

    /**
     * Clear the specified side panel, or null to clear any.
     */
    public function clearSidePanel (side :UIComponent) :void
    {
        if ((_sidePanel != null) && (side == null || side == _sidePanel)) {
            if (_tableDisp != null) {
                _tableDisp.x -= _sidePanel.width;
                if (_tableDisp.x < 0) {
                    _tableDisp.x = 0;
                }
            }

            removeChild(_sidePanel);
            _sidePanel = null;

            layoutPanels();
        }
    }

    /**
     * Sets the current table display
     */
    public function setTableDisplay (tableDisp :FloatingTableDisplay) :void
    {
        if (tableDisp != _tableDisp) {
            clearTableDisplay();
            _tableDisp = tableDisp;
            _tableDisp.x = 0;
            _tableDisp.y = DECORATIVE_MARGIN_HEIGHT + HeaderBar.HEIGHT;
        }
    }

    /**
     * Gets the current table display
     */
    public function getTableDisplay () :FloatingTableDisplay
    {
        return _tableDisp;
    }

    /**
     * Clears the current table display - should only be used if this table display should be
     * destroyed (i.e. the game started, or the another table was joined)
     */
    public function clearTableDisplay () :void
    {
        if (_tableDisp != null) {
            _tableDisp.shutdown();
            _tableDisp = null;
        }
    }

    /**
     * Set the panel that should be shown along the bottom. The panel
     * should have an explicit height. If the height is 100 pixels
     * or larger, a chat box will be placed to the left of it and
     * removed from the room overlay.
     */
    public function setBottomPanel (bottom :UIComponent) :void
    {
        clearBottomPanel(null);

        _bottomPanel = new HBox();
        _bottomPanel.setStyle("horizontalGap", 0);
        _bottomPanel.setStyle("bottom", ControlBar.HEIGHT);
        _bottomPanel.setStyle("left", 0);
        _bottomPanel.setStyle("right", 0);

        _bottomComp = bottom;
        _bottomComp.percentWidth = 100;
        _bottomPanel.addChild(bottom);
        _bottomPanel.includeInLayout = false;
        _bottomPanel.height = bottom.height; // eek?

        addChild(_bottomPanel); // add to end
        layoutPanels();
    }

    public function clearBottomPanel (bottom :UIComponent) :void
    {
        if ((_bottomComp != null) && (bottom == null || bottom == _bottomComp)) {
            removeChild(_bottomPanel);
            _bottomPanel = null;
            _bottomComp = null;
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

    protected function stageResized (event :Event) :void
    {
        layoutPanels();
    }

    protected function layoutPanels () :void
    {
        if (_sidePanel != null) {
            _sidePanel.setStyle("top", 0);
            _sidePanel.setStyle("bottom", getBottomPanelHeight());
            _sidePanel.setStyle("left", 0);
            _controlBar.setStyle("left", _sidePanel.width);
            _headerBar.setStyle("left", _sidePanel.width);
        } else {
            _controlBar.setStyle("left", 0);
            _headerBar.setStyle("left", 0);
        }

        updatePlaceViewSize();
    }

    protected function updatePlaceViewSize () :void
    {
        var botHeight :int = getBottomPanelHeight();
        var w :int = stage.stageWidth - getSidePanelWidth();
        var h :int = stage.stageHeight - ControlBar.HEIGHT - botHeight - HeaderBar.HEIGHT;
        var top :int = HeaderBar.HEIGHT;
        var bottom :int = botHeight + ControlBar.HEIGHT;

        // actually, for place views, we want to insert decorative margins
        // above and below the view - so let's tweak the sizes
        if (_placeView is MsoyPlaceView) {
            top += DECORATIVE_MARGIN_HEIGHT;
            bottom += DECORATIVE_MARGIN_HEIGHT;
            h -= DECORATIVE_MARGIN_HEIGHT * 2;
        }

        _placeBox.setStyle("top", top);
        _placeBox.setStyle("bottom", bottom);
        _placeBox.setStyle("left", getSidePanelWidth());
        _placeBox.setStyle("right", 0);

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
        return (_sidePanel == null ? 0 : _sidePanel.width);
    }

    protected function getBottomPanelHeight () :int
    {
        return (_bottomPanel == null ? 0 : _bottomPanel.height);
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

    /** The thing what holds the bottom panel. */
    protected var _bottomPanel :HBox;

    /** The current bottom panel component. */
    protected var _bottomComp :UIComponent;

    /** Header bar at the top of the window. */
    protected var _headerBar :HeaderBar;

    /** Control bar at the bottom of the window. */
    protected var _controlBar :ControlBar;

    /** Storage for a GUI element corresponding to decorative lines. */
    protected var _decorativeBar :Canvas;

    /** The list of our friends. */
    protected var _friendsList :FriendsList;

    /** the currently active table display */
    protected var _tableDisp :FloatingTableDisplay;

    protected static const DECORATIVE_MARGIN_HEIGHT :int = 4;

    protected static const MIN_FLASH_VERSION :int = 9;
    protected static const MIN_FLASH_REVISION :int = 28;
}
}
