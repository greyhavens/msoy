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
import com.threerings.msoy.data.VizMemberName;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.ui.MediaWrapper;
import com.threerings.msoy.ui.SimpleGrid;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.MsoyTableConfig;

/**
 * Displays a summary of a table for use in the match panel.
 */
public class TableSummaryPanel extends HBox
{
    public var tableId :int;

    public function TableSummaryPanel (gctx :GameContext, lobj :LobbyObject)
    {
        _gctx = gctx;
        _lobj = lobj;

        styleName = "tableSummary";
        percentWidth = 100;

        addChild(_icon = MediaWrapper.createView(null, MediaDesc.HALF_THUMBNAIL_SIZE));
        var bits :VBox = new VBox();
        bits.percentWidth = 100;
        bits.setStyle("verticalGap", 0);
        bits.addChild(_title = FlexUtil.createLabel("", "tableTitle"));
        bits.addChild(_info = FlexUtil.createLabel("", "tableStatus"));
        addChild(bits);
        addChild(_action = new CommandButton());
    }

    public function update (table :Table) :void
    {
        this.tableId = table.tableId;

// TODO: highlight tables iwth friends
//             var count :int = _ctrl.countFriends(table, _gctx.getPlayerObject());

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
            _info.text = "In progress...";
        } else if (table.players != null) {
            var open :int = table.players.filter(function (pname :Name, ii :int, a :Array) :Boolean {
                return (pname == null);
            }).length;
            _info.text = table.players.length + " players, " + open + " openings";
        } else {
            _info.text = "Partay...";
        }
        _info.toolTip = createInfoTip(table);

        // if the game is in progress
        if (table.gameOid != -1) {
            var key :String = null;
            switch (table.config.getMatchType()) {
            case GameConfig.PARTY:
                if (!table.tconfig.privateTable) {
                    key = "b.join";
                }
                break;

            default:
                if (!(_lobj.gameDef.match as MsoyMatchConfig).unwatchable &&
                    !table.tconfig.privateTable) {
                    key = "b.watch";
                }
                break;
            }

            if (key != null) {
                _action.label = Msgs.GAME.get(key);
                _action.setCommand(MsoyController.GO_GAME, [ _lobj.game.gameId, table.gameOid ]);
            }
            _action.visible = (key != null);
            _action.enabled = (key != null);

        } else /* TODO: if party game, then just play! */ {
            _action.label = Msgs.GAME.get("b.join");
            _action.setCommand(LobbyController.JOIN_TABLE, [ table.tableId, Table.ANY_POSITION ]);
        }
    }

    protected function createInfoTip (table :Table) :String
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

    protected var _gctx :GameContext;
    protected var _lobj :LobbyObject;

    protected var _icon :MediaWrapper;
    protected var _title :Label;
    protected var _info :Label;
    protected var _action :CommandButton;
}
}
