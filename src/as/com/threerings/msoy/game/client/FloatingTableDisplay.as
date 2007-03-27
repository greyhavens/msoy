package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import mx.controls.Button;

import com.threerings.msoy.client.Msgs
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.parlor.data.Table;

import com.threerings.util.CommandEvent;

public class FloatingTableDisplay extends FloatingPanel
{
    public function FloatingTableDisplay (ctx :WorldContext, panel :LobbyPanel, table :Table)
    {
        super(ctx, Msgs.GAME.get("t.table_display"));
        _panel = panel;
        _table = table;
    }

    public function getRenderer () :TableRenderer
    {
        return _tableRender;
    }

    public function shutdown () :void
    {
        _hasBeenShutDown = true;
        close();
    }

    override public function open (modal :Boolean = false, parent :DisplayObject = null,
        avoid :DisplayObject = null) :void
    {
        if (!_hasBeenShutDown) {
            super.open(modal, parent, avoid);
            x = 10;
            y = 10;
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _tableRender = new TableRenderer(true);
        _tableRender.ctx = _ctx;
        _tableRender.panel = _panel;
        addChild(_tableRender);
        _tableRender.data = _table;
    }

    /** controlled panel to dispatch LobbyController events on */
    protected var _panel :LobbyPanel;

    /** The table we're displaying */
    protected var _table :Table;

    protected var _tableRender :TableRenderer;

    protected var _hasBeenShutDown :Boolean = false;
}
}
