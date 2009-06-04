//
// $Id$

package com.threerings.msoy.game.client {

import mx.collections.ArrayCollection;
import mx.controls.List;
import mx.core.ClassFactory;
import mx.core.ScrollPolicy;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.FlyingPanel;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.world.client.WorldContext;

/**
 * Displays a snapshot of games that are presently awaiting players.
 */
public class TablesWaitingPanel extends FlyingPanel
{
    public function TablesWaitingPanel (ctx :WorldContext)
    {
        super(ctx, Msgs.GAME.get("t.tables_waiting"));
        showCloseButton = true;

        addChild(FlexUtil.createLabel(Msgs.GAME.get("m.tables_waiting")));

        _list = new List();
        _list.horizontalScrollPolicy = ScrollPolicy.OFF;
        _list.verticalScrollPolicy = ScrollPolicy.ON;
        _list.selectable = false;

        _list.itemRenderer = new ClassFactory(TablesWaitingRenderer);

        _list.dataProvider = new ArrayCollection([ true ]); // indicates that we're loading
        addChild(_list);

        setButtonWidth(0);
        _refresh = new CommandButton(Msgs.GAME.get("b.refresh"), refresh);
        addButtons(_refresh,
            new CommandButton(Msgs.GAME.get("b.allGames"), MsoyController.VIEW_GAMES),
            // and force the cancel button rightmost by pre-creating it, rather than defining
            // constants for these other two buttons and tweaking them up.
            createButton(CANCEL_BUTTON));
    }

    public function refresh () :void
    {
        _refresh.enabled = false;
        WorldGameService(_ctx.getClient().requireService(WorldGameService)).getTablesWaiting(
            _ctx.getClient(), _ctx.resultListener(gotTables));
    }

    override protected function didOpen () :void
    {
        super.didOpen();
        refresh();
    }

    protected function gotTables (tables :Array /* of TablesWaiting */) :void
    {
        _refresh.enabled = true;
        if (tables.length == 0) {
            tables.push(false); // indicates that there are no tables
        }
        _list.dataProvider = new ArrayCollection(tables);
    }

    protected var _list :List;
    protected var _refresh :CommandButton;
}
}

import mx.containers.HBox;
import mx.controls.Label;
import mx.core.ScrollPolicy;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;

import com.threerings.msoy.game.client.TablesWaitingPanel;
import com.threerings.msoy.game.data.TablesWaiting;

import com.threerings.msoy.world.client.WorldController;

class TablesWaitingRenderer extends HBox
{
    public function TablesWaitingRenderer ()
    {
        horizontalScrollPolicy = ScrollPolicy.OFF;
        verticalScrollPolicy = ScrollPolicy.OFF;

        _name = new Label();
        _name.width = 150;

        _info = new CommandButton(Msgs.GAME.get("b.info"));
        _play = new CommandButton(Msgs.GAME.get("b.play_now"));
        _play.styleName = "orangeButton";
    }

    override public function set data (value :Object) :void
    {
        super.data = value;

        var table :TablesWaiting = value as TablesWaiting;
        if (table == null) {
            // indicates no waiting games
            _name.text = Msgs.GAME.get(Boolean(value) ? "m.loading" : "m.none");
            _info.visible = false; // but keep reserving the room
            _play.visible = false; // but keep reserving the room

        } else {
            _name.text = table.name;
            _info.visible = true;
            _info.setCommand(WorldController.VIEW_GAME, table.gameId);
            _play.visible = true;
            _play.setCommand(WorldController.SHOW_LOBBY, table.gameId);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        setStyle("paddingTop", 0);
        setStyle("paddingBottom", 0);
        setStyle("paddingLeft", 3);
        setStyle("paddingRight", 3);
        setStyle("verticalAlign", "middle");

        addChild(_name);
        addChild(_info);
        addChild(_play);
    }

    protected var _name :Label;
    protected var _info :CommandButton;
    protected var _play :CommandButton;
}
