//
// $Id$

package com.threerings.msoy.ui {

import flash.events.MouseEvent;

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;

import mx.containers.Canvas;

import mx.controls.MenuBar;

import mx.core.Application;

import mx.managers.PopUpManager;

import com.threerings.flex.FlexWrapper;

/**
 * In-progress.
 *
 * A radial Menu as a Drop-in replacement for CommandMenu.
 *
 * LOTS of love will be needed, so I'm going to let this fester until we have time to
 * love on it.
 */
public class RadialMenu extends Canvas
{
    public static const RADIUS :int = 25;

    public function RadialMenu ()
    {
    }

    public function set dataProvider (array :Array) :void
    {
        var inc :Number = Math.PI / 3;
        var radians :Number = -Math.PI/2; // + (inc/2);

        _items = [];

        for (var ii :int = 0; ii < 6; ii++) {
            var item :RadialItem = new RadialItem(this, array);
            _items.push(item);

            var fw :FlexWrapper = new FlexWrapper(item);
            addChild(fw);

            // we just need the global mouse coordinates, even if we're down
            fw.x = Application(Application.application).mouseX + (RADIUS * 2 * Math.cos(radians));
            fw.y = Application(Application.application).mouseY + (RADIUS * 2 * Math.sin(radians));
            radians += inc;
        }
    }

    public function popUp (parent :DisplayObjectContainer) :void
    {
        PopUpManager.addPopUp(this, parent, false);
        systemManager.addEventListener(MouseEvent.MOUSE_DOWN, handlePopDown, false, 0, true);
    }

    public function willRoll (sub :RadialItem) :void
    {
        for each (var item :RadialItem in _items) {
            if (item != sub) {
                item.minimize();
            }
        }

        // if we're showing a sub-squidgy, make it topmost
        if (sub != null) {
            for (var ii :int = 0; ii < numChildren; ii++) {
                var wrapper :FlexWrapper = FlexWrapper(getChildAt(ii));
                if (wrapper.contains(sub)) {
                    setChildIndex(wrapper, numChildren - 1);
                    break;
                }
            }
        }
    }

    protected function handlePopDown (event :MouseEvent) :void
    {
        if (this.contains(event.target as DisplayObject)) {
            return;
        }

        systemManager.removeEventListener(MouseEvent.MOUSE_DOWN, handlePopDown);
        PopUpManager.removePopUp(this);
    }

    protected var _items :Array;
}
}

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.MouseEvent;

import flash.geom.Point;

import com.threerings.flex.CommandMenu;

import com.threerings.msoy.ui.RadialMenu;

class RadialItem extends Sprite
{
    public function RadialItem (menu :RadialMenu, subitems :Array)
    {
        _menu = menu;
        _subitems = subitems;

        addEventListener(MouseEvent.ROLL_OVER, handleRoll);
        addEventListener(MouseEvent.ROLL_OUT, handleRoll);
        updateVis();
    }

    public function minimize () :void
    {
        if (_submenu != null) {
            _submenu.hide();
            _submenu = null;

            updateVis();
        }
    }

    protected function handleRoll (event :MouseEvent) :void
    {
        if (event.type == MouseEvent.ROLL_OUT) {
//            if (_submenu.contains(DisplayObject(event.relatedObject))) {
//                trace("========");
//                _submenu.addEventListener(MouseEvent.ROLL_OUT, handleSubRoll);
//            } else {
//                _menu.willRoll(null);
//            }
            return;
        }

        if (_submenu != null) {
            return;
        }

        _menu.willRoll(this);

        _submenu = CommandMenu.createMenu(_subitems);
        var p :Point = this.localToGlobal(new Point());
        _submenu.popUpAt(p.x, p.y + RadialMenu.RADIUS);

        updateVis();
    }

    protected function handleSubRoll (event :MouseEvent) :void
    {
        _menu.willRoll(null);
    }

    protected function updateVis () :void
    {
        var g :Graphics = this.graphics;
        g.clear();
        g.lineStyle(1, 0);
        g.beginFill(0x0077cc);

        if (_submenu != null) {
            g.drawRoundRect(-RadialMenu.RADIUS, -RadialMenu.RADIUS, RadialMenu.RADIUS * 5,
                RadialMenu.RADIUS * 2, RadialMenu.RADIUS * 2, RadialMenu.RADIUS * 2);
        } else {
            g.drawCircle(0, 0, RadialMenu.RADIUS);
        }

        g.endFill();
    }

    protected var _menu :RadialMenu;

    protected var _subitems :Array;

    protected var _submenu :CommandMenu;
}
