//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Bitmap;
import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.external.ExternalInterface;
import flash.text.TextField;
import flash.text.TextFieldAutoSize;
import flash.text.TextFormat;

import flash.utils.ByteArray;

import mx.binding.utils.BindingUtils;

import mx.containers.Canvas;
import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.ComboBox;
import mx.controls.HSlider;
import mx.controls.Label;

import mx.events.FlexEvent;

import com.threerings.flash.FPSDisplay;
import com.threerings.flash.MediaContainer;
import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandMenu;
import com.threerings.util.ParameterUtil;
import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.msoy.ui.MsoyUI;
import com.threerings.msoy.world.client.OccupantSprite;

public class AvatarViewerComp extends VBox
{
    public function AvatarViewerComp ()
    {
    }

    /**
     * Load the media to display as a byteArray.
     */
    public function loadBytes (bytes :ByteArray) :void
    {
        var avatar :ViewerAvatarSprite;
        for each (avatar in _avatars) {
            avatar.shutdown();
            _holder.rawChildren.removeChild(avatar);
        }
        _avatars.length = 0;

        avatar = new ViewerAvatarSprite(this);
        _avatars.push(avatar);

        avatar.setMediaBytes(bytes);
        _holder.rawChildren.addChild(avatar);

        // on the last one, add a listener
        avatar.addEventListener(MouseEvent.CLICK, handleMouseClick);
        avatar.addEventListener(MediaContainer.SIZE_KNOWN, handleSizeKnown);

        setMode(_mode);
        setOrient(_orient);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        percentWidth = 100;
        percentHeight = 100;

        // create an HBox to hold "walking", "facing", "talk"
        var controls: HBox = new HBox();
        controls.percentWidth = 100;
        controls.setStyle("verticalAlign", "middle");
        controls.setStyle("horizontalAlign", "center");

        var mode :ComboBox = new ComboBox();
        mode.dataProvider = [ "Standing", "Walking", "Idle" ];
        mode.selectedIndex = 0;
        controls.addChild(mode);

        controls.addChild(MsoyUI.createLabel("Facing angle:"));

        var canv :Canvas = new Canvas();
        canv.width = OrientationControl.SIZE;
        canv.height = OrientationControl.SIZE;
        canv.rawChildren.addChild(new OrientationControl(setOrient));
        controls.addChild(canv);

        controls.addChild(new CommandButton("Talk!", speak));
        addChild(controls);

        // create an HBox to hold the scaling controls
        _scaleControls = new HBox();
        _scaleControls.percentWidth = 100;
        _scaleControls.setStyle("horizontalAlign", "center");
        createScaleControls();
        addChild(_scaleControls);

        addChild(_holder = new Canvas());
        _holder.percentWidth = 100;
        _holder.percentHeight = 100;

        // draw a gradient background
        var bmp :Bitmap = Bitmap(new BACKGROUND());
        var bgsprite :Sprite = new Sprite();
        bgsprite.graphics.beginBitmapFill(bmp.bitmapData);
        bgsprite.graphics.drawRect(0, 0, 1000, 1000);
        bgsprite.graphics.endFill();
        _holder.rawChildren.addChildAt(bgsprite, 0);
        // Disabled, because it confused the "usability testers" to have the menu always
        // popping up.
        //bgsprite.addEventListener(MouseEvent.CLICK, handleMouseClick);

        // bind actions to the user interface elements
        BindingUtils.bindSetter(setMode, mode, "selectedIndex");

        // finally, load our parameters and see what we should do.
        ParameterUtil.getParameters(this, gotParams);
    }

    /**
     * Configure the scaling controls.
     */
    protected function createScaleControls () :void
    {
        _scaleReset = new CommandButton("Reset scale", function () :void {
            _scaleSlider.value = 1;
        });

        _scaleSlider = new HSlider();
        _scaleSlider.liveDragging = true;
        _scaleSlider.minimum = 0;
        _scaleSlider.maximum = int.MAX_VALUE;
        _scaleSlider.value = 1;
        _scaleSlider.enabled = false;
        _scaleSlider.tickValues = [ 1 ];
        BindingUtils.bindSetter(scaleUpdated, _scaleSlider, "value");

        _scaleControls.addChild(_scaleSlider);
        _scaleControls.addChild(_scaleReset);
    }

    /**
     * Our ParameterUtil callback.
     */
    protected function gotParams (params :Object) :void
    {
        var media :String = params["avatar"] as String;
        var scale :Number = Number(params["scale"]);
        if (isNaN(scale) || (scale == 0)) {
            scale = 1;
        }
        _scaleSlider.value = scale;

        // see if we need to turn off the scaling controls
        var scaling :Boolean = "true" == String(params["scaling"]);
        if (!scaling) {
            _scaleControls.includeInLayout = false;
            _scaleControls.visible = false;
        }

        if (StringUtil.isBlank(media)) {
            var msg :String = params["message"] as String;
            if (msg != null) {
                var av :ViewerAvatarSprite = new ViewerAvatarSprite(this);
                _avatars.push(av);
                _holder.rawChildren.addChild(av);

                var tf :TextField = new TextField();
                tf.autoSize = TextFieldAutoSize.LEFT;
                tf.defaultTextFormat = new TextFormat(null, 24);
                tf.text = msg;
                tf.width = tf.textWidth + 5;
                av.setMediaObject(tf);
            }
            return;
        }

        var count :int = 1;
        var testing :Boolean = false;
        if ("count" in params) {
            count = int(params["count"]);
            testing = true;
        }

        for (var ii :int = count; ii > 0; ii--) {
            var avatar :ViewerAvatarSprite = new ViewerAvatarSprite(this, scale);
            _avatars.push(avatar);

            avatar.setMedia(media);
            _holder.rawChildren.addChild(avatar);

            if (ii == 1) {
                // on the last one, add a listener
                avatar.addEventListener(MouseEvent.CLICK, handleMouseClick);
                avatar.addEventListener(MediaContainer.SIZE_KNOWN, handleSizeKnown);
            }
        }

        if (testing) {
            var fps :FPSDisplay = new FPSDisplay(100);
            fps.y = _holder.height - fps.height;
            fps.background = false; // it's readable enough in front of our background
            _holder.rawChildren.addChild(fps);

            var countLabel :TextField = new TextField();
            countLabel.text = "Testing: " + count + " avatars";
            countLabel.width = countLabel.textWidth + 5;
            countLabel.height = countLabel.textHeight + 4;
            countLabel.x = _holder.width - countLabel.width;
            countLabel.y = _holder.height - countLabel.height;
            _holder.rawChildren.addChild(countLabel);
        }
    }

    protected function handleSizeKnown (event :ValueEvent) :void
    {
        var width :int = int(event.value[0]);
        var height :int = int(event.value[1]);

        // the minimum scale makes things 10 pixels in a dimension
        var minScale :Number = Math.max(10 / width, 10 / height);
        // the maximum bumps us up against the overall maximums
        var maxScale :Number = Math.min(OccupantSprite.MAX_WIDTH / width,
                                        OccupantSprite.MAX_HEIGHT / height);

        // but we always ensure that scale 1.0 is selectable, even if it seems it shouldn't be.
        _scaleSlider.minimum = Math.min(1, minScale);
        _scaleSlider.maximum = Math.max(1, maxScale);

        // enable everything
        _scaleSlider.enabled = true;
        scaleUpdated();
    }

    protected function speak () :void
    {
        for each (var avatar :ViewerAvatarSprite in _avatars) {
            avatar.performAvatarSpoke();
        }
    }

    protected function setMode (mode :int) :void
    {
        _mode = mode;
        for each (var avatar :ViewerAvatarSprite in _avatars) {
            avatar.setMoving(mode == 1);
            avatar.setIdle(mode == 2);
        }
    }

    protected function setOrient (val :Number) :void
    {
        var orient :int = int(val);
        _orient = orient;

        for each (var avatar :ViewerAvatarSprite in _avatars) {
            avatar.setOrientation(orient);
        }
    }

    protected function scaleUpdated (... ignored) :void
    {
        var scale :Number = _scaleSlider.value;

        _scaleReset.enabled = (scale != 1);

        for each (var avatar :ViewerAvatarSprite in _avatars) {
            avatar.setScale(scale);
        }

        if (_scaleControls.visible && ExternalInterface.available) {
            try {
                ExternalInterface.call("updateAvatarScale", scale);
            } catch (e :Error) {
                trace(e);
            }
        }
    }

    protected function setState (state :String) :void
    {
        for each (var avatar :ViewerAvatarSprite in _avatars) {
            avatar.setState(state);
        }
    }

    protected function triggerAction (action :String) :void
    {
        for each (var avatar :ViewerAvatarSprite in _avatars) {
            avatar.performAvatarAction(action);
        }
    }

    /**
     * Handles a mouse click on the first avatar component.
     */
    public function handleMouseClick (event :MouseEvent) :void
    {
        if (_avatars.length == 0) {
            return;
        }

        var sprite :ViewerAvatarSprite = ViewerAvatarSprite(_avatars[0]);

        var actions :Array = sprite.getAvatarActions();
        var actionItems :Array = [];
        for each (var act :String in actions) {
            actionItems.push({ label: act, callback: triggerAction,
                arg: act });
        }

        var states :Array = sprite.getAvatarStates();
        var stateItems :Array = [];
        for each (var state :String in states) {
            stateItems.push({ label: state, callback: setState,
                arg: state });
        }

        var menuItems :Array = [];
        if (actionItems.length > 0) {
            menuItems.push({ label: "Perform action...",
                children: actionItems });
        }
        if (stateItems.length > 0) {
            menuItems.push({ label: "Change state...",
                children: stateItems });
        }
        if (menuItems.length == 0) {
            menuItems.push({ label: "No actions" });
        }

        CommandMenu.createMenu(menuItems).popUpIn(getBounds(stage));
    }

    /** The container that holds our non-flex children. */
    protected var _holder :Canvas;

    /** The avatars with which we're testing. Normally contains just 1. */
    protected var _avatars :Array = [];

    /** Holds scaling controls, only visible sometimes. */
    protected var _scaleControls :HBox;

    protected var _orient :int;
    protected var _mode :int;

    protected var _scaleSlider :HSlider;
    protected var _scaleReset :CommandButton;

    [Embed(source="../../../../../../../pages/images/item/detail_preview_bg.png")]
    protected static const BACKGROUND :Class;
}
}

import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.MouseEvent;

import flash.geom.Point;

import mx.core.UIComponent;

import com.threerings.msoy.world.client.MemberSprite;

class ViewerAvatarSprite extends MemberSprite
{
    public function ViewerAvatarSprite (comp :UIComponent, scale :Number = 1)
    {
        super(null);
        _comp = comp;
        _scale = scale; // defined in MemberSprite
        configureMouseProperties();
    }

    public function setMoving (moving :Boolean) :void
    {
        _moving = moving;
        appearanceChanged();
    }

    public function setIdle (idle :Boolean) :void
    {
        _idle = idle;
        appearanceChanged();
    }

    override public function isMoving () :Boolean
    {
        return _moving;
    }

    override public function isIdle () :Boolean
    {
        return _idle;
    }

    public function setScale (scale :Number) :void
    {
        _scale = scale;
        scaleUpdated();
        x = (_comp.width - width)/2;
        y = Math.max(0, (_comp.height - height - parent.y));
    }

    override public function getState () :String
    {
        return _state;
    }

    override public function setState (state :String) :void
    {
        _state = state;
        callUserCode("stateSet_v1", state);
    }

    /**
     * Callback adapter used in our menu.
     */
    public function performAvatarAction (action :String) :void
    {
        messageReceived(action, null, true);
    }

    override public function sendMessage (
        name :String, arg :Object, isAction :Boolean) :void
    {
        // route this directly through
        messageReceived(name, arg, isAction);
    }

    override public function requestControl () :void
    {
        gotControl();
    }

    override protected function stoppedLoading () :void
    {
        super.stoppedLoading();

        // fake that we got control
        gotControl();
    }

    override protected function allowSetMedia () :Boolean
    {
        return true;
    }

    protected var _comp :UIComponent;

    protected var _moving :Boolean = false;

    protected var _idle :Boolean = false;

    protected var _state :String;
}

class OrientationControl extends Sprite
{
    public static const SIZE :int = 30;

    public function OrientationControl (orientSetter :Function)
    {
        _setter = orientSetter;

        // draw transparent pixels to grab all mouse events
        var g :Graphics = graphics;
        g.beginFill(0xFFFFFF, 0);
        g.drawRect(0, 0, SIZE, SIZE);
        g.endFill();

        // draw a circle indicating the control area
        g.beginFill(0xCCCCCC);
        g.drawCircle(SIZE/2, SIZE/2, SIZE/2);
        g.endFill();

        // create Mr. wee arrow sprite.
        var arrow :Sprite = new Sprite();
        arrow.mouseEnabled = false;
        arrow.x = SIZE/2;
        arrow.y = SIZE/2;
        addChild(arrow);
        _arrowG = arrow.graphics;

        addEventListener(MouseEvent.MOUSE_MOVE, handleMouseMove);
        setArrow(new Point());
    }

    protected function handleMouseMove (event :MouseEvent) :void
    {
        setArrow(new Point(event.localX - SIZE/2, event.localY - SIZE/2));
    }

    protected function setArrow (p :Point) :void
    {
        // we always want some sort of orientation, so point straight ahead if the
        // point is 0,0
        if (p.x == 0 && p.y == 0) {
            p.y = 1;
        }
        p.normalize(SIZE/2);
        _arrowG.clear();
        _arrowG.lineStyle(3, 0x003333);
        _arrowG.moveTo(0, 0);
        _arrowG.lineTo(p.x, p.y);

        var orient :Number = (360 + 90 + (180 / Math.PI * Math.atan2(-p.y, p.x))) % 360;
        _setter(orient);
    }

    /** The setter function to use. */
    protected var _setter :Function;

    /** The graphics for drawing on the arrow sprite. */
    protected var _arrowG :Graphics;
}
