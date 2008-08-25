//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObjectContainer;

import flash.events.Event;

import flash.geom.Point;

import mx.core.Application;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import mx.containers.Canvas;
import mx.containers.Tile;

import com.threerings.util.CommandEvent;

import com.threerings.flex.CommandCheckBox;

public class ButtonPalette extends Canvas
{
    public function ButtonPalette (ctx :MsoyContext)
    {
        percentWidth = 100;
        height = ControlBar.HEIGHT;

        _toggle = new CommandCheckBox(null, showAll);
        _toggle.styleName = "panelToggle";
        _toggle.y = 7;
        addChild(_toggle);

        minWidth = TOGGLE_WIDTH + DIM;

        _tile = new Tile();
        _tile.tileWidth = 22; //DIM;
        _tile.tileHeight = 23; //DIM;
        _tile.styleName = "buttonPalette";
        _tile.owner = DisplayObjectContainer(Application.application.systemManager);
        _tile.owner.addChild(_tile);

        CommandEvent.configureBridge(_tile, ctx.getTopPanel());

        addEventListener(Event.RENDER, handleRender);
        showAll(false);
    }

    public function addButton (comp :UIComponent, priority :int) :void
    {
        _tile.addChild(comp);
    }

    public function clearButtons () :void
    {
        _tile.removeAllChildren();
    }

    public function recheckButtons () :void
    {
        _tile.width = DIM * Math.max(1, Math.floor((this.width - TOGGLE_WIDTH) / DIM));
        _tile.validateNow();
        const singleRow :Boolean = (_tile.height == _tile.tileHeight);
        _toggle.visible = !singleRow;
        if (singleRow) {
            _toggle.selected = false;
            _up = false;
        }
    }

    protected function updateTileLoc () :void
    {
        var y :int = 3;
        if (_up) {
            y -= (_tile.height - _tile.tileHeight);
        }

        var p :Point = localToGlobal(new Point(TOGGLE_WIDTH, y));
        p = _tile.owner.globalToLocal(p);
        _tile.move(p.x, p.y);
    }

    /**
     * Should we show all the buttons?
     */
    protected function showAll (show :Boolean) :void
    {
        _up = show;
        _tile.setStyle("backgroundAlpha",
            _tile.getStyle(_up ? "backgroundAlphaOpen" : "backgroundAlphaClosed"));
        updateTileLoc();
    }

    override protected function updateDisplayList (uw :Number, uh :Number) :void
    {
        super.updateDisplayList(uw, uh);
        recheckButtons();
    }

    protected function handleRender (event :Event) :void
    {
        updateTileLoc();
    }

    protected var _toggle :CommandCheckBox;

    protected var _tile :Tile;

    protected var _up :Boolean;

    protected static const TOGGLE_WIDTH :int = 20;

    protected static const DIM :int = ControlBar.HEIGHT;
}
}
