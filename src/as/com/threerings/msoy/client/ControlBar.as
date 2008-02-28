//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.controls.Spacer;
import mx.core.UIComponent;

import mx.events.FlexEvent;

import com.threerings.flex.ChatControl;
import com.threerings.flex.CommandButton;
import com.threerings.util.ValueEvent;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.ui.SkinnableImage;

[Style(name="backgroundSkin", type="Class", inherit="no")]

/**
 * Dispatched when the client is unminimized, and this component's display list has been validated.
 *
 * @eventType com.threerings.msoy.client.ControlBar.DISPLAY_LIST_VALID
 */
[Event(name="displayListValid", type="com.threerings.util.ValueEvent")]

/**
 * The control bar: the main menu and global UI element across all scenes.
 */
public class ControlBar extends HBox
{
    /**
     * An event dispatched when the client is unminimized, and this component's
     * display list has been validated.
     *
     * @eventType displayListValid
     */
    public static const DISPLAY_LIST_VALID :String = "displayListValid";

    /** The height of the control bar. This is fixed. */
    public static const HEIGHT :int = 24;

    /** Different groups of UI elements. */
    public static const UI_ALL :String = "All UI Elements"; // created automatically
    public static const UI_STD :String = "Standard Member UI";
    public static const UI_MINI :String = "Member UI Mini";
    public static const UI_EDIT :String = "Member UI Edit";
    public static const UI_GUEST :String = "Guest UI";
    public static const UI_SIDEBAR :String = "Member UI Sidebar";

    public static const ALL_UI_GROUPS :Array = [
        UI_ALL, UI_STD, UI_SIDEBAR, UI_MINI, UI_EDIT, UI_GUEST ];

    public function init (ctx :MsoyContext, top :TopPanel) :void
    {
        _ctx = ctx;
        styleName = "controlBar";

        var cls :Class = getStyle("backgroundSkin");
        setStyle("backgroundImage", cls);

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        height = HEIGHT;
        percentWidth = 100;

        var fn :Function = function (event :ClientEvent) :void {
            checkControls();
            updateUI();
        };
        _ctx.getClient().addClientObserver(new ClientAdapter(fn, fn, null, null, null, null, fn));

        createControls();
        checkControls();
        updateUI();
    }

    /**
     * Enables or disables our chat input.
     */
    public function setChatEnabled (enabled :Boolean) :void
    {
        if (_chatControl != null) {
            _chatControl.setEnabled(enabled);
        }
    }

    /**
     * Redirects our chat input to the specified chat director.
     */
    public function setChatDirector (chatDtr :ChatDirector) :void
    {
        if (_chatControl != null) {
            _chatControl.setChatDirector(chatDtr);
        }
    }

    /**
     * Changes the visibility and parameters of the navigation widgets.
     *
     * @param visible controls visibility of both the name and the back button
     * @param name specifies the location name to be displayed on the control bar
     * @param backEnabled specifies whether the back button should be enabled
     */
    public function updateNavigationWidgets (
        visible :Boolean, name :String, backEnabled :Boolean) :void
    {
        // don't do navigation here for now...
//         _loc.includeInLayout = _bookend.includeInLayout =
//             _loc.visible = _bookend.visible = visible;
        if (_backBtn != null) {
            _backBtn.enabled = backEnabled;
        }
//         const maxLen :int = 25;
//         if (name != null) {
//             _loc.text = name.length < maxLen ? name : (name.substr(0, maxLen) + "...");
//         } else {
//             _loc.text = "";
//         }
    }

    /**
     * Add a custom component to the control bar.
     * Note that there is no remove: just do component.parent.removeChild(component);
     */
    public function addCustomComponent (comp :UIComponent) :void
    {
        if (_spacer.width != 0) {
            _spacer.addChild(comp);
        } else {
            addChild(comp);
        }
    }

    /**
     * Called when our minimization status has changed.
     */
    public function miniChanged () :void
    {
        _isMinimized = _ctx.getTopPanel().isMinimized();
        updateUI();
    }

    /**
     * Called to tell us when we're in sidebar mode.
     */
    public function inSidebar (sidebaring :Boolean) :void
    {
        _inSidebar = sidebaring;
        updateUI();
    }

    /**
     * Configures our spacer width.
     */
    public function setSpacerWidth (width :Number) :void
    {
        _spacer.width = width;
    }

    /**
     * Configures the chat input background color.
     */
    public function setChatColor (color :int) :void
    {
        _chatControl.setChatColor(color);
    }

    public function enableZoomControl (enabled :Boolean ) :void
    {
        _zoomBtn.enabled = enabled;
    }

    // from HBox
    override protected function updateDisplayList (w :Number, h :Number) :void
    {
        super.updateDisplayList(w, h);
        if (!_isMinimized && width != 0) {
            dispatchEvent(new ValueEvent(DISPLAY_LIST_VALID, true));
        }
    }

    /**
     * Creates the controls we'll be using.
     */
    protected function createControls () :void
    {
        _spacer = new HBox();
        _spacer.styleName = "controlBarSpacer";
        _spacer.height = this.height;
        _spacer.percentWidth = 100;

        _chatBtn = new CommandButton();
        _chatBtn.toolTip = Msgs.GENERAL.get("i.channel"); // i.chatPrefs
        _chatBtn.setCommand(MsoyController.POP_CHANNEL_MENU); // MsoyController.CHAT_PREFS
        _chatBtn.styleName = "controlBarButtonChat";

        _volBtn = new CommandButton();
        _volBtn.toolTip = Msgs.GENERAL.get("i.volume");
        _volBtn.setCommand(MsoyController.POP_VOLUME, _volBtn);
        _volBtn.styleName = "controlBarButtonVolume";

        _zoomBtn = new CommandButton();
        _zoomBtn.toolTip = Msgs.GENERAL.get("i.zoom");
        _zoomBtn.setCommand(MsoyController.POP_ZOOM, _zoomBtn);
        _zoomBtn.styleName = "controlBarButtonZoom";

        _backBtn = new CommandButton();
        _backBtn.toolTip = Msgs.GENERAL.get("i.goBack");
        _backBtn.setCommand(MsoyController.MOVE_BACK);
        _backBtn.styleName = "controlBarButtonBack";
        _backBtn.enabled = false;
    }

    /**
     * Checks to see which controls the client should see.
     */
    protected function checkControls () :void
    {
        var isMember :Boolean = (_ctx.getMyName() != null &&
                                 _ctx.getMyName().getMemberId() != MemberName.GUEST_ID);
        if (numChildren > 0 && (isMember == _isMember)) {
            return;
        }

        removeAllChildren();
        clearAllGroups();

        addGroupChild(_spacer, [ UI_SIDEBAR ]);

        // add our standard control bar features
        addGroupChild(_chatBtn, [ UI_STD, UI_MINI, UI_EDIT, UI_GUEST, UI_SIDEBAR ]);
        _chatControl = null;
        _chatControl = new ChatControl(
            _ctx, Msgs.CHAT.get("b.send"), this.height, this.height - 4);
        addGroupChild(_chatControl, [ UI_STD, UI_MINI, UI_EDIT, UI_GUEST, UI_SIDEBAR ]);
        addGroupChild(_volBtn, [ UI_STD, UI_MINI, UI_GUEST, UI_EDIT, UI_SIDEBAR ]);
        addGroupChild(_zoomBtn, [ UI_STD, UI_GUEST, UI_EDIT ]);

        // add our various control buttons
        addControlButtons();

        // some elements that are common to guest and logged in users
        var footerLeft :SkinnableImage = new SkinnableImage();
        footerLeft.styleName = "controlBarFooterLeft";
        addGroupChild(footerLeft, [ UI_STD, UI_MINI,  UI_GUEST ]);

        var blank :Canvas = new Canvas();
        blank.styleName = "controlBarSpacer";
        blank.height = this.height;
        blank.percentWidth = 100;
        addGroupChild(blank, [ UI_STD, UI_MINI, UI_GUEST, UI_SIDEBAR ]);

        var footerRight :SkinnableImage = new SkinnableImage();
        footerRight.styleName = "controlBarFooterRight";
        addGroupChild(footerRight, [ UI_STD, UI_GUEST ]);

        addGroupChild(_backBtn, [ UI_STD, UI_GUEST ]);

        // and remember how things are set for now
        _isMember = isMember;
        _isMinimized = false;
    }

    protected function addControlButtons () :void
    {
        // derived classes can add other buttons here
    }

    protected function clearAllGroups () :void
    {
        for each (var key :String in ALL_UI_GROUPS) {
            if (_groups[key] == null) {
                _groups[key] = new Array();
            } else {
                _groups[key].length = 0;
            }
        }
    }

    protected function addGroupChild (child :UIComponent, groupNames :Array) :void
    {
        addChild(child);
        for each (var groupName :String in groupNames) {
            if (groupName != UI_ALL) {
                _groups[groupName].push(child);
            }
            _groups[UI_ALL].push(child);
        }
    }

    protected function updateGroup (groupName :String, value :Boolean) :void
    {
        var elt :UIComponent = null;
        for each (elt in _groups[groupName]) {
            elt.visible = elt.includeInLayout = value;
        }
    }

    protected function updateUI () :void
    {
        updateGroup(UI_ALL, false);
        updateGroup(getMode(), true);
    }

    protected function getMode () :String
    {
        if (_isMinimized) {
            return UI_MINI;
        } else if (_inSidebar) {
            return UI_SIDEBAR;
        } else if (_isMember) {
            return UI_STD;
        } else {
            return UI_GUEST;
        }
    }

    /** Our clientside context. */
    protected var _ctx :MsoyContext;

    /** Are we currently configured to show the controls for a member? */
    protected var _isMember :Boolean;

    /** Are we in a minimized mode? */
    protected var _isMinimized :Boolean;

    /** Are we in a sidebar? */
    protected var _inSidebar :Boolean;

    /** Object that contains all the different groups of UI elements. */
    protected var _groups :Object = new Object();

    /** Our chat control. */
    protected var _chatControl :ChatControl;

    /** The chat preferences button. */
    protected var _chatBtn :CommandButton;

    /** Handles volume adjustment. */
    protected var _volBtn :CommandButton;

    /** Handles room zooming. */
    protected var _zoomBtn :CommandButton;

    /** The back-movement button. */
    protected var _backBtn :CommandButton;

    /** Current location label. */
    protected var _loc :CanvasWithText;

    /** Bookend image at the other end of name label. */
    protected var _bookend :SkinnableImage;

    /** A spacer to bump the UI bits over to the right if needed */
    protected var _spacer :HBox;
}
}

import flash.text.TextFieldAutoSize;

import mx.containers.Canvas;
import mx.core.ScrollPolicy;
import mx.core.UITextField;

/** Internal: helper class that extends mx.containers.Canvas with automatic background loading from
 * the style sheet (e.g. via an external style sheet file). */
internal class CanvasWithText extends Canvas
{
    public var textfield :UITextField;

    public function CanvasWithText (height :int)
    {
        this.height = height;
        horizontalScrollPolicy = verticalScrollPolicy = ScrollPolicy.OFF;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        textfield = new UITextField ();
        textfield.styleName = "controlBarText";
        textfield.x = 5;
        textfield.y = 0;
        textfield.height = height;
        textfield.width = width;
        textfield.autoSize = TextFieldAutoSize.LEFT;
        addChild(textfield);
    }

    public function set text (message :String) :void
    {
        textfield.text = message;
        textfield.y = (this.height - textfield.textHeight) / 2;
    }

    public function get text () :String
    {
        return textfield.text;
    }
}
