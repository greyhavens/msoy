//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.geom.Rectangle;

import mx.core.Application;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;

import mx.controls.Label;
import mx.controls.scrollClasses.ScrollBar;

import com.threerings.crowd.client.PlaceView;
import com.threerings.util.ValueEvent;

import com.threerings.msoy.chat.client.ChatTabBar;
import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.ComicOverlay;
import com.threerings.msoy.chat.client.GameChatContainer;

/**
 * Dispatched when the name of our current location changes. The value supplied will be a string
 * with the new location name.
 *
 * @eventType com.threerings.msoy.client.TopPanel.LOCATION_NAME_CHANGED
 */
[Event(name="locationNameChanged", type="com.threerings.util.ValueEvent")]

/**
 * Dispatched when the owner for our current location changes. The value supplied will either be a
 * MemberName or a GroupName, or null if we move to a location with no owner.
 *
 * @eventType com.threerings.msoy.client.TopPanel.LOCATION_OWNER_CHANGED
 */
[Event(name="locationOwnerChanged", type="com.threerings.util.ValueEvent")]

public class TopPanel extends Canvas
{
    public static const RIGHT_SIDEBAR_WIDTH :int = 300;

    /** An event dispatched when our location name changes. */
    public static const LOCATION_NAME_CHANGED :String = "locationNameChanged";

    /** An event dispatched when our location owner changes. */
    public static const LOCATION_OWNER_CHANGED :String = "locationOwnerChanged";

    /**
     * Construct the top panel.
     */
    public function TopPanel (ctx :MsoyContext, controlBar :ControlBar)
    {
        _ctx = ctx;
        _showChrome = !ctx.getMsoyClient().isChromeless();
        percentWidth = 100;
        percentHeight = 100;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        styleName = "topPanel";

        var chatTabs :ChatTabBar = new ChatTabBar(_ctx);
        _ctx.getMsoyChatDirector().setChatTabs(chatTabs);

        _headerBar = new HeaderBar(_ctx, this, chatTabs);
        _headerBar.includeInLayout = false;
        _headerBar.setStyle("left", 0);
        _headerBar.setStyle("right", 0);
        addChild(_headerBar);

        _placeBox = new PlaceBox();
        _placeBox.autoLayout = false;
        _placeBox.includeInLayout = false;
        addChild(_placeBox);

        // set up the control bar
        _controlBar = controlBar;
        _controlBar.includeInLayout = false;
        _controlBar.init(this);
        _controlBar.setStyle("left", 0);
        _controlBar.setStyle("right", 0);
        addChild(_controlBar);

        // show a subtle build-stamp on dev builds
        if (DeploymentConfig.devDeployment) {
            var buildStamp :Label = new Label();
            buildStamp.includeInLayout = false;
            buildStamp.mouseEnabled = false;
            buildStamp.mouseChildren = false;
            buildStamp.text = "Build: " + DeploymentConfig.buildTime;
            buildStamp.setStyle("color", "#F7069A");
            buildStamp.setStyle("fontSize", 8);
            buildStamp.setStyle("bottom", ControlBar.HEIGHT);
            // The scrollbar isn't really this thick, but it's pretty close.
            buildStamp.setStyle("right", ScrollBar.THICKNESS);
            addChild(buildStamp);
        }

        // only create and display an overlay for real clients
        if (UberClient.isRegularClient()) {
            _comicOverlay = new ComicOverlay(_ctx, _placeBox);
            _ctx.getMsoyChatDirector().addChatDisplay(_comicOverlay);
        }

        // clear out the application and install ourselves as the only child
        var app :Application = UberClient.getApplication();
        app.removeAllChildren();
        app.addChild(this);
        app.stage.addEventListener(Event.RESIZE, stageResized);

        // display something until someone comes along and sets a real view on us
        setPlaceView(new BlankPlaceView(_ctx));
    }

    /**
     * Get the flex container that is holding the PlaceView. This is useful if you want to overlay
     * things over the placeview or register to receive flex-specific events.
     */
    public function getPlaceContainer () :PlaceBox
    {
        return _placeBox;
    }

    /**
     * Returns the currently configured place view.
     */
    public function getPlaceView () :PlaceView
    {
        return _placeBox.getPlaceView();
    }

    /**
     * Returns the chat overlay that is in use, or null if there is none.
     */
    public function getChatOverlay () :ChatOverlay
    {
        if (_leftPanel is GameChatContainer) {
            return (_leftPanel as GameChatContainer).getChatOverlay();
        } else {
            return _comicOverlay;
        }
    }

    /**
     * Returns the comic overlay that is used for all place view chat
     */
    public function getPlaceChatOverlay () :ComicOverlay
    {
        return _comicOverlay;
    }

    /**
     * Change whether we're showing or hiding most of the UI.
     */
    public function setShowChrome (show :Boolean) :void
    {
        _showChrome = show;
        layoutPanels();
    }

    /**
     * Sets the specified view as the current place view.
     */
    public function setPlaceView (view :PlaceView) :void
    {
        _placeBox.setPlaceView(view);
        layoutPanels();

        const mView :MsoyPlaceView = view as MsoyPlaceView;
        if (_comicOverlay != null) {
            _comicOverlay.displayChat((mView != null) && mView.shouldUseChatOverlay());
        }
    }

    /**
     * Clear the specified place view, or null to clear any.
     */
    public function clearPlaceView (view :PlaceView) :void
    {
        if (_placeBox.clearPlaceView(view)) {
            setPlaceView(new BlankPlaceView(_ctx));
        }
    }

    /**
     * Returns the location and dimensions of the place view in relation to the entire stage.
     */
    public function getPlaceViewBounds () :Rectangle
    {
        var left :Number = _placeBox.getStyle("left");
        var top :Number = _placeBox.getStyle("top");
        var width :Number = _ctx.getWidth() - _placeBox.getStyle("right") - left;
        var height :Number = _ctx.getHeight() - _placeBox.getStyle("bottom") - top;
        return new Rectangle(left, top, width, height);
    }

    /**
     * Returns a rectangle in stage coordinates that specifies the main game area.  This is
     * basically just the bounds on the client, minus the any margins from control/header bars, etc.
     */
    public function getMainAreaBounds () :Rectangle
    {
        var height: Number = _ctx.getHeight() - _placeBox.getStyle("bottom");
        return new Rectangle(0, _placeBox.getStyle("top"), _ctx.getWidth(), height);
    }

    /**
     * Returns a reference to our ControlBar component.
     */
    public function getControlBar () :ControlBar
    {
        return _controlBar;
    }

    /**
     * Returns a reference to our HeaderBar component
     */
    public function getHeaderBar () :HeaderBar
    {
        return _headerBar;
    }

    /**
     * Configures our left side panel. Any previous right side panel will be cleared.
     */
    public function setLeftPanel (side :UIComponent) :void
    {
        clearLeftPanel(null);
        _leftPanel = side;
        _leftPanel.includeInLayout = false;
        _leftPanel.width = side.width;
        addChild(_leftPanel);
        layoutPanels();
    }

    /**
     * Clear the specified side panel, or null to clear any.
     */
    public function clearLeftPanel (side :UIComponent = null) :void
    {
        if ((_leftPanel != null) && (side == null || side == _leftPanel)) {
            if (_leftPanel.parent == this) {
                removeChild(_leftPanel);
            }
            _leftPanel = null;
            layoutPanels();
            // HACK ATTACK: jiggle the selected tab so that the room occupant list shows up when
            // we leave a game.
            // TODO: when we have time HAW HAW HAW HAW, clean up the chat overlay/sidebar code
            _headerBar.getChatTabs().selectedIndex = _headerBar.getChatTabs().selectedIndex;
        }
    }

    public function getLeftPanel () :UIComponent
    {
        return _leftPanel;
    }

    public function getLeftPanelWidth () :int
    {
        return (_leftPanel == null ? 0 : _leftPanel.width);
    }

    protected function stageResized (event :Event) :void
    {
        layoutPanels();
    }

    protected function getHeaderBarHeight () :int
    {
        return _showChrome ? HeaderBar.getHeight(_ctx.getMsoyClient()) : 0;
    }

    protected function layoutPanels () :void
    {
        // Pin the app to the stage.
        // This became necessary for "stubs" after we upgraded to flex 3.2.
        var app :Application = UberClient.getApplication();
        app.width = _ctx.getWidth();
        app.height = _ctx.getHeight();

        _controlBar.setStyle("bottom", _showChrome ? 0 : -ControlBar.HEIGHT);
        _headerBar.setStyle("top", _showChrome ? 0 : -HeaderBar.getHeight(_ctx.getMsoyClient()));

        if (_leftPanel != null) {
            _leftPanel.setStyle("top", getHeaderBarHeight());
            _leftPanel.setStyle("left", 0);
            _leftPanel.setStyle("bottom", ControlBar.HEIGHT);

            // if we have no place view currently, stretch it all the way to the left; otherwise
            // let it be as wide as it wants to be
            if (_placeBox.parent == this) {
                _leftPanel.clearStyle("right");
            } else {
                _leftPanel.setStyle("right", 0);
            }
        }

        updatePlaceViewSize();
    }

    protected function updatePlaceViewSize () :void
    {
        if (_placeBox.parent != this) {
            return; // nothing doing if we're not in control
        }

        var top :int = getHeaderBarHeight();
        var left :int = 0;
        var right :int = 0;
        var bottom :int = 0;
        var w :int = _ctx.getWidth() - getLeftPanelWidth();
        var h :int = _ctx.getHeight() - top;

        if (_showChrome) {
            bottom += ControlBar.HEIGHT;
            h -= ControlBar.HEIGHT;
        }

        if (_comicOverlay != null) {
            _comicOverlay.setTargetBounds(new Rectangle(0, 0, ChatOverlay.DEFAULT_WIDTH, h));
        }

        // w -= ScrollBar.THICKNESS;
        _placeBox.setMinimized(_ctx.getMsoyClient().isMinimized());
        _placeBox.setStyle("top", top);
        _placeBox.setStyle("bottom", bottom);
        _placeBox.setStyle("right", right);
        _placeBox.setStyle("left", left + getLeftPanelWidth()); // + ScrollBar.THICKNESS);
        _placeBox.setActualSize(w, h);
    }

    /** The giver of life. */
    protected var _ctx :MsoyContext;

    /** Are we showing the header and control bars? */
    protected var _showChrome :Boolean;

    /** The box that will hold the placeview. */
    protected var _placeBox :PlaceBox;

    /** The current right panel component. */
    protected var _leftPanel :UIComponent;

    /** Header bar at the top of the window. */
    protected var _headerBar :HeaderBar;

    /** Control bar at the bottom of the window. */
    protected var _controlBar :ControlBar;

    protected var _comicOverlay :ComicOverlay;
}
}
