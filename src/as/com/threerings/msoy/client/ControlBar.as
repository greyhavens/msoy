//
// $Id$

package com.threerings.msoy.client {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.core.Container;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.binding.utils.BindingUtils;
import mx.binding.utils.ChangeWatcher;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.controls.Button;
import mx.controls.Spacer;

import mx.events.FlexEvent;

import com.threerings.flex.CommandButton;
import com.threerings.util.ValueEvent;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.world.client.RoomController;

import com.threerings.msoy.chat.client.ChatControl;

[Style(name="backgroundSkin", type="Class", inherit="no")]

/**
 * The control bar: the main menu and global UI element across all scenes.
 */
public class ControlBar extends HBox
{
    /** The height of the control bar. This is fixed. */
    public static const HEIGHT :int = 24;

    public function ControlBar (ctx :WorldContext, top :TopPanel)
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
        };
        _ctx.getClient().addClientObserver(new ClientAdapter(fn, fn, null, null, null, null, fn));

        _controller = new ControlBarController(ctx, top, this);

        addEventListener(Event.ADDED_TO_STAGE, handleAddRemove);
        addEventListener(Event.REMOVED_FROM_STAGE, handleAddRemove);

        _ctx.getClient().addEventListener(WorldClient.MINIZATION_CHANGED, minimizationChanged);
        _ctx.getClient().addEventListener(WorldClient.EMBEDDED_STATE_KNOWN, embeddedStateKnown);

        checkControls();
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
     * Called by the ChannelChatPanel when it needs to stuff a chat input field into the control
     * bar while it's open. Setting it to null removes it.
     */
    public function setChannelChatInput (input :Container) :void
    {
        if (_channelInput != null) {
            removeChild(_channelInput);
            _channelInput = null;
        }
        if (input != null) {
            var chidx :int = -1;
            // TEMP: non-admins have no channel button for now
            if (_channelBtn == null) {
                chidx = numChildren;
            } else {
                // insert it to the left of the channel button
                chidx = getChildIndex(_channelBtn);
            }
            if (chidx >= 0) {
                addChildAt(_channelInput = input, chidx);
            }
        }
    }

    /**
     * Check to see which controls the client should see.
     */
    protected function checkControls () :void
    {
        var user :MemberObject = _ctx.getMemberObject();
        var isMember :Boolean = (user != null) && !user.isGuest();
        if (numChildren > 0 && (isMember == _isMember)) {
            return;
        }

        removeAllChildren();
        _hiders.length = 0;
        _chatControl = null;
        _avatarBtn = null;
        _editBtn = null;
        _channelBtn = null;

        if (isMember) {
            addHideableChild(_chatControl = new ChatControl(_ctx, this.height - 4));

            var chatBtn :CommandButton = new CommandButton();
            chatBtn.toolTip = Msgs.GENERAL.get("i.chatPrefs");
            chatBtn.setCommand(MsoyController.CHAT_PREFS);
            chatBtn.styleName = "controlBarButtonChat";
            addHideableChild(chatBtn);

            var volBtn :CommandButton = new CommandButton();
            volBtn.toolTip = Msgs.GENERAL.get("i.volume");
            volBtn.setCommand(ControlBarController.POP_VOLUME, volBtn);
            volBtn.styleName = "controlBarButtonVolume";
            addHideableChild(volBtn);

            _avatarBtn = new CommandButton();
            _avatarBtn.toolTip = Msgs.GENERAL.get("i.avatar");
            _avatarBtn.setCommand(MsoyController.PICK_AVATAR);
            _avatarBtn.styleName = "controlBarButtonAvatar";
            addHideableChild(_avatarBtn);

            _petBtn = new CommandButton();
            _petBtn.toolTip = Msgs.GENERAL.get("i.pet");
            _petBtn.setCommand(MsoyController.SHOW_PETS);
            _petBtn.styleName = "controlBarButtonPet";
            addHideableChild(_petBtn);

            _editBtn = new CommandButton();
            _editBtn.toolTip = Msgs.GENERAL.get("i.editScene");
            _editBtn.setCommand(ControlBarController.EDIT_SCENE);
            _editBtn.styleName = "controlBarButtonEdit";
            _editBtn.enabled = false;
            addHideableChild(_editBtn);

        } else {
            if (_ctx.getWorldClient().isEmbedded()) {
                addHideableChild(_logonPanel = new LogonPanel(_ctx, this.height - 4));
            }

            volBtn = new CommandButton();
            volBtn.setCommand(ControlBarController.POP_VOLUME, volBtn);
            volBtn.styleName = "controlBarButtonVolume";
            addHideableChild(volBtn);
        }

        // some elements that are common to guest and logged in users
        var footerLeft :SkinnableImage = new SkinnableImage();
        footerLeft.styleName = "controlBarFooterLeft";
        addHideableChild(footerLeft);

        var blank :Canvas = new Canvas();
        blank.styleName = "controlBarSpacer";
        blank.height = this.height;
        blank.percentWidth = 100;
        addChild(blank);

        // don't do navigation down here for now...
        /*_goback = new CommandButton();
        _goback.setCommand(ControlBarController.MOVE_BACK, _goback);
        _goback.styleName = "controlBarButtonGoBack";
        addHideableChild(_goback);

        _loc = new CanvasWithText(this.height - 4);
        _loc.styleName = "controlBarLocationText";
        _loc.height = this.height;
        _loc.width = 200;
        addHideableChild(_loc);

        _bookend = new SkinnableImage();
        _bookend.styleName = "controlBarBookend";
        addHideableChild(_bookend);*/

        var footerRight :SkinnableImage = new SkinnableImage();
        footerRight.styleName = "controlBarFooterRight";
        addHideableChild(footerRight);

        if (isMember && user.tokens.isSupport()) {
            _channelBtn = new CommandButton();
            _channelBtn.toolTip = Msgs.GENERAL.get("i.channel");
            _channelBtn.setCommand(MsoyController.POP_CHANNEL_MENU, _channelBtn);
            _channelBtn.styleName = "controlBarButtonAvatar";
            addChild(_channelBtn);
        }

        // and remember how things are set for now
        _isMember = isMember;

        recheckAvatarControl();
    }

    protected function addHideableChild (child :UIComponent) :void
    {
        addChild(child);
        _hiders.push(child);
    }

    protected function embeddedStateKnown (event :ValueEvent) :void
    {
        var embedded :Boolean = (event.value as Boolean);
        // no logon panel if we're not in embedded mode
        if (!embedded && _logonPanel != null) {
            removeChild(_logonPanel);
        }
    }

    protected function minimizationChanged (event :ValueEvent) :void
    {
        var minimized :Boolean = (event.value as Boolean);
        for each (var child :UIComponent in _hiders) {
            child.visible = !minimized;
            child.includeInLayout = !minimized;
        }
    }

    protected function handleAddRemove (event :Event) :void
    {
        var added :Boolean = (event.type == Event.ADDED_TO_STAGE);
        _controller.registerForSessionObservations(added);

        if (added) {
            _avatarControlWatcher = BindingUtils.bindSetter(
                recheckAvatarControl, _ctx.worldProps, "userControlsAvatar");

        } else {
            _avatarControlWatcher.unwatch();
            _avatarControlWatcher = null;
        }
    }

    protected function recheckAvatarControl (... ignored) :void
    {
        if (_avatarBtn != null) {
            _avatarBtn.enabled = _ctx.worldProps.userControlsAvatar;
        }
    }

    /** Changes the visibility and parameters of the navigation widgets.
     *  @param visible controls visibility of both the name and the back button
     *  @param name specifies the location name to be displayed on the control bar
     *  @param backEnabled specifies whether the back button should be enabled
     */
    public function updateNavigationWidgets (
        visible :Boolean, name :String, backEnabled :Boolean) :void
    {
        // don't do navigation here for now...
        /*const maxLen :int = 25;
        _loc.includeInLayout = _goback.includeInLayout = _bookend.includeInLayout =
            _loc.visible = _goback.visible = _bookend.visible = visible;
        _goback.enabled = backEnabled;
        _goback.toolTip = backEnabled ? Msgs.GENERAL.get("i.goBack") : null;
        if (name != null) {
            _loc.text = name.length < maxLen ? name : (name.substr(0, maxLen) + "...");
        } else {
            _loc.text = "";
        }*/
    }

    /** Receives notification whether scene editing is possible for this scene. */
    public function set sceneEditPossible (value :Boolean) :void
    {
        if (_editBtn != null) {
            _editBtn.enabled = value;
        }
    }

    /** Our clientside context. */
    protected var _ctx :WorldContext;

    /** Controller for this object. */
    protected var _controller :ControlBarController;

    /** Are we currently configured to show the controls for a member? */
    protected var _isMember :Boolean;

    /** The back-movement button. */
    protected var _goback :CommandButton;

    /** A list of children that can be hidden when we are minimized. */
    protected var _hiders :Array = new Array();

    /** Our chat control. */
    protected var _chatControl :ChatControl;

    /** Our logon panel (if shown). */
    protected var _logonPanel :LogonPanel;

    /** Button for changing your avatar. */
    protected var _avatarBtn :CommandButton;

    /** Button for managing pets. */
    protected var _petBtn :CommandButton;

    /** Button for editing the current scene. */
    protected var _editBtn :CommandButton;

    /** Button for selecting/creating chat channels. */
    protected var _channelBtn :CommandButton;

    /** Our channel chat input. */
    protected var _channelInput :Container;

    /** Notifies us of changes to the userControlsAvatar property. */
    protected var _avatarControlWatcher :ChangeWatcher;

    /** Current location label. */
    protected var _loc :CanvasWithText;

    /** Bookend image at the other end of name label. */
    protected var _bookend :SkinnableImage;
}
}


import flash.display.DisplayObject;
import flash.text.TextFieldAutoSize;
import mx.containers.Canvas;
import mx.controls.Image;
import mx.core.IFlexDisplayObject;
import mx.core.ScrollPolicy;
import mx.core.UITextField;

/** Internal: helper function that extends ms.control.Image functionality with automatic image
 * loading from the style sheet (e.g. via an external style sheet file). */
[Style(name="backgroundSkin", type="Class", inherit="no")]
internal class SkinnableImage extends Image
{
    public function SkinnableImage ()
    {
    }

    override public function styleChanged (styleProp:String) :void
    {
        super.styleChanged(styleProp);

        var cls : Class = Class(getStyle("backgroundSkin"));
        if (cls != null) {
            updateSkin(cls);
        }
    }

    protected function updateSkin (skinclass : Class) : void
    {
        if (_skin != null) {
            removeChild(_skin);
        }

        _skin = DisplayObject (IFlexDisplayObject (new skinclass()));
        this.width = _skin.width;
        this.height = _skin.height;
        _skin.x = 0;
        _skin.y = 0;
        addChild(_skin);
    }

    protected var _skin : DisplayObject;
}

/** Internal: helper class that extends ms.containers.Canvas
    with automatic background loading from the style sheet (e.g. via an
    external style sheet file). */
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
