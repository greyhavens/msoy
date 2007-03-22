//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.Bitmap;
import flash.display.Sprite;

import flash.events.MouseEvent;

import flash.text.TextField;

import mx.binding.utils.BindingUtils;

import mx.containers.Canvas;
import mx.containers.Grid;

import mx.controls.Button;
import mx.controls.CheckBox;
import mx.controls.ComboBox;
import mx.controls.HSlider;
import mx.controls.Label;

import mx.events.FlexEvent;

import com.threerings.util.ParameterUtil;

import com.threerings.flash.FPSDisplay;

import com.threerings.flex.CommandMenu;
import com.threerings.flex.GridUtil;

public class AvatarViewerComp extends Canvas
{
    public function AvatarViewerComp ()
    {
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // TODO: replace slider with custom control
        var rotation :HSlider = new HSlider();
        rotation.minimum = -180;
        rotation.maximum = 180;
        rotation.showDataTip = false;
        rotation.snapInterval = 1;
        rotation.liveDragging = true;
        rotation.value = 0;

        var walking :CheckBox = new CheckBox();

        var talking :Button = new Button();
        talking.label = "Talk!";

        var grid :Grid = new Grid();
        GridUtil.addRow(grid, "Walking:", walking);
        GridUtil.addRow(grid, "Facing angle:", rotation);
        GridUtil.addRow(grid, talking);

        _holder = new Canvas();
        GridUtil.addRow(grid, _holder, [2, 1]);
        addChild(grid);

        // bind actions to the user interface elements
        talking.addEventListener(FlexEvent.BUTTON_DOWN, speak);
        BindingUtils.bindSetter(setOrient, rotation, "value");
        BindingUtils.bindSetter(setMoving, walking, "selected");

        // finally, load our parameters and see what we should do.
        ParameterUtil.getParameters(this, gotParams);
    }

    /**
     * Our ParameterUtil callback.
     */
    protected function gotParams (params :Object) :void
    {
        var media :String = String(params["avatar"]);

        var count :int = 1;
        var testing :Boolean = false;
        if ("count" in params) {
            count = int(params["count"]);
            testing = true;
        }

        for (var ii :int = count; ii > 0; ii--) {
            var avatar :ViewerAvatarSprite = new ViewerAvatarSprite();
            _avatars.push(avatar);

            avatar.setMedia(media);
            _holder.rawChildren.addChild(avatar);

            if (ii == 1) {
                // on the last one, add a listener
                avatar.addEventListener(MouseEvent.CLICK, handleMouseClick);

                _holder.width = avatar.getMaxContentWidth();
                _holder.height = avatar.getMaxContentHeight();

                // draw a gradient background
                var bmp :Bitmap = Bitmap(new BACKGROUND());
                var sprite :Sprite = new Sprite();
                sprite.graphics.beginBitmapFill(bmp.bitmapData);
                sprite.graphics.drawRect(0, 0, _holder.width, _holder.height);
                sprite.graphics.endFill();
                _holder.rawChildren.addChildAt(sprite, 0);

                // add a click listener on the background area, too
                sprite.addEventListener(MouseEvent.CLICK, handleMouseClick);
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

    protected function speak (... ignored) :void
    {
        for each (var avatar :ViewerAvatarSprite in _avatars) {
            avatar.performAvatarSpoke();
        }
    }

    protected function setMoving (moving :Boolean) :void
    {
        for each (var avatar :ViewerAvatarSprite in _avatars) {
            avatar.setMoving(moving);
        }
    }

    protected function setOrient (val :Number) :void
    {
        var orient :int = int(val + 360) % 360;

        for each (var avatar :ViewerAvatarSprite in _avatars) {
            avatar.setOrientation(orient);
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

        CommandMenu.createMenu(menuItems).show(event.stageX, event.stageY);
    }

    /** The container that holds our non-flex children. */
    protected var _holder :Canvas;

    /** The avatars with which we're testing. Normally contains just 1. */
    protected var _avatars :Array = [];

    [Embed(source="../../../../../../../pages/images/item/detail_preview_bg.png")]
    protected static const BACKGROUND :Class;
}
}

import com.threerings.msoy.world.client.AvatarSprite;

class ViewerAvatarSprite extends AvatarSprite
{
    public function ViewerAvatarSprite ()
    {
        super(null);
        configureMouseProperties();
    }

    public function setMoving (moving :Boolean) :void
    {
        _moving = moving;
        appearanceChanged();
    }

    override public function isMoving () :Boolean
    {
        return _moving;
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

    protected var _moving :Boolean = false;

    protected var _state :String;
}
