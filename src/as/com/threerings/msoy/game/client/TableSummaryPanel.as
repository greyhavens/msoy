//
// $Id$

package com.threerings.msoy.game.client {

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;

import com.threerings.util.Name;
import com.threerings.util.StringUtil;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.parlor.data.Parameter;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.data.GameConfig;

import com.whirled.game.data.WhirledGameConfig;
import com.whirled.game.data.GameDefinition;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.VizMemberName;
import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.SimpleGrid;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.MsoyTableConfig;

/**
 * Displays a summary of a table for use in a TableList.
 */
public class TableSummaryPanel extends HBox
{
    public var tableId :int;

    public var gctx :GameContext;
    public var lobj :LobbyObject;

    // from Container
    override public function set data (value :Object) :void
    {
        super.data = value;

        // better safe than sorry
        if (value == null) {
            return;
        }

        var table :Table = (value as Table);
        this.tableId = table.tableId;

// TODO: highlight tables iwth friends
//             var count :int = _ctrl.countFriends(table, gctx.getPlayerObject());

        // set up the icon of the first player
        var player :VizMemberName = (table.players[0] as VizMemberName);
        if (player != null) {
            _icon.setMediaDesc(player.getPhoto());
        }

        // set up the table title
        const tableConfig :MsoyTableConfig = table.tconfig as MsoyTableConfig;
        _title.text = (tableConfig != null) ? tableConfig.title : "";

        // set up the game info
        var info :String;
        if (table.gameOid != -1) {
            var pcount :int = table.getPlayers().length;
            _info.text = Msgs.GAME.get("m.tsp_in_progress",
                (pcount == 0) ? table.watchers.length : pcount);
        } else if (table.players != null) {
            var open :int = table.players.filter(function (pname :Name, ... rest) :Boolean {
                return (pname == null);
            }).length;
            _info.text = Msgs.GAME.get("m.tsp_players", table.players.length, open);
        } else {
            _info.text = Msgs.GAME.get("m.tsp_in_progress"); // can't happen?
        }
        _info.toolTip = createInfoTip(table);

        // if the game is in progress
        if (table.gameOid != -1) {
            var key :String = null;
            switch (table.config.getMatchType()) {
            case GameConfig.PARTY:
                if (!table.tconfig.privateTable) {
                    key = "b.tsp_play";
                }
                break;

            default:
                if (!(lobj.gameDef.match as MsoyMatchConfig).unwatchable &&
                    !table.tconfig.privateTable) {
                    key = "b.tsp_watch";
                }
                break;
            }

            if (key != null) {
                _action.label = Msgs.GAME.get(key);
                _action.setCommand(MsoyController.GO_GAME, [ lobj.game.gameId, table.gameOid ]);
            }
            _action.visible = (key != null);
            _action.enabled = (key != null);

        } else {
            _action.label = Msgs.GAME.get("b.tsp_join");
            _action.setCommand(LobbyController.JOIN_TABLE, [ table.tableId, Table.ANY_POSITION ]);
        }
    }

    // from HBox
    override protected function createChildren () :void
    {
        super.createChildren();

        // we can't set a style name because we're a list renderer
        percentWidth = 100;
        setStyle("verticalAlign", "middle");
        setStyle("paddingLeft", 10);
        setStyle("paddingRight", 10);

        addChild(_icon = MediaWrapper.createView(null, MediaDesc.HALF_THUMBNAIL_SIZE));
        var bits :VBox = new VBox();
        bits.percentWidth = 100;
        bits.setStyle("verticalGap", 0);
        bits.addChild(_title = FlexUtil.createLabel("", "tableSummaryTitle"));
        bits.addChild(_info = FlexUtil.createLabel("", "tableSummaryStatus"));
        addChild(bits);
        addChild(_action = new CommandButton());
    }

    protected static function createInfoTip (table :Table) :String
    {
        // display whether this game is rated
        var info :String = Msgs.GAME.get(table.config.rated ? "l.is_rated" : "l.not_rated");

        // display the non-players in the room (or everyone for party games)
        if (table.watchers != null && table.watchers.length > 0) {
            info = info + ", " + Msgs.GAME.get("l.people") + ": " + table.watchers.join(", ");
        }

        // and display any custom table configuration
        if (table.config is WhirledGameConfig) {
            var gconfig :WhirledGameConfig = (table.config as WhirledGameConfig);
            var params :Array = gconfig.getGameDefinition().params;
            if (params != null) {
                for each (var param :Parameter in params) {
                    var name :String = StringUtil.isBlank(param.name) ? param.ident : param.name;
                    var value :String = String(gconfig.params.get(param.ident));
                    info = info + ", " + name + ": "+ value;
                }
            }
        }

        return info;
    }

    protected var _icon :MediaWrapper;
    protected var _title :Label;
    protected var _info :Label;
    protected var _action :CommandButton;
}
}
