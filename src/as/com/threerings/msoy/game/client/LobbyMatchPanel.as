//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObjectContainer;

import mx.collections.ArrayCollection;
import mx.collections.Sort;
import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.List;
import mx.controls.Text;
import mx.core.ClassFactory;
import mx.core.ScrollPolicy;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;
import com.threerings.util.Integer;

import com.threerings.parlor.client.TableObserver;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.game.client.GameContext;
import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyMatchConfig;

/**
 * Displays the current tables list and related bits.
 */
public class LobbyMatchPanel extends VBox
    implements TableObserver
{
    public function LobbyMatchPanel (gctx :GameContext, ctrl :LobbyController, lobj :LobbyObject)
    {
        _gctx = gctx;
        _ctrl = ctrl;
        _lobj = lobj;

        styleName = "lobbyMatchPanel";
        percentWidth = 100;
        percentHeight = 100;
    }

    // from TableObserver
    public function tableAdded (table :Table) :void
    {
        // if the table is somehow already here, just overwrite it, otherwise add it
        var tidx :int = getTableIndex(table.tableId);
        if (tidx != -1) {
            _tableList.dataProvider.setItemAt(table, tidx);
        } else {
            _tableList.dataProvider.addItem(table);
        }

        // resort and refilter
        _tableList.dataProvider.refresh();

        updateTableState();
    }

    // from TableObserver
    public function tableUpdated (table :Table) :void
    {
        var tidx :int = getTableIndex(table.tableId);
        if (tidx == -1) {
            return;
        }
            
        // update the table, resort and refilter
        _tableList.dataProvider.setItemAt(table, tidx);
        _tableList.dataProvider.refresh();
    }

    // from TableObserver
    public function tableRemoved (tableId :int) :void
    {
        var tidx :int = getTableIndex(tableId);
        if (tidx != -1) {
            _tableList.dataProvider.removeItemAt(tidx);
        }
        updateTableState();
    }

    override public function parentChanged (parent :DisplayObjectContainer) :void
    {
        super.parentChanged(parent);

        if (parent == null) {
            _ctrl.tableDir.removeTableObserver(this);
        } else {
            _ctrl.tableDir.addTableObserver(this);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        // this contains the things that are actually left and right padded
        var padded :VBox = new VBox();
        padded.percentWidth = 100;
        padded.setStyle("verticalGap", 0);
        padded.setStyle("paddingLeft", 10);
        padded.setStyle("paddingRight", 10);
        addChild(padded);

        var hbox :HBox = new HBox();
        hbox.percentWidth = 100;
        hbox.setStyle("verticalAlign", "middle");
        hbox.setStyle("paddingBottom", 10);
        var createTip :Text = new Text();
        createTip.styleName = "createTip";
        createTip.text = Msgs.GAME.get("i.create_multi");
        createTip.percentWidth = 100;
        hbox.addChild(createTip);
        var createBtn :CommandButton = new CommandButton(Msgs.GAME.get("b.create_multi"),
            _ctrl.panel.setMode, LobbyController.MODE_CREATE);
        createBtn.styleName = "blueButton";
        hbox.addChild(createBtn);
        padded.addChild(hbox);

        padded.addChild(FlexUtil.createLabel(Msgs.GAME.get("t.lmp_games"), "lobbyTitle"));

        // determine our informational messages
        var noPendersMsg :String = (GameConfig.SEATED_GAME == _lobj.gameDef.match.getMatchType()) ?
            Msgs.GAME.get("m.no_penders_seated") : Msgs.GAME.get("m.no_penders_party");

        _noTablesLabel = new Text();
        _noTablesLabel.styleName = "tableMessage";
        _noTablesLabel.text = noPendersMsg;
        _noTablesLabel.percentWidth = 100;
        padded.addChild(_noTablesLabel);

        // and the table list has no padding, right up to the borders baby!
        addChild(_tableList = new TableList());
        _tableList.styleName = "tableList";
        _tableList.dataProvider = new ArrayCollection();
        _tableList.horizontalScrollPolicy = ScrollPolicy.OFF;
        _tableList.percentWidth = 100;
        _tableList.percentHeight = 100;
        _tableList.selectable = false;

        var cf :ClassFactory = new ClassFactory(TableSummaryPanel);
        cf.properties =  { gctx: _gctx, lobj: _lobj };
        _tableList.itemRenderer = cf;

        // set up a filter to not show unactionable tables
        _tableList.dataProvider.filterFunction = function (item :Object) :Boolean {
            return _ctrl.isActionableTable(item as Table);
        };

        // sort in play tables below pending tables
        var sort :Sort = new Sort();
        sort.compareFunction = function (a :Object, b :Object, fields: Array = null) :int {
            var ta :Table = (a as Table), tb :Table = (b as Table);
            // TODO: sort tables with friends higher than tables without?
            if (ta.inPlay() && !tb.inPlay()) {
                return 1;
            } else if (tb.inPlay() && !ta.inPlay()) {
                return -1;
            } else {
                return Integer.compare(ta.tableId, tb.tableId);
            }
        };
        _tableList.dataProvider.sort = sort;

        // add our existing tables
        for each (var table :Table in _lobj.tables.toArray()) {
            _tableList.dataProvider.addItem(table);
        }
        _tableList.dataProvider.refresh();

        updateTableState();
    }

    /**
     * Returns the index of the table with the specified id, or -1.
     */
    protected function getTableIndex (tableId :int) :int
    {
        for (var ii :int = 0; ii < _tableList.dataProvider.length; ii++) {
            var table :Table = (_tableList.dataProvider.getItemAt(ii) as Table);
            if (table.tableId == tableId) {
                // redelegate to getItemIndex on the real reference to account for sorting and
                // filtering and hairy business
                return _tableList.dataProvider.getItemIndex(table);
            }
        }
        return -1;
    }

    protected function updateTableState () :void
    {
        var haveTables :Boolean = (_tableList.dataProvider.length > 0);
        FlexUtil.setVisible(_noTablesLabel, !haveTables);
        FlexUtil.setVisible(_tableList, haveTables);
    }

    protected var _gctx :GameContext;
    protected var _ctrl :LobbyController;
    protected var _lobj :LobbyObject;

    protected var _noTablesLabel :Text;
    protected var _tableList :List;
}
}

import flash.display.Sprite;
import mx.controls.List;

class TableList extends List
{
    override protected function drawRowBackground(
        s :Sprite, rowIndex :int, y :Number, height :Number, color :uint, dataIndex :int) :void
    {
        if (rowIndex % 2 == 1) {
            return;
        } else {
            super.drawRowBackground(s, rowIndex, y, height, color, dataIndex);
        }
    }
}
