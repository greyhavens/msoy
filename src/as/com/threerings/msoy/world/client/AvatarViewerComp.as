//
// $Id$

package com.threerings.msoy.world.client {

import flash.events.MouseEvent;

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

import com.threerings.flex.GridUtil;

public class AvatarViewerComp extends Canvas
{
    public function AvatarViewerComp ()
    {
    }

    /**
     * Our ParameterUtil callback.
     */
    protected function gotParams (params :Object) :void
    {
        _avatar.setMedia(String(params["avatar"]));
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _avatar = new ViewerAvatarSprite();
        // load params after creating the sprite
        ParameterUtil.getParameters(this, gotParams);

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
        talking.addEventListener(FlexEvent.BUTTON_DOWN,
            function (evt :FlexEvent) :void {
                _avatar.performAvatarSpoke();
            });

        var grid :Grid = new Grid();
        GridUtil.addRow(grid, "Walking:", walking);
        GridUtil.addRow(grid, "Facing angle:", rotation);
        GridUtil.addRow(grid, talking);

        var holder :Canvas = new Canvas();
        holder.width = _avatar.getMaxContentWidth();
        holder.height = _avatar.getMaxContentWidth();

        holder.rawChildren.addChild(_avatar);
        GridUtil.addRow(grid, holder, [2, 1]);
        addChild(grid);

        BindingUtils.bindSetter(function (val :Number) :void {
            _avatar.setOrientation(int(val + 360) % 360);
        }, rotation, "value");

        BindingUtils.bindSetter(_avatar.setMoving, walking, "selected");

        // listen for mouse clicks on this canvas,
        // call into the avatar topop up the action menu
        addEventListener(MouseEvent.CLICK, _avatar.mouseClick);
    }

    protected var _avatar :ViewerAvatarSprite;
}
}

import flash.display.Bitmap;
import flash.display.Sprite;
import flash.events.MouseEvent;
import flash.geom.Matrix;
import flash.geom.Point;

import com.threerings.flex.CommandMenu;
import com.threerings.msoy.world.client.AvatarSprite;

class ViewerAvatarSprite extends AvatarSprite
{
    public function ViewerAvatarSprite ()
    {
        super(null);
        configureMouseProperties();

        // draw a gradient background
        var bmp :Bitmap = Bitmap(new BACKGROUND());
        var sprite :Sprite = new Sprite();
        sprite.graphics.beginBitmapFill(bmp.bitmapData);
        sprite.graphics.drawRect(0, 0, getMaxContentWidth(), getMaxContentHeight());
        sprite.graphics.endFill();
        sprite.mouseEnabled = false;
        addChildAt(sprite, 0);
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

    override public function mouseClick (event :MouseEvent) :void
    {
        var actions :Array = getAvatarActions();
        var actionItems :Array = [];
        for each (var act :String in actions) {
            actionItems.push({ label: act, callback: performAvatarAction,
                arg: act });
        }

        var states :Array = getAvatarStates();
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
    protected function performAvatarAction (action :String) :void
    {
        messageReceived(action, null, true);
    }

    protected var _moving :Boolean = false;

    protected var _state :String;

    [Embed(source="../../../../../../../pages/images/item/detail_preview_bg.png")]
    protected static const BACKGROUND :Class;
}
