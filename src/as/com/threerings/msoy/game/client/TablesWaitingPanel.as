//
// $Id$

package com.threerings.msoy.game.client {

import mx.collections.ArrayCollection;
import mx.controls.List;
import mx.core.ClassFactory;
import mx.core.ScrollPolicy;

import com.threerings.util.CommandEvent;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;

import com.threerings.msoy.world.client.WorldContext;

/**
 * Displays a snapshot of games that are presently awaiting players.
 */
public class TablesWaitingPanel extends FloatingPanel
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

        var factory :ClassFactory = new ClassFactory(TablesWaitingRenderer);
        var thisPanel :Object = this;
        factory.properties = { panel: thisPanel };
        _list.itemRenderer = factory;

        _list.dataProvider = new ArrayCollection([ true ]); // indicates that we're loading
        addChild(_list);

        setButtonWidth(0);
        _refresh = new CommandButton(Msgs.GAME.get("b.refresh"), makeRequest);
        addButtons(_refresh,
            new CommandButton(Msgs.GAME.get("b.allGames"), routeCmd, MsoyController.VIEW_GAMES),
            CANCEL_BUTTON);
    }

    /**
     * Routes a command onward, closing this dialog afterwards.
     */
    public function routeCmd (cmd :String, arg :Object = null) :void
    {
        CommandEvent.dispatch(this, cmd, arg);
        close();
    }
    
    override protected function didOpen () :void
    {
        super.didOpen();
        makeRequest();
    }

    protected function makeRequest () :void
    {
        _refresh.enabled = false;
        WorldGameService(_ctx.getClient().requireService(WorldGameService)).getTablesWaiting(
            _ctx.getClient(), _ctx.resultListener(gotTables));
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
    public var panel :TablesWaitingPanel;

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
            _info.setCallback(panel.routeCmd, [ WorldController.VIEW_GAME, table.gameId ]);
            _play.visible = true;
            _play.setCallback(panel.routeCmd, [ WorldController.PLAY_GAME, table.gameId ]);
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
