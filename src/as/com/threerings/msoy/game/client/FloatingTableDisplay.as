package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import mx.collections.ArrayCollection;

import mx.controls.Button;
import mx.controls.Label;

import mx.containers.VBox;
import mx.containers.Grid;
import mx.containers.GridItem;
import mx.containers.GridRow;

import mx.core.ClassFactory;

import com.threerings.msoy.client.Msgs
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.parlor.data.Table;

import com.threerings.util.CommandEvent;

import com.threerings.msoy.ui.MsoyList;

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

        // This is a *hopefully* temporary hack to get the TitleWindow to correctly size and 
        // layout its children (namely, this list and the Back to Lobby button).  TODO: go through
        // the MsoyList/List code and figure out what in the hell is causing it to correctly 
        // resize this title window, when nothing else seems to do it.
        var list :MsoyList = new MsoyList(_ctx);
        list.variableRowHeight = true;
        list.height = 200;
        var factory :ClassFactory = new ClassFactory(TableRenderer);
        factory.properties = { ctx: _ctx, panel: _panel };
        list.itemRenderer = factory;
        list.dataProvider = new ArrayCollection();
        (list.dataProvider as ArrayCollection).addItem(_table);
        addChild(list);

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
