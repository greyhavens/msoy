//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.ProgressEvent;

import flash.display.StageDisplayState;
import flash.display.LoaderInfo;

import flash.utils.Dictionary;
import flash.utils.setInterval;
import flash.utils.clearInterval;
import flash.utils.getTimer;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.HBox;

import com.threerings.util.ArrayUtil;
import com.threerings.util.Integer;

import com.threerings.flash.DisplayUtil;

import com.threerings.flex.ChatControl;
import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.presents.client.ClientAdapter;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.world.client.WorldController;

/**
 * The control bar: the main menu and global UI element across all scenes.
 */
public class ControlBar extends HBox
{
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
    public static const UI_AVRGAME :String = "AVR Game UI";
    public static const UI_VIEWER :String = "Room Entity Viewer UI";

    public static const ALL_UI_GROUPS :Array = [ 
        UI_ALL, UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME, UI_VIEWER ];

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
     * Called to tell us when we're in game mode.
     */
    public function setInGame (inGame :Boolean) :void
    {
        _inGame = inGame;
        updateUI();
    }

    /**
     * Called to tell us when we're in avr game mode.
     */
    public function setInAVRGame (inAVRGame :Boolean) :void
    {
        _inAVRGame = inAVRGame;
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

    /**
     * Sets up the load progress feedback for an AVRG.
     */
    public function setAVRGameLoaderInfo (info :LoaderInfo) :void
    {
        const PERIOD :Number = 1.5 * 1000;
        const DELAY :Number = 3.0 * 1000;

        var start :Number = getTimer() + DELAY;
        var intervalId :uint = 0;

        // animate the alpha based on time
        function updateAlpha () :void {
            var t :Number = getTimer() - start;
            if (t > 0) {
                var cos :Number = Math.cos(t * 2 * Math.PI / PERIOD);
                _avrgBtn.alpha = 0.7 + 0.3 * cos; // 0.4 .. 1.0
            }
        }

        function progress (evt :ProgressEvent) :void {
            // TODO: update text field in menu
        }

        function complete (evt :Event) :void {
            _avrgBtn.alpha = 1.0;
            clearInterval(intervalId);
            info.removeEventListener(ProgressEvent.PROGRESS, progress);
            info.removeEventListener(Event.COMPLETE, complete);
        }

        intervalId = setInterval(updateAlpha, 1);
        info.addEventListener(ProgressEvent.PROGRESS, progress);
        info.addEventListener(Event.COMPLETE, complete);
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

        _avrgBtn = new CommandButton();
        _avrgBtn.toolTip = Msgs.GENERAL.get("i.avrg");
        _avrgBtn.setCommand(WorldController.POP_AVRG_MENU, _avrgBtn);
        _avrgBtn.styleName = "controlBarAVRGButton";
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

        // and add our various control buttons
        setupControls();
        updateUI();
    }

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
        addControl(_chatOptsBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ], CHAT_SECTION);
        _chatControl = new ChatControl(_ctx, null);
        _chatControl.chatInput.height = HEIGHT - 8;
        _chatControl.sendButton.styleName = "controlBarButtonSend";
        addControl(_chatControl, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ], CHAT_SECTION);
        addControl(_buttons, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME, UI_VIEWER ], BUTTON_SECTION);

        // add buttons
        addButton(_volBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME, UI_VIEWER ], VOLUME_PRIORITY);
        if (false && DeploymentConfig.devDeployment) {
            addButton(_fullBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME, UI_VIEWER ],
                GLOBAL_PRIORITY);
        }

        addButton(_instructBtn, [ UI_GAME, UI_AVRGAME ]);
        addButton(_shareBtn, [ UI_ROOM, UI_GAME, UI_AVRGAME ]);
        addButton(_commentBtn, [ UI_ROOM, UI_GAME, UI_AVRGAME ]);
        addButton(_avrgBtn, [ UI_AVRGAME ]);
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
        if (_inGame) {
            return UI_GAME;
        } else if (_inAVRGame) {
            return UI_AVRGAME;
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

    /** Are we currently configured to show the controls for a member? */
    protected var _isMember :Boolean;

    /** Are we in a game? */
    protected var _inGame :Boolean;

    /** Are we in alternate reality game? */
    protected var _inAVRGame :Boolean;

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

    /** Indicates AVRG media loading and handles AVRG menu. */
    protected var _avrgBtn :CommandButton;
}
}
