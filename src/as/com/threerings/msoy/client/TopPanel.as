//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Rectangle;
import flash.system.Capabilities;

import mx.core.Application;
import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;
import mx.containers.HBox;

import mx.controls.Label;
import mx.controls.scrollClasses.ScrollBar;

import com.threerings.util.ConfigValueSetEvent;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.MethodQueue;
import com.threerings.util.ValueEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.client.LocationObserver;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.data.GameObject;

import com.threerings.msoy.chat.client.ChatTabBar;
import com.threerings.msoy.chat.client.ChatOverlay;
import com.threerings.msoy.chat.client.MsoyChatDirector;

public class TopPanel extends Canvas
    implements LocationObserver
{
    public static const DECORATIVE_MARGIN_HEIGHT :int = 0;

    public static const RIGHT_SIDEBAR_WIDTH :int = 300;

    /**
     * Construct the top panel.
     */
    public function TopPanel (ctx :MsoyContext, controlBar :ControlBar)
    {
        _ctx = ctx;
        _ctx.getLocationDirector().addLocationObserver(this);
        percentWidth = 100;
        percentHeight = 100;
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        styleName = "topPanel";

        var chatTabs :ChatTabBar = new ChatTabBar(_ctx);
        _ctx.getMsoyChatDirector().setChatTabs(chatTabs);

        if (!_ctx.getMsoyClient().isFeaturedPlaceView()) {
            _headerBar = new HeaderBar(_ctx, chatTabs);
            _headerBar.includeInLayout = false;
            _headerBar.setStyle("top", 0);
            _headerBar.setStyle("left", 0);
            _headerBar.setStyle("right", 0);
            addChild(_headerBar);
        }

        _placeBox = new PlaceBox();
        _placeBox.autoLayout = false;
        _placeBox.includeInLayout = false;
        addChild(_placeBox);

        if (!_ctx.getMsoyClient().isFeaturedPlaceView()) {
            // set up the control bar
            _controlBar = controlBar;
            _controlBar.init(ctx, this);
            _controlBar.includeInLayout = false;
            _controlBar.setStyle("bottom", 0);
            _controlBar.setStyle("left", 0);
            _controlBar.setStyle("right", 0);
            addChild(_controlBar);
            _controlBar.setLocation(null, false, false);

            // show a subtle build-stamp
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

        // clear out the application and install ourselves as the only child
        var app :Application = Application(Application.application);
        app.removeAllChildren();
        app.addChild(this);

        app.stage.addEventListener(Event.RESIZE, stageResized);
        _ctx.getClient().addEventListener(MsoyClient.MINI_WILL_CHANGE, miniWillChange);

        // display something until someone comes along and sets a real view on us
        setPlaceView(new BlankPlaceView());
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
        return true;
    }

    // from LocationObserver
    public function locationDidChange (place :PlaceObject) :void
    {
        if (_controlBar != null) {
            _controlBar.locationDidChange(place);
        }
    }

    // from LocationObserver
    public function locationChangeFailed (placeId :int, reason :String) :void
    {
        // NOOP
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

    public function setActiveOverlay (overlay :ChatOverlay) :void
    {
        _activeOverlay = overlay;
    }

    /**
     * Returns the chat overlay that is in use, or null if there is none.
     */
    public function getChatOverlay () :ChatOverlay
    {
        if (_activeOverlay == null) {
            var view :MsoyPlaceView = getPlaceView() as MsoyPlaceView;
            if (view != null) {
                _activeOverlay = view.getChatOverlay();
            }
        }
        return _activeOverlay;
    }

    /**
     * Sets the specified view as the current place view.
     */
    public function setPlaceView (view :PlaceView) :void
    {
        _placeBox.setPlaceView(view);
        layoutPanels();
        // update the overlay later so that it gets the updated width and height from layoutPanels,
        // which doesn't set the width and height directly, but sets the top and bottom style
        // properties and lets it figure it out for itself.
        callLater(updatePlaceViewChatOverlay);
    }

    /**
     * Clear the specified place view, or null to clear any.
     */
    public function clearPlaceView (view :PlaceView) :void
    {
        if (_placeBox.clearPlaceView(view)) {
            setPlaceView(new BlankPlaceView());
        }
    }

    /**
     * Returns the location and dimensions of the place view in relation to the entire stage.
     */
    public function getPlaceViewBounds () :Rectangle
    {
        var bounds :Rectangle = new Rectangle();
        bounds.x = 0;
        bounds.y = HeaderBar.HEIGHT;
        bounds.width = stage.stageWidth - getRightPanelWidth();
        bounds.height = stage.stageHeight - getBottomPanelHeight() - HeaderBar.HEIGHT;
        if (padVertical(_placeBox.getPlaceView())) {
            bounds.y += DECORATIVE_MARGIN_HEIGHT;
            bounds.height -= 2*DECORATIVE_MARGIN_HEIGHT;
        }
        bounds.height -= ControlBar.HEIGHT;
        return bounds;
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
     * Configures our right side panel. Any previous right side panel will be cleared.
     */
    public function setRightPanel (side :UIComponent) :void
    {
        clearRightPanel(null);
        _rightPanel = side;
        _rightPanel.includeInLayout = false;
        _rightPanel.width = side.width;
        addChild(_rightPanel);
        layoutPanels();
    }

    /**
     * Clear the specified side panel, or null to clear any.
     */
    public function clearRightPanel (side :UIComponent = null) :void
    {
        if ((_rightPanel != null) && (side == null || side == _rightPanel)) {
            if (_rightPanel.parent == this) {
                removeChild(_rightPanel);
            }
            _rightPanel = null;
            layoutPanels();
        }
    }

    public function getRightPanel () :UIComponent
    {
        return _rightPanel;
    }

    /**
     * Set the panel that should be shown along the bottom. The panel should have an explicit
     * height.
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

    public function getRightPanelWidth () :int
    {
        return (_rightPanel == null ? 0 : _rightPanel.width);
    }

    public function getBottomPanelHeight () :int
    {
        return (_bottomPanel == null ? 0 : _bottomPanel.height);
    }

    public function isMinimized () :Boolean
    {
        return _minimized;
    }

    public function slideInChat (chat :Container, bounds :Rectangle) :void
    {
        _chatBounds = bounds;
        if (_chat != null) {
            // we already own the chat
            return;
        }

        chat.autoLayout = false;
        chat.includeInLayout = false;
        addChild(_chat = chat);
        layoutPanels();
    }

    public function slideOutChat () :void
    {
        if (_chat == null) {
            // we don't currently own the chat
            return;
        }

        removeChild(_chat);
        _chat = null;
        layoutPanels();
    }

    protected function stageResized (event :Event) :void
    {
        layoutPanels();
    }

    protected function miniWillChange (event :ValueEvent) :void
    {
        if (event.value as Boolean) {
            minimizePlaceView();
        } else {
            restorePlaceView();
        }
    }

    /**
     * Take care of any bits that need to be changed when the place view is getting mini'd.
     */
    protected function minimizePlaceView () :void
    {
        _minimized = true;

// TODO: we're disabling the "click anywhere to restore the client" because that results in unhappy
// accidents; let's see how we like having to click the close button for a while
//
//         _placeBox.mouseChildren = false;
//         _placeBox.buttonMode = true;
//         _placeBox.useHandCursor = true;
//
//         // add this on the next frame, so that we don't accidentally register ourselves
//         // to receive the very click that causes this minimize action...
//         MethodQueue.callLater(_placeBox.addEventListener, [ MouseEvent.CLICK, placeBoxClicked ]);

        _headerBar.miniChanged();
        _controlBar.miniChanged();
    }

    /**
     * Undo any bits that were changed in minimizePlaceView()
     */
    protected function restorePlaceView () :void
    {
        _minimized = false;

// TODO: see above
//
//         _placeBox.useHandCursor = false;
//         _placeBox.buttonMode = false;
//         _placeBox.mouseChildren = true;
//         _placeBox.removeEventListener(MouseEvent.CLICK, placeBoxClicked);

        _headerBar.miniChanged();
        _controlBar.miniChanged();
    }

    protected function placeBoxClicked (event :MouseEvent) :void
    {
        _ctx.getMsoyClient().restoreClient();
    }

    protected function layoutPanels () :void
    {
        if (_ctx.getMsoyClient().isFeaturedPlaceView()) {
            // in this case, we only have one panel...
            updatePlaceViewSize();
            return;
        }

        _controlBar.setStyle("left", 0);
        _headerBar.setStyle("left", 0);

        if (_rightPanel != null) {
            _rightPanel.setStyle("top", HeaderBar.HEIGHT);
            _rightPanel.setStyle("right", 0);
            _rightPanel.setStyle("bottom", getBottomPanelHeight() + ControlBar.HEIGHT);

            // if we have no place view currently, stretch it all the way to the left; otherwise
            // let it be as wide as it wants to be
            if (_placeBox.parent == this) {
                _rightPanel.clearStyle("left");
            } else {
                _rightPanel.setStyle("left", 0);
            }

            _controlBar.setSpacerWidth(stage.stageWidth - _rightPanel.width);
        } else {
            _controlBar.setSpacerWidth(0);
        }

        updatePlaceViewSize();
    }

    /**
     * Check to see if the placeview should be using its chat overlay.
     */
    protected function updatePlaceViewChatOverlay () :void
    {
        var pv :PlaceView = getPlaceView();
        if (pv is MsoyPlaceView) {
            (pv as MsoyPlaceView).setUseChatOverlay(!_ctx.getMsoyClient().isFeaturedPlaceView());
        }
    }

    protected function updatePlaceViewSize () :void
    {
        if (_placeBox.parent != this) {
            return; // nothing doing if we're not in control
        }

        if (_ctx.getMsoyClient().isFeaturedPlaceView()) {
            _placeBox.clearStyle("top");
            _placeBox.clearStyle("bottom");
            _placeBox.clearStyle("left");
            _placeBox.clearStyle("right");
            _placeBox.wasResized(stage.stageWidth, stage.stageHeight);
            return;
        }

        var w :int = stage.stageWidth - getRightPanelWidth();
        var h :int = stage.stageHeight - getBottomPanelHeight() - HeaderBar.HEIGHT;
        var top :int = HeaderBar.HEIGHT;
        if (padVertical(_placeBox.getPlaceView())) {
            top += DECORATIVE_MARGIN_HEIGHT;
            h -= DECORATIVE_MARGIN_HEIGHT;
        }

        var bottom :int = getBottomPanelHeight();
        // for place views, we want to insert decorative margins above and below the view
        if (padVertical(_placeBox.getPlaceView())) {
            bottom += DECORATIVE_MARGIN_HEIGHT;
            h -= DECORATIVE_MARGIN_HEIGHT;
        }
        bottom += ControlBar.HEIGHT;
        h -= ControlBar.HEIGHT;

        var left :int = 0;
        if (_chat != null) {
            left += _chatBounds.width + CHAT_PADDING;
            w -= _chatBounds.width + CHAT_PADDING;
            _chat.setStyle("top", top);
            _chat.setStyle("bottom", bottom);
            _chat.setStyle("left", 0);
            _chat.setStyle("right", getRightPanelWidth() + w + CHAT_PADDING);
        }
        _placeBox.setStyle("top", top);
        _placeBox.setStyle("bottom", bottom);
        _placeBox.setStyle("left", left);
        _placeBox.setStyle("right", getRightPanelWidth());
        _placeBox.wasResized(w, h);
    }

    protected function padVertical (view :PlaceView) :Boolean
    {
        return (view is MsoyPlaceView) && (view as MsoyPlaceView).padVertical();
    }

    private static const log :Log = Log.getLog(TopPanel);

    protected static const CHAT_PADDING :int = 15;

    /** The giver of life. */
    protected var _ctx :MsoyContext;

    /** The box that will hold the placeview. */
    protected var _placeBox :PlaceBox;

    /** The current right panel component. */
    protected var _rightPanel :UIComponent;

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

    /** A flag to indicate if we're working in mini-view or not. */
    protected var _minimized :Boolean = false;

    protected var _activeOverlay :ChatOverlay;

    /** When chat is operating in slide mode (non-overlay), we manage it here. */
    protected var _chat :Container;
    protected var _chatBounds :Rectangle;

    protected static const MIN_FLASH_VERSION :int = 9;
    protected static const MIN_FLASH_REVISION :int = 28;
}
}
