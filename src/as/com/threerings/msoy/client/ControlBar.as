//
// $Id$

package com.threerings.msoy.client {

import flash.display.StageDisplayState;

import flash.utils.Dictionary;

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

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.ui.ScalingMediaContainer;
import com.threerings.msoy.ui.SliderPopup;

import com.threerings.msoy.notify.client.NotificationDisplay;

import com.threerings.msoy.world.client.WorldController;

/**
 * The control bar: the main menu and global UI element across all scenes.
 */
public class ControlBar extends HBox
{
    /** The height of the control bar. This is fixed. */
    public static const HEIGHT :int = 28; // if you feel like changing this, don't.

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
    public static const UI_NONE :String = ""; // when you're logged off.

    public static const ALL_UI_GROUPS :Array = [
        UI_ALL, UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME, UI_VIEWER ];

    /** The chat preferences button. */
    public var chatOptsBtn :CommandButton;

    /** Handles volume adjustment. */
    public var volBtn :CommandButton;

    /** Handles full screening. */
//    public var fullBtn :CommandButton;

    /** Handles commenting on the current scene or game. */
    public var commentBtn :CommandButton;

    /** Handles bringing up a share dialog. */
    public var shareBtn :CommandButton;

    /** Indicates game media loading and handles game menu. */
    public var gameBtn :CommandButton;

    /**
     * Construct.
     */
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
            new ClientAdapter(null, checkControls, checkControls, null, checkControls, null, null,
                checkControls));

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

    public function setNotificationDisplay (notificationDisplay :NotificationDisplay) :void
    {
        _notificationDisplay = notificationDisplay;
        setupControls();
        updateUI();
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
    public function setInGame (inGame :Boolean, icon :MediaDesc = null) :void
    {
        setGameButtonStyle(icon);
        _inGame = inGame;
        updateUI();
    }

    /**
     * Called to tell us when we're in avr game mode.
     */
    public function setInAVRGame (inAVRGame :Boolean, icon :MediaDesc = null) :void
    {
        setGameButtonStyle(icon);
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

    // from Container
    override public function setActualSize (uw :Number, uh :Number) :void
    {
        super.setActualSize(uw, uh);

        if (_notificationDisplay != null && _notificationDisplay.visible) {
            callLater(_notificationDisplay.updatePopupLocation);
        }
    }

    /**
     * Creates the controls we'll be using.
     */
    protected function createControls () :void
    {
        _chatControl = new ChatControl(_ctx, Msgs.GENERAL.get("b.chat_send"));
        _chatControl.chatInput.height = HEIGHT - 8;

        chatOptsBtn = createButton("controlBarButtonChat", "i.channel");
        chatOptsBtn.setCommand(WorldController.POP_CHANNEL_MENU, chatOptsBtn);

        volBtn = createButton(getVolumeStyle(Prefs.getSoundVolume()), "i.volume");
        volBtn.setCallback(handlePopVolume);

//        fullBtn = createButton("controlBarButtonFull", "i.full");
//        fullBtn.setCallback(handleFullScreen);

        commentBtn = createButton("controlBarButtonComment", "i.comment");
        commentBtn.setCommand(MsoyController.VIEW_COMMENT_PAGE);

        shareBtn = createButton("controlBarButtonShare", "i.share");
        shareBtn.toggle = true;
        shareBtn.setCallback(FloatingPanel.createPopper(function () :ShareDialog {
            return new ShareDialog(_ctx);
        }, shareBtn));

        gameBtn = createButton("controlBarGameButton", "i.game");
        gameBtn.setCommand(WorldController.POP_GAME_MENU, gameBtn);
    }

    protected function createButton (style :String, tipKey :String) :CommandButton
    {
        var cb :CommandButton = new CommandButton();
        cb.styleName = style;
        cb.toolTip = Msgs.GENERAL.get(tipKey);
        return cb;
    }

    /**
     * Checks to see which controls the client should see.
     */
    protected function checkControls (... ignored) :void
    {
        const isLoggedOn :Boolean = _ctx.getClient().isLoggedOn();
        chatOptsBtn.enabled = isLoggedOn;
        _chatControl.enabled = isLoggedOn;

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
        DisplayUtil.sortDisplayChildren(volBtn.parent, comparePriority);
    }

    protected function addControls () :void
    {
        // add our standard control bar features
        addControl(chatOptsBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ], CHAT_SECTION);
        addControl(_chatControl, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ], CHAT_SECTION);
        addControl(_buttons, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME, UI_VIEWER ], BUTTON_SECTION);

        // add buttons
        addButton(volBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME, UI_VIEWER ], VOLUME_PRIORITY);
//        if (DeploymentConfig.devDeployment) {
//            addButton(fullBtn, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME, UI_VIEWER ],
//                GLOBAL_PRIORITY);
//        }

        addButton(shareBtn, [ UI_ROOM, UI_GAME, UI_AVRGAME ]);
        addButton(commentBtn, [ UI_ROOM, UI_AVRGAME ]);
        addButton(gameBtn, [ UI_GAME, UI_AVRGAME ], PLACE_PRIORITY + 1);

        if (_notificationDisplay != null) {
            addControl(_notificationDisplay, [ UI_BASE, UI_ROOM, UI_GAME, UI_AVRGAME ],
                NOTIFICATION_SECTION);
        }
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

    protected function setGameButtonStyle (icon :MediaDesc) :void
    {
        if (icon == null) {
            gameBtn.styleName = "controlBarGameButton";
        } else {
            var smc :ScalingMediaContainer = new ScalingMediaContainer(22, 22);
            smc.setMediaDesc(icon);
            gameBtn.setStyle("image", smc);
        }
    }

    protected function getVolumeStyle (level :Number) :String
    {
        // if the level is 0, we want to show icon 0,
        // otherwise show a smooth transition between levels 1 and 4
        const icon :int = (level == 0) ? 0 : (1 + Math.round(level * 3));
        return "controlBarButtonVolume" + icon;
    }

    protected function handlePopVolume () :void
    {
        var dfmt :Function = function (value :Number) :String {
            return Msgs.GENERAL.get("i.percent_fmt", ""+Math.floor(value*100));
        };
        SliderPopup.toggle(volBtn, Prefs.getSoundVolume(), updateVolume,
            { styleName: "volumeSlider", tickValues: [ 0, 1 ], dataTipFormatFunction: dfmt });
    }

    protected function updateVolume (level :Number) :void
    {
        Prefs.setSoundVolume(level);
        volBtn.styleName = getVolumeStyle(level);
    }

//    protected function handleFullScreen () :void
//    {
//        try {
//            stage.displayState = (stage.displayState != StageDisplayState.FULL_SCREEN)
//                ? StageDisplayState.FULL_SCREEN
//                : StageDisplayState.NORMAL;
//        } catch (se :SecurityError) {
//            fullBtn.enabled = false;
//        }
//    }

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

    /** Our chat control. */
    protected var _chatControl :ChatControl;

    /** Holds the 22x22 button area. */
    protected var _buttons :ButtonPalette;

    /** Displays incoming notifications. */
    protected var _notificationDisplay :NotificationDisplay;
}
}
