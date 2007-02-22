//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObject;

import flash.events.MouseEvent;
import flash.events.TextEvent;

import mx.collections.ArrayCollection;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.ButtonBar;
import mx.controls.Label;
import mx.controls.Alert;

import mx.core.Container;
import mx.core.ClassFactory;

import com.threerings.util.ArrayUtil;
import com.threerings.util.MediaContainer;

import com.threerings.crowd.client.PlaceView;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.flex.CommandButton;

import com.threerings.parlor.client.SeatednessObserver;
import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.client.TableObserver;

import com.threerings.parlor.data.Table;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.client.ChatContainer;

import com.threerings.msoy.ui.MsoyList;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.game.data.LobbyObject;

/**
 * A panel that displays pending table games.
 */
public class LobbyPanel extends VBox
    implements PlaceView, TableObserver, SeatednessObserver
{
    /** Our log. */
    private const log :Log = Log.getLog(LobbyPanel);

    /** The lobby controller. */
    public var controller :LobbyController;

    /** The create-a-table button. */
    public var createBtn :CommandButton;

    /**
     * Create a new LobbyPanel.
     */
    public function LobbyPanel (ctx :WorldContext, ctrl :LobbyController)
    {
        _ctx = ctx;
        controller = ctrl;
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
        _isSeated = isSeated;
        createBtn.enabled = !isSeated;

        // wacky: I think I'd need to refresh the list or something
        // but apparently everything gets re-rendered when any element
        // changes, so I don't. If this turns out to be not true, then
        // here we'll want to do _tables.refresh() or, if we save
        // the list we can do list.updateList().
    }

    /**
     * Are we seated at a table?
     */
    public function isSeated () :Boolean
    {
        return _isSeated;
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        styleName = "lobbyPanel";

        var gameBox :HBox = new HBox();
        gameBox.percentWidth = 100;
        gameBox.percentHeight = 100;
        gameBox.styleName = "gameBox";
        addChild(gameBox);

        // the hard-coded URL bits here are temporary, and will be changed to the default supplied
        // by the Game object, when these fields have been added
        var descriptionBox :VBox = new VBox();
        descriptionBox.width = 160;
        descriptionBox.percentHeight = 100;
        descriptionBox.styleName = "descriptionBox";
        gameBox.addChild(descriptionBox);
        descriptionBox.addChild(new MediaWrapper(new MediaContainer(
            "/media/static/game/logo.png")));
        descriptionBox.addChild(new MediaWrapper(new MediaContainer(
            "/media/static/game/info_top.png")));
        var infoBox :HBox = new HBox();
        infoBox.width = 160;
        infoBox.percentHeight = 100;
        infoBox.setStyle("backgroundImage", "/media/static/game/info_tile.png");
        infoBox.setStyle("backgroundSize", "100%");
        descriptionBox.addChild(infoBox);
        var infoLabel :Label = new Label();
        infoLabel.percentWidth = 100;
        infoLabel.percentHeight = 100;
        infoBox.addChild(infoLabel);
        descriptionBox.addChild(new MediaWrapper(new MediaContainer(
            "/media/static/game/info_bottom.png")));

        var tablesBox :VBox = new VBox();
        tablesBox.styleName = "tablesBox";
        tablesBox.percentWidth = 100;
        tablesBox.percentHeight = 100;
        gameBox.addChild(tablesBox);

        var tabsBox :HBox = new HBox();
        tabsBox.styleName = "tabsBox";
        tabsBox.percentWidth = 100;
        tabsBox.height = 27;
        tablesBox.addChild(tabsBox);
        tabsBox.setStyle("backgroundImage", "/media/static/game/box_tile.png");
        tabsBox.setStyle("backgroundSize", "100%");
        tabsBox.setStyle("verticalAlign", "middle");
        var padding :HBox = new HBox();
        padding.percentWidth = 100;
        padding.percentHeight = 100;
        tabsBox.addChild(padding);
        var about :Label = new Label();
        about.text = Msgs.GAME.get("b.about");
        about.styleName = "lobbyLink";
        // TODO use the correct URL here and on the buy button
        about.addEventListener(MouseEvent.CLICK, function () :void {
            _ctx.getMsoyController().showExternalURL("http://google.com");
        });
        tabsBox.addChild(about);
        var buy :Label = new Label();
        buy.text = Msgs.GAME.get("b.buy");
        buy.styleName = "lobbyLink";
        buy.addEventListener(MouseEvent.CLICK, function () :void {
            _ctx.getMsoyController().showExternalURL("http://google.com");
        });
        tabsBox.addChild(buy);
        var inviteBtn :CommandButton = new CommandButton();
        inviteBtn.height = 22;
        inviteBtn.label = Msgs.GAME.get("b.invite_to_game");
        createBtn = new CommandButton(LobbyController.CREATE_TABLE);
        createBtn.height = 22;
        createBtn.label = Msgs.GAME.get("b.create");
        var butbar :ButtonBar = new ButtonBar();
        butbar.addChild(inviteBtn);
        butbar.addChild(createBtn);
        tabsBox.addChild(butbar);

        var list :MsoyList = new MsoyList(_ctx);
        list.variableRowHeight = true;
        list.percentHeight = 100;
        list.percentWidth = 100;
        tablesBox.addChild(list);

        var factory :ClassFactory = new ClassFactory(TableRenderer);
        factory.properties = { ctx: _ctx, panel: this };
        list.itemRenderer = factory;
        list.dataProvider = _tables;

        // and a chat box
        var chatbox :ChatContainer = new ChatContainer(_ctx);
        chatbox.percentWidth = 100;
        chatbox.height = 100;
        addChild(chatbox);
    }

    /** Buy one get one free. */
    protected var _ctx :WorldContext;

    /** Are we seated? */
    protected var _isSeated :Boolean;

    /** The currently displayed tables. */
    protected var _tables :ArrayCollection = new ArrayCollection();
}
}
