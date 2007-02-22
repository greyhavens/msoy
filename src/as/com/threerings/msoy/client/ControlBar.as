package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.events.MouseEvent;

import flash.media.SoundMixer;
import flash.media.SoundTransform;

import mx.binding.utils.BindingUtils;

import mx.core.ScrollPolicy;

import mx.containers.Canvas;
import mx.containers.HBox;

import mx.controls.Button;
import mx.controls.HSlider;
import mx.controls.Spacer;

import mx.events.FlexEvent;

import com.threerings.flex.CommandButton;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.chat.client.ChatControl;




[Style(name="backgroundSkin", type="Class", inherit="no")]

/**
 * The control bar: the main menu and global UI element across all scenes.
 */
public class ControlBar extends HBox
{
    /** The height of the control bar. This is fixed. */
    private static const HEIGHT :int = 24;
    private static const WIDTH :int = 800;

    public function ControlBar (ctx :WorldContext)
    {
        _ctx = ctx;
        styleName = "controlBar";

        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;

        height = HEIGHT;
        width = WIDTH;

        // TODO: move volume controller to a separate pop-up (robert)
        var volume :HSlider = new HSlider();
        volume.tickInterval = .1;
        volume.liveDragging = true;
        volume.minimum = 0;
        volume.maximum = 1;
        volume.x = 311;
        volume.y = 41;
        volume.width = 50;
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

            var cls :Class = getStyle("backgroundSkin");
            setStyle("backgroundImage", cls);
            
            var chatControl :ChatControl = new ChatControl(_ctx, this);
            addChild(chatControl);

            var sp :Spacer = new Spacer();
            sp.width = 20;
            addChild (sp);
            
            var chatBtn :CommandButton = new CommandButton();
            chatBtn.setCommand(MsoyController.POP_FRIENDS_MENU, chatBtn);
            chatBtn.styleName = "controlBarButtonChat";
            addChild(chatBtn);

            var volBtn :CommandButton = new CommandButton();
            volBtn.setCommand(MsoyController.POP_PREFS_MENU, volBtn); // TODO
            volBtn.styleName = "controlBarButtonVolume";
            addChild(volBtn);
            
            var prefsBtn :CommandButton = new CommandButton();
            prefsBtn.setCommand(MsoyController.POP_PREFS_MENU, prefsBtn);
            prefsBtn.styleName = "controlBarButtonAvatar";
            addChild(prefsBtn);

            var editBtn :CommandButton = new CommandButton();
            editBtn.setCommand(MsoyController.POP_ROOMS_MENU, editBtn);
            editBtn.styleName = "controlBarButtonEdit";
            addChild(editBtn);

            var footerLeft :SkinnableImage = new SkinnableImage();
            footerLeft.styleName = "controlBarFooterLeft";
            addChild (footerLeft);
            
            var spacer :Spacer = new Spacer();
            spacer.percentWidth = 100;
            addChild(spacer);
            
            var footerRight :SkinnableImage = new SkinnableImage();
            footerRight.styleName = "controlBarFooterRight";
            addChild (footerRight);

            // TODO: other options to consider:
            // MsoyController.SHOW_PETS
            // MsoyController.SHOW_FRIENDS
            
        } else {
            setStyle("backgroundImage", null);
            var logonPanel :LogonPanel = new LogonPanel(_ctx, this);
            addChild(logonPanel);
        }

        // and remember how things are set for now
        _isMember = isMember;
    }

    /** Our clientside context. */
    protected var _ctx :WorldContext;

    /** Are we currently configured to show the controls for a member? */
    protected var _isMember :Boolean;
}
}


import flash.display.DisplayObject;
import mx.controls.Image;
import mx.core.IFlexDisplayObject;

/** Internal: helper function that extends ms.control.Image functionality
    with automatic image loading from the style sheet (e.g. via an
    external style sheet file). */
[Style(name="backgroundSkin", type="Class", inherit="no")]
internal class SkinnableImage extends Image
{
    public function SkinnableImage ()
    {
    }

    override public function styleChanged(styleProp:String):void
    {
        super.styleChanged(styleProp);

        var cls : Class = Class(getStyle("backgroundSkin"));
        if (cls != null) {
            updateSkin (cls);
        }
    }

    protected function updateSkin (skinclass : Class) : void
    {
        if (_skin != null) {
            removeChild (_skin);
        }
        
        _skin = DisplayObject (IFlexDisplayObject (new skinclass()));
        this.width = _skin.width;
        this.height = _skin.height;
        _skin.x = 0;
        _skin.y = 0;
        addChild (_skin);
    }

    protected var _skin : DisplayObject;

    

}
