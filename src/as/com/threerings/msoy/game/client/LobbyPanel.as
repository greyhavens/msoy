//
// $Id$

package com.threerings.msoy.game.client {

import mx.collections.ArrayCollection;

import mx.containers.VBox;
import mx.controls.ButtonBar;

import mx.core.ClassFactory;

import com.threerings.util.ArrayUtil;

import com.threerings.crowd.client.PlaceView;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.mx.controls.CommandButton;

import com.threerings.parlor.client.SeatednessObserver;
import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.client.TableObserver;

import com.threerings.parlor.data.Table;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.ui.List;

import com.threerings.msoy.game.data.LobbyObject;

/**
 * A panel that displays pending table games.
 */
public class LobbyPanel extends VBox
    implements PlaceView, TableObserver, SeatednessObserver
{
    /** Our log. */
    private const log :Log = Log.getLog(LobbyPanel);

    /** The create-a-table button. */
    public var createBtn :CommandButton;

    /**
     * Create a new LobbyPanel.
     */
    public function LobbyPanel (ctx :MsoyContext)
    {
        _ctx = ctx;
        TableRenderer.ctx = ctx;
    }

    // from PlaceView
    public function willEnterPlace (plobj :PlaceObject) :void
    {
        // add all preexisting tables
        var lobbyObj :LobbyObject = (plobj as LobbyObject);
        for each (var table :Table in lobbyObj.tables.toArray()) {
            tableAdded(table);
        }
    }

    // from PlaceView
    public function didLeavePlace (plobj :PlaceObject) :void
    {
        // clear all the tables
        _tables.removeAll();
    }

    // from TableObserver
    public function tableAdded (table :Table) :void
    {
        _tables.addItem(table);
    }

    // from TableObserver
    public function tableUpdated (table :Table) :void
    {
        var idx :int = ArrayUtil.indexOf(_tables.source, table);
        if (idx >= 0) {
            _tables.setItemAt(table, idx);

        } else {
            log.warning("Never found table to update: " + table);
        }
    }

    // from TableObserver
    public function tableRemoved (tableId :int) :void
    {
        for (var ii :int = 0; ii < _tables.length; ii++) {
            var table :Table = (_tables.getItemAt(ii) as Table);
            if (table.tableId == tableId) {
                _tables.removeItemAt(ii);
                return;
            }
        }

        log.warning("Never found table to remove: " + tableId);
    }

    // from SeatednessObserver
    public function seatednessDidChange (isSeated :Boolean) :void
    {
        createBtn.enabled = !isSeated;
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var list :List = new List(_ctx);
        addChild(list);
        list.itemRenderer = new ClassFactory(TableRenderer);
        list.dataProvider = _tables;

        createBtn = new CommandButton(LobbyController.CREATE_TABLE);
        createBtn.label = _ctx.xlate("game", "b.create");

        var butbar :ButtonBar = new ButtonBar();
        butbar.addChild(createBtn);
        addChild(butbar);
    }

    /** Buy one get one free. */
    protected var _ctx :MsoyContext;

    /** The currently displayed tables. */
    protected var _tables :ArrayCollection = new ArrayCollection();
}
}

import mx.containers.HBox;
import mx.containers.VBox;

import mx.controls.Label;

import mx.core.ScrollPolicy;
import mx.core.UIComponent;

import com.threerings.util.Name;

import com.threerings.mx.controls.CommandButton;

import com.threerings.parlor.data.Table;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.game.client.LobbyController;

class TableRenderer extends HBox
{
    /** Total hackery, to pass a few things in... */
    // TODO?
    public static var ctx :MsoyContext;

    public function TableRenderer ()
    {
        super();
        verticalScrollPolicy = ScrollPolicy.OFF;
        horizontalScrollPolicy = ScrollPolicy.OFF;
    }

    override public function validateDisplayList () :void
    {
        super.validateDisplayList();
        recheckTable();
    }

    protected function recheckTable () :void
    {
        var table :Table = (data as Table);
        var childCount :int = numChildren;
        var seats :int = (table == null) ? 0 : table.occupants.length;
        var nn :int = Math.max(childCount, seats);
        var ourName :Name = ctx.getClientObject().getVisibleName();
        for (var ii :int = 0; ii < nn; ii++) {
            if (ii >= seats) {
                removeChildAt(ii);
                continue;
            }

            var comp :UIComponent = (ii < childCount)
                ? UIComponent(getChildAt(ii))
                : null;
            var occupant :Name = (table.occupants[ii] as Name);
            if (occupant == null || occupant.equals(ourName)) {
                var btn :CommandButton;
                if (comp is CommandButton) {
                    btn = (comp as CommandButton);
                } else {
                    if (comp != null) {
                        removeChildAt(ii);
                    }
                    btn = new CommandButton();
                    addChildAt(btn, ii);
                }
                if (occupant == null) {
                    btn.setCommand(LobbyController.SIT, [ table.tableId , ii ]);
                    btn.label = ctx.xlate("game", "b.sit");

                } else {
                    btn.setCommand(LobbyController.LEAVE, table.tableId);
                    btn.label = ctx.xlate("game", "b.leave");
                }

            } else {
                var lbl :Label;
                if (comp is Label) {
                    lbl = (comp as Label);
                } else {
                    if (comp != null) {
                        removeChildAt(ii);
                    }
                    lbl = new Label();
                    addChildAt(lbl, ii);
                }
                lbl.text = occupant.toString();
            }
        }
    }
}
