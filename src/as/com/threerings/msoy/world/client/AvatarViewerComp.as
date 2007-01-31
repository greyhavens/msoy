//
// $Id$

package com.threerings.msoy.world.client {

import mx.binding.utils.BindingUtils;

import mx.containers.Canvas;
import mx.controls.Button;
import mx.controls.CheckBox;
import mx.controls.ComboBox;
import mx.controls.HSlider;
import mx.controls.Label;

import mx.events.FlexEvent;

import com.threerings.msoy.ui.Grid;

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

        var rotation :HSlider = new HSlider();
        rotation.minimum = 0;
        rotation.maximum = 360;
        rotation.snapInterval = 1;
        rotation.liveDragging = true;

        var walking :CheckBox = new CheckBox();

        var talking :Button = new Button();
        talking.label = "Talk!";
        talking.addEventListener(FlexEvent.BUTTON_DOWN,
            function (evt :FlexEvent) :void {
                _avatar.performAvatarSpoke();
            });

        var grid :Grid = new Grid();
        grid.addRow("Walking:", walking);
        grid.addRow("Facing angle:", rotation);
        grid.addRow(talking);

        var holder :Canvas = new Canvas();
        holder.rawChildren.addChild(_avatar);
        grid.addRow(holder, [2, 1]);
        addChild(grid);

        BindingUtils.bindSetter(function (val :Number) :void {
            _avatar.setOrientation(int(val));
        }, rotation, "value");

        BindingUtils.bindSetter(function (val :Boolean) :void {
            _avatar.setMoving(val);
        }, walking, "selected");
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

        // add the event listener, because we don't do it ourselves anymore
        // now that the room handles it for normal avatars
        addEventListener(MouseEvent.CLICK, mouseClick);
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
