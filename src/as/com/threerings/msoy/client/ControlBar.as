package com.threerings.msoy.client {

import flash.events.MouseEvent;

import flash.media.SoundMixer;
import flash.media.SoundTransform;

import mx.binding.utils.BindingUtils;

import mx.core.ScrollPolicy;

import mx.containers.Canvas;

import mx.controls.Button;
import mx.controls.HSlider;

import mx.events.FlexEvent;

import com.threerings.flex.CommandButton;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.chat.client.ChatControl;

/**
 * The control bar: the main menu and global UI element across all scenes.
 */
public class ControlBar extends Canvas
{
    /** The height of the control bar. This is fixed. */
    public static const HEIGHT :int = 59;
    public static const WIDTH :int = 800;

    public function ControlBar (ctx :MsoyContext)
    {
        _ctx = ctx;

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        height = HEIGHT;
        width = WIDTH;

        var volume :HSlider = new HSlider();
        volume.tickInterval = .1;
        volume.liveDragging = true;
        volume.minimum = 0;
        volume.maximum = 1;
        volume.x = 311;
        volume.y = 41;
        volume.width = 190;
        SoundMixer.soundTransform = new SoundTransform(Prefs.getSoundVolume());
        volume.value = SoundMixer.soundTransform.volume;
        BindingUtils.bindSetter(function (val :Number) :void {
            SoundMixer.soundTransform = new SoundTransform(val);
            Prefs.setSoundVolume(val);
        }, volume, "value");
        addChild(volume);

        var fn :Function = function (event :ClientEvent) :void {
            checkControls();
        };
        _ctx.getClient().addClientObserver(
            new ClientAdapter(fn, fn, null, null, null, null, fn));

        checkControls();
    }

    /**
     * Check to see which controls the client should see.
     */
    protected function checkControls () :void
    {
        var user :MemberObject = _ctx.getClientObject();
        var isMember :Boolean = (user != null) && !user.isGuest();
        if (numChildren > 1 && (isMember == _isMember)) {
            return;
        }

        // remove all children (except the volume slider)
        while (numChildren > 1) {
            removeChildAt(1);
        }

        if (isMember) {
            [Embed(source="../../../../../../rsrc/media/uibar.png")]
            var cls :Class;
            setStyle("backgroundImage", cls);

            var chatControl :ChatControl = new ChatControl(_ctx);
            chatControl.x = 10;
            chatControl.y = 10;
            addChild(chatControl);

            // set up buttons
            var petsBtn :CommandButton = new CommandButton();
            petsBtn.styleName = BUTTON_STYLE;
            petsBtn.setCommand(MsoyController.SHOW_PETS);

            petsBtn.x = 546;
            petsBtn.y = 0;
            petsBtn.width = 38;
            petsBtn.height = HEIGHT;
            addChild(petsBtn);

            var friendsBtn :CommandButton = new CommandButton();
            friendsBtn.styleName = BUTTON_STYLE;
            friendsBtn.setCommand(MsoyController.SHOW_FRIENDS);
            friendsBtn.toggle = true;

            // TODO: dynamic layout?
            friendsBtn.x = 585;
            friendsBtn.y = 0;
            friendsBtn.width = 19;
            friendsBtn.height = HEIGHT;
            addChild(friendsBtn);

            // a second friends button, for now
            friendsBtn = new CommandButton();
            friendsBtn.styleName = BUTTON_STYLE;
            friendsBtn.setCommand(MsoyController.POP_FRIENDS_MENU, friendsBtn);

            friendsBtn.x = 585 + 19;
            friendsBtn.y = 0;
            friendsBtn.width = 19;
            friendsBtn.height = HEIGHT;
            addChild(friendsBtn);

            var scenesBtn :CommandButton = new CommandButton();
            scenesBtn.styleName = BUTTON_STYLE;
            scenesBtn.setCommand(MsoyController.POP_ROOMS_MENU, scenesBtn);

            scenesBtn.x = 624
            scenesBtn.y = 0;
            scenesBtn.width = 38;
            scenesBtn.height = HEIGHT;
            addChild(scenesBtn);

            // settings, prefs, whatever we want to call them
            var prefsBtn :CommandButton = new CommandButton();
            prefsBtn.styleName = BUTTON_STYLE;
            prefsBtn.setCommand(MsoyController.POP_PREFS_MENU, prefsBtn);
            prefsBtn.x = 753;
            prefsBtn.y = 0;
            prefsBtn.width = 47;
            prefsBtn.height = HEIGHT;
            addChild(prefsBtn);

        } else {
            setStyle("backgroundImage", null);
            var logonPanel :LogonPanel = new LogonPanel(_ctx);
            logonPanel.x = 10;
            logonPanel.y = 10;
            addChild(logonPanel);
        }

        // and remember how things are set for now
        _isMember = isMember;
    }

    /** Our clientside context. */
    protected var _ctx :MsoyContext;

    /** Are we currently configured to show the controls for a member? */
    protected var _isMember :Boolean;

    /** The style class used by buttons appearing on this bar. */
    protected static const BUTTON_STYLE :String = "controlBarButtons";
}
}
