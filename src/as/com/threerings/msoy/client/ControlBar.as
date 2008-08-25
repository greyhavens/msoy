//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.MouseEvent;

import flash.display.StageDisplayState;

import flash.utils.Dictionary;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.Tile;

import mx.controls.Spacer;

import mx.events.FlexEvent;

import com.threerings.util.ArrayUtil;
import com.threerings.util.Integer;
import com.threerings.util.Log;
import com.threerings.util.ValueEvent;

import com.threerings.flash.DisplayUtil;

import com.threerings.flex.ChatControl;
import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.FlexUtil;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.SkinnableImage;

import com.threerings.msoy.world.client.WorldController;

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
    public static const HEIGHT :int = 28;

    /** Button priorities */
    public static const VOLUME_PRIORITY :int = 0;
    public static const GLOBAL_PRIORITY :int = 100;
    public static const DEFAULT_PRIORITY :int = 200;
    public static const PLACE_PRIORITY :int = 300;

    /** Different groups of UI elements. */
    public static const UI_ALL :String = "All UI Elements"; // created automatically
    public static const UI_BASE :String = "Base UI"; // when in neither a game nor a room
    public static const UI_ROOM :String = "Room UI";
    public static const UI_GAME :String = "Game UI";
    public static const UI_MINI :String = "Mini UI"; // when GWT is up (TODO: remove, auto-adjust)
    public static const UI_VIEWER :String = "Room Entity Viewer UI";

    public static const ALL_UI_GROUPS :Array = [
        UI_ALL, UI_BASE, UI_ROOM, UI_GAME, UI_MINI, UI_VIEWER ];

    public function ControlBar (ctx :MsoyContext)
    {
        _ctx = ctx;
        // the rest is in init()
    }

    public function init (top :TopPanel) :void
    {
        styleName = "controlBar";
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
        height = HEIGHT;
        percentWidth = 100;

        _ctx.getClient().addClientObserver(
            new ClientAdapter(checkControls, checkControls, null, null, null, null, checkControls));

        _buttons = new ButtonPalette(top);

        createControls();
        checkControls();
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
     * Gives the chat input focus.
     */
    public function giveChatFocus () :void
    {
        if (_chatControl != null) {
            _chatControl.setFocus();
        }
    }

    /**
     * Called by the TopPanel when we move to a new location. This should some day take over for
     * setLocation().
     */
    public function locationDidChange (place :PlaceObject) :void
    {
        // by default we do nothing, but our derived classes do things
    }

    /**
     * Add a custom component to the control bar.
     * Note that there is no remove: just do component.parent.removeChild(component);
     */
    public function addCustomComponent (comp :UIComponent) :void
    {
        // no priority set- becomes priority = 0.
        addChild(comp);
        sortControls();
    }

    public function addCustomButton (comp :UIComponent, priority :int = DEFAULT_PRIORITY) :void
    {
        _priorities[comp] = priority;
        _buttons.addButton(comp, priority);
        sortControls();
        _buttons.recheckButtons();
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
     * Called to tell us when we're in game mode.
     */
    public function setInGame (inGame :Boolean) :void
    {
        _inGame = inGame;
        updateUI();
    }

    /**
     * Called to tell us when we're in room mode.
     */
    public function setInRoom (inRoom :Boolean) :void
    {
        _inRoom = inRoom;
        updateUI();
    }

    /**
     * Configures the chat input background color.
     */
    public function setChatColor (color :int) :void
    {
        _chatControl.setChatColor(color);
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
        _chatOptsBtn = new CommandButton();
        _chatOptsBtn.toolTip = Msgs.GENERAL.get("i.channel");
        _chatOptsBtn.setCommand(WorldController.POP_CHANNEL_MENU, _chatOptsBtn);
        _chatOptsBtn.styleName = "controlBarButtonChat";

        _volBtn = new CommandButton();
        _volBtn.toolTip = Msgs.GENERAL.get("i.volume");
        _volBtn.setCommand(MsoyController.POP_VOLUME, _volBtn);
        _volBtn.styleName = "controlBarButtonVolume";

        //_partyBtn = new CommandCheckBox("Party", handleJoinLeaveParty);

        _fullBtn = new CommandButton();
        _fullBtn.styleName = "controlBarButtonFull";
        _fullBtn.toolTip = "FullScreen: dev only"; //Msgs.GENERAL.get("i.full");
        _fullBtn.setCallback(handleFullScreen);

        _commentBtn = new CommandButton(null, MsoyController.VIEW_COMMENT_PAGE);
        _commentBtn.styleName = "controlBarButtonComment";
        _commentBtn.toolTip = Msgs.GENERAL.get("i.comment");

        _shareBtn = new CommandButton(null, FloatingPanel.createPopper(function () :ShareDialog {
            return new ShareDialog(_ctx);
        }));
        _shareBtn.styleName = "controlBarButtonShare";
        _shareBtn.toolTip = Msgs.GENERAL.get("i.share");

        _instructBtn = new CommandButton(null, MsoyController.VIEW_GAME_INSTRUCTIONS);
        _instructBtn.styleName = "controlBarButtonInstructions";
        _instructBtn.toolTip = Msgs.GENERAL.get("i.instructions");
    }

    /**
     * Checks to see which controls the client should see.
     */
    protected function checkControls (... ignored) :void
    {
        const memName :MemberName = _ctx.getMyName();
        const isMember :Boolean = (memName != null) && !memName.isGuest();
        if (numChildren > 0 && (isMember == _isMember)) {
            return;
        }

        // remember how things are set for now
        _isMember = isMember;
        _isMinimized = false;

        // and add our various control buttons
        setupControls();
        updateUI();
    }

//    protected function handleJoinLeaveParty (state :Boolean) :void
//    {
//        if (state) {
//            _ctx.getPartyDirector().joinParty();
//        } else {
//            _ctx.getPartyDirector().leaveParty();
//        }
//    }

    protected function setupControls () :void
    {
        removeAllChildren();
        _buttons.clearButtons();
        clearAllGroups();
        addControls();
    }

    protected function sortControls () :void
    {
        DisplayUtil.sortDisplayChildren(this, comparePriority);
        DisplayUtil.sortDisplayChildren(_volBtn.parent, comparePriority);
    }

    protected function addControls () :void
    {
        // add our standard control bar features
        addControl(_chatOptsBtn, [ UI_BASE, UI_ROOM, UI_MINI, UI_GAME ], CHAT_SECTION);
        _chatControl = new ChatControl(_ctx, null);
        _chatControl.chatInput.height = HEIGHT - 8;
        _chatControl.sendButton.styleName = "controlBarButtonSend";
        addControl(_chatControl, [ UI_BASE, UI_ROOM, UI_MINI, UI_GAME ], CHAT_SECTION);
        addControl(_buttons, [ UI_BASE, UI_ROOM, UI_MINI, UI_GAME, UI_VIEWER ],
            BUTTON_SECTION);

        // add buttons
        addButton(_volBtn, [ UI_BASE, UI_ROOM, UI_MINI, UI_GAME, UI_VIEWER ], VOLUME_PRIORITY);
        if (DeploymentConfig.devDeployment) {
            addButton(_fullBtn, [ UI_BASE, UI_ROOM, UI_MINI, UI_GAME, UI_VIEWER ],
                GLOBAL_PRIORITY);
        }

        addButton(_instructBtn, [ UI_GAME ]);
        addButton(_shareBtn, [ UI_BASE, UI_ROOM, UI_GAME ]);
        addButton(_commentBtn, [ UI_ROOM, UI_GAME ]);

        //addButton(_partyBtn, [ UI_ROOM, UI_MINI, UI_GAME, UI_VIEWER ]);
    }

    /**
     * Used to sort the buttons.
     */
    protected function comparePriority (comp1 :Object, comp2 :Object) :int
    {
        const pri1 :int = int(_priorities[comp1]);
        const pri2 :int = int(_priorities[comp2]);
        return Integer.compare(pri1, pri2);
    }

    protected function clearAllGroups () :void
    {
        for each (var key :String in ALL_UI_GROUPS) {
            _groups[key] = new Array();
        }
    }

    protected function addControl (child :UIComponent, groupNames :Array, section :int) :void
    {
        addChild(child);
        addToGroups(child, groupNames, section);
    }

    protected function addButton (
        child :UIComponent, groupNames :Array, priority :int = DEFAULT_PRIORITY) :void
    {
        _buttons.addButton(child, priority);
        addToGroups(child, groupNames, priority);
    }

    protected function addToGroups (child :UIComponent, groupNames :Array, priority :int) :void
    {
        _priorities[child] = priority;
        if (!ArrayUtil.contains(groupNames, UI_ALL)) {
            groupNames.push(UI_ALL);
        }
        for each (var groupName :String in groupNames) {
            _groups[groupName].push(child);
        }
    }

    protected function updateGroup (groupName :String, value :Boolean) :void
    {
        for each (var elt :UIComponent in _groups[groupName]) {
            FlexUtil.setVisible(elt, value);
        }
    }

    protected function updateUI () :void
    {
        updateGroup(UI_ALL, false);
        updateGroup(getMode(), true);
        sortControls();
        _buttons.recheckButtons();
    }

    protected function getMode () :String
    {
        if (_isMinimized) {
            return UI_MINI;
        } else if (_inGame) {
            return UI_GAME;
        } else if (_inRoom) {
            return UI_ROOM;
        } else {
            return UI_BASE;
        }
    }

    protected function handleFullScreen () :void
    {
        try {
            stage.displayState = (stage.displayState != StageDisplayState.FULL_SCREEN)
                ? StageDisplayState.FULL_SCREEN
                : StageDisplayState.NORMAL;
        } catch (se :SecurityError) {
            _fullBtn.enabled = false;
        }
    }

    protected static const CHAT_SECTION :int = -2;
    protected static const BUTTON_SECTION :int = -1;
    // implicit: custom controls section = 0
    protected static const NOTIFICATION_SECTION :int = 1;

    /** Our clientside context. */
    protected var _ctx :MsoyContext;

    //protected var _partyBtn :CommandCheckBox;

    /** Are we currently configured to show the controls for a member? */
    protected var _isMember :Boolean;

    /** Are we in a minimized mode? */
    protected var _isMinimized :Boolean;

    /** Are we in a game? */
    protected var _inGame :Boolean;

    /** Are we in a room? */
    protected var _inRoom :Boolean;

    /** Object that contains all the different groups of UI elements. */
    protected var _groups :Object = new Object();

    /** Button priority levels. */
    protected var _priorities :Dictionary = new Dictionary(true);

    protected var _buttons :ButtonPalette;

    /** Our chat control. */
    protected var _chatControl :ChatControl;

    /** The chat preferences button. */
    protected var _chatOptsBtn :CommandButton;

    /** Handles volume adjustment. */
    protected var _volBtn :CommandButton;

    /** Handles full screening. */
    protected var _fullBtn :CommandButton;

    /** Handles viewing game instructions. */
    protected var _instructBtn :CommandButton;

    /** Handles commenting on the current scene or game. */
    protected var _commentBtn :CommandButton;

    /** Handles bringing up a share dialog. */
    protected var _shareBtn :CommandButton;
}
}
