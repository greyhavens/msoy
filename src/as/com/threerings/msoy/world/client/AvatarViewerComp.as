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

import com.threerings.flex.GridUtil;

public class AvatarViewerComp extends Canvas
{
    public function AvatarViewerComp ()
    {
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var params :Object = this.root.loaderInfo.parameters;
        _avatar = new ViewerAvatarSprite(String(params["avatar"]));

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

import flash.events.MouseEvent;
import flash.geom.Point;

import com.threerings.flex.CommandMenu;
import com.threerings.msoy.world.client.AvatarSprite;

class ViewerAvatarSprite extends AvatarSprite
{
    public function ViewerAvatarSprite (url :String)
    {
        super(null);
        setMedia(url);
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

    override public function mouseClick (event :MouseEvent) :void
    {
        var actions :Array = getAvatarActions();
        var menuItems :Array = [];

        for each (var act :String in actions) {
            menuItems.push({ label: act, callback: performAvatarAction,
                arg: act });
        }

        CommandMenu.createMenu(menuItems).show(event.stageX, event.stageY);
    }

    protected var _moving :Boolean = false;
}
