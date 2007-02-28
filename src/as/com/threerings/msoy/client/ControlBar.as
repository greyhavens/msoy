package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.events.MouseEvent;

import mx.core.ScrollPolicy;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.controls.Button;
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

        var fn :Function = function (event :ClientEvent) :void {
            checkControls();
        };
        _ctx.getClient().addClientObserver(
            new ClientAdapter(fn, fn, null, null, null, null, fn));

        _controller = new ControlBarController(ctx, this);
        
        checkControls();
    }

    // from HBox
    override public function parentChanged (p :DisplayObjectContainer) :void
    {
        super.parentChanged(p);
        _controller.registerForSessionObservations (p != null);
    }


    /**
     * Check to see which controls the client should see.
     */
    protected function checkControls () :void
    {
        var cls :Class = getStyle("backgroundSkin");
        setStyle("backgroundImage", cls);
            
        var user :MemberObject = _ctx.getClientObject();
        var isMember :Boolean = (user != null) && !user.isGuest();
        if (numChildren > 1 && (isMember == _isMember)) {
            return;
        }

        removeAllChildren();
        
        if (isMember) {

            var chatControl :ChatControl = new ChatControl(_ctx, this.height - 4);
            addChild(chatControl);

            var sp :Spacer = new Spacer();
            sp.width = 20;
            addChild(sp);
            
            var chatBtn :CommandButton = new CommandButton();
            chatBtn.setCommand(MsoyController.POP_FRIENDS_MENU, chatBtn);
            chatBtn.styleName = "controlBarButtonChat";
            addChild(chatBtn);

            var volBtn :CommandButton = new CommandButton();
            volBtn.setCommand(ControlBarController.POP_VOLUME, volBtn);
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

            // TODO: other options to consider:
            // MsoyController.SHOW_PETS
            // MsoyController.SHOW_FRIENDS
            
        } else {
            var logonPanel :LogonPanel = new LogonPanel(_ctx, this.height - 4);
            addChild(logonPanel);

            sp = new Spacer();
            sp.width = 20;
            addChild(sp);

            volBtn = new CommandButton();
            volBtn.setCommand(ControlBarController.POP_VOLUME, volBtn);
            volBtn.styleName = "controlBarButtonVolume";
            addChild(volBtn);
        }

        // some elements that are common to guest and logged in users
        var footerLeft :SkinnableImage = new SkinnableImage();
        footerLeft.styleName = "controlBarFooterLeft";
        addChild (footerLeft);

        var blank :SkinnableCanvas = new SkinnableCanvas();
        blank.styleName = "controlBar";
        blank.height = this.height;
        blank.percentWidth = 100;
        addChild(blank);
        
        _goback = new CommandButton();
        _goback.setCommand(ControlBarController.MOVE_BACK, _goback);
        _goback.styleName = "controlBarButtonGoBack";
        addChild(_goback);

        _loc = new SkinnableCanvasWithText(this.height - 4);
        _loc.styleName = "controlBarLocationText";
        _loc.height = this.height;
        _loc.width = 200;
        addChild(_loc);

        var bookend :SkinnableImage = new SkinnableImage();
        bookend.styleName = "controlBarBookend";
        addChild(bookend);
        
        var footerRight :SkinnableImage = new SkinnableImage();
        footerRight.styleName = "controlBarFooterRight";
        addChild(footerRight);
        
        // and remember how things are set for now
        _isMember = isMember;
    }

    /** Receives notifications whether movement back to the previous scene is possible,
        and adjusts the back-button appropriately. */
    public function set backMovementPossible (value :Boolean) :void
    {
        _goback.enabled = value;
    }

    /** Sets location label. */
    public function set locationLabel (value :String) :void
    {
        _loc.text = value;
    }

        
    // IMPLEMENTATION DETAILS

    /** Our clientside context. */
    protected var _ctx :WorldContext;

    /** Controller for this object. */
    protected var _controller :ControlBarController;

    /** Are we currently configured to show the controls for a member? */
    protected var _isMember :Boolean;

    /** The back-movement button. */
    protected var _goback :CommandButton;

    /** Current location label. */
    protected var _loc :SkinnableCanvasWithText;
}
}


import flash.display.DisplayObject;
import flash.text.TextFieldAutoSize;
import mx.containers.Canvas;
import mx.controls.Image;
import mx.core.IFlexDisplayObject;
import mx.core.ScrollPolicy;
import mx.core.UITextField;

/** Internal: helper function that extends ms.control.Image functionality
    with automatic image loading from the style sheet (e.g. via an
    external style sheet file). */
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
[Style(name="backgroundSkin", type="Class", inherit="no")]
internal class SkinnableCanvas extends Canvas
{
    public function SkinnableCanvas ()
    {
    }

    override public function styleChanged (styleProp:String) :void
    {
        super.styleChanged(styleProp);
        var cls :Class = getStyle("backgroundSkin");
        setStyle("backgroundImage", cls);
    }
}

/** Internal: helper class that extends ms.containers.Canvas
    with automatic background loading from the style sheet (e.g. via an
    external style sheet file). */
internal class SkinnableCanvasWithText extends SkinnableCanvas
{
    public var textfield :UITextField;
    
    public function SkinnableCanvasWithText (height :int)
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


