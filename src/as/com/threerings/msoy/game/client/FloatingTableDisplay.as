package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import mx.controls.Button;
import mx.controls.Label;

import mx.containers.Grid;
import mx.containers.GridItem;
import mx.containers.GridRow;

import com.threerings.msoy.client.Msgs
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.parlor.data.Table;

import com.threerings.util.CommandEvent;

public class FloatingTableDisplay extends FloatingPanel
{
    public static const BACK_TO_LOBBY_BUTTON :int = 100;

    public function FloatingTableDisplay (ctx :WorldContext, panel :LobbyPanel, table :Table)
    {
        super(ctx, Msgs.GAME.get("t.table_display"));
        _panel = panel;
        _table = table;
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

        //var tableRender :TableRenderer = new TableRenderer();
        //tableRender.ctx = _ctx;
        //tableRender.panel = _panel;
        //addChild(tableRender);

        // this is a shameful hack.  I can't get this damn thing to show a VBox correctly -
        // so I'm stuffing the VBox into a Grid for now
        var grid :Grid = new Grid();
        var row :GridRow = new GridRow();
        var item :GridItem = new GridItem();
        //item.addChild(tableRender);
        var label :Label = new Label();
        label.text = "Hello Whirled";
        item.addChild(label);
        row.addChild(item);
        grid.addChild(row);
        addChild(grid);

        //tableRender.data = _table;

        addButtons(BACK_TO_LOBBY_BUTTON);
    }

    override protected function createButton (buttonId :int) :Button
    {
        var btn :Button;
        switch (buttonId) {
        case BACK_TO_LOBBY_BUTTON:
            btn = new Button();
            btn.label = Msgs.GAME.get("b.back_to_lobby");
            break;

        default:
            btn = super.createButton(buttonId);
        }
        return btn;
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        switch(buttonId) {
        case BACK_TO_LOBBY_BUTTON:
            CommandEvent.dispatch(_panel, LobbyController.JOIN_LOBBY);
            close();
            break;

        default:
            super.buttonClicked(buttonId);
        }
    }

    /** controlled panel to dispatch LobbyController events on */
    protected var _panel :LobbyPanel;

    /** The table we're displaying */
    protected var _table :Table;

    protected var _hasBeenShutDown :Boolean = false;
}
}
