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
    public function RadialMenu (size :Number)
    {
        _size = size;
    }

    public function set dataProvider (array :Array) :void
    {
        var radians :Number = -Math.PI/2;
        var inc :Number = (Math.PI * 2) / array.length;

        // we create a top-level menubar for each element in the array
        for each (var value :Object in array) {
            var bar :MenuBar = new MenuBar();
            bar.styleName = "radialMenu";
            bar.dataProvider = value;

            addChild(bar);
            // we just need the global mouse coordinates, even if we're down
            bar.x = Application(Application.application).mouseX + (_size * Math.cos(radians));
            bar.y = Application(Application.application).mouseY + (_size * Math.sin(radians));
            radians += inc;
        }
    }

    public function popUp (parent :DisplayObjectContainer) :void
    {
        PopUpManager.addPopUp(this, parent, false);
        systemManager.addEventListener(MouseEvent.MOUSE_DOWN, handlePopDown, false, 0, true);
    }

    protected function handlePopDown (event :MouseEvent) :void
    {
        if (this.contains(event.target as DisplayObject)) {
            return;
        }

        systemManager.removeEventListener(MouseEvent.MOUSE_DOWN, handlePopDown);
        PopUpManager.removePopUp(this);
    }

    protected var _size :Number;
}
}
