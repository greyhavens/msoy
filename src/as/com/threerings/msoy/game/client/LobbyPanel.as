//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;

import flash.events.MouseEvent;
import flash.events.TextEvent;

import mx.collections.ArrayCollection;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.ViewStack;
import mx.controls.ButtonBar;
import mx.controls.Label;
import mx.controls.Alert;
import mx.controls.Text;
import mx.controls.TabBar;

import mx.core.Container;
import mx.core.ClassFactory;

import com.threerings.util.ArrayUtil;
import com.threerings.util.CommandEvent;

import com.threerings.flash.MediaContainer;

import com.threerings.crowd.client.PlaceView;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.flex.CommandButton;

import com.threerings.parlor.client.SeatednessObserver;
import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.client.TableObserver;

import com.threerings.parlor.data.Table;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.MsoyController;

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
        _formingTables.removeAll();
    }

    // from TableObserver
    public function tableAdded (table :Table) :void
    {
        _formingTables.addItem(table);
    }

    // from TableObserver
    public function tableUpdated (table :Table) :void
    {
        var idx :int = ArrayUtil.indexOf(_formingTables.source, table);
        if (idx >= 0) {
            _formingTables.setItemAt(table, idx);

        } else {
            log.warning("Never found table to update: " + table);
        }
    }

    // from TableObserver
    public function tableRemoved (tableId :int) :void
    {
        for (var ii :int = 0; ii < _formingTables.length; ii++) {
            var table :Table = (_formingTables.getItemAt(ii) as Table);
            if (table.tableId == tableId) {
                _formingTables.removeItemAt(ii);
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
        // here we'll want to do _formingTables.refresh() or, if we save
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

        // left side: logo, description, create table button and invite friend button
        var descriptionBox :VBox = new VBox();
        descriptionBox.width = 160;
        descriptionBox.percentHeight = 100;
        descriptionBox.styleName = "descriptionBox";
        gameBox.addChild(descriptionBox);
        var logo :VBox = new VBox();
        logo.styleName = "lobbyLogoBox";
        logo.width = 160;
        logo.height = 120;
        logo.addChild(new MediaWrapper(new MediaContainer(controller.game.getThumbnailPath())));
        logo.setStyle("backgroundImage", "/media/static/game/logo_background.png");
        descriptionBox.addChild(logo);
        descriptionBox.addChild(new MediaWrapper(new MediaContainer(
            "/media/static/game/info_top.png")));
        var infoBox :HBox = new HBox();
        infoBox.width = 160;
        infoBox.percentHeight = 100;
        infoBox.setStyle("backgroundImage", "/media/static/game/info_tile.png");
        infoBox.setStyle("backgroundSize", "100%");
        descriptionBox.addChild(infoBox);
        var info :Text = new Text();
        info.styleName = "lobbyInfo";
        info.percentWidth = 100;
        info.percentHeight = 100;
        info.text = controller.game.description;
        infoBox.addChild(info);
        createBtn = new CommandButton(LobbyController.CREATE_TABLE);
        createBtn.height = 22;
        createBtn.label = Msgs.GAME.get("b.create");
        var inviteBtn :CommandButton = new CommandButton();
        inviteBtn.height = 22;
        inviteBtn.label = Msgs.GAME.get("b.invite_to_game");
        var buttonBox :VBox = new VBox();
        buttonBox.setStyle("backgroundImage", "/media/static/game/info_bottom.png");
        buttonBox.setStyle("horizontalAlign", "center");
        buttonBox.percentWidth = 100;
        descriptionBox.addChild(buttonBox);
        var butbar :ButtonBar = new ButtonBar();
        butbar.setStyle("paddingTop", 40);
        butbar.setStyle("verticalGap", 5);
        butbar.direction = "vertical";
        butbar.addChild(createBtn);
        butbar.addChild(inviteBtn);
        buttonBox.addChild(butbar);

        // right side: name, tabs, about/buy links, tables and chat
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
        var name :Label = new Label();
        name.text = controller.game.name;
        name.styleName = "lobbyGameName";
        tabsBox.addChild(name);
        // must do this here so that tabs get dropped into the correct location, if they're needed
        createTablesDisplay(tabsBox, tablesBox);
        var padding :HBox = new HBox();
        padding.percentWidth = 100;
        padding.percentHeight = 100;
        tabsBox.addChild(padding);
        var about :Label = new Label();
        about.text = Msgs.GAME.get("b.about");
        about.styleName = "lobbyLink";
        var thisLobbyPanel :LobbyPanel = this;
        about.addEventListener(MouseEvent.CLICK, function () :void {
            CommandEvent.dispatch(thisLobbyPanel, MsoyController.VIEW_ITEM, 
                controller.game.getIdent());
        });
        tabsBox.addChild(about);
        // if ownerId = 0, we were pushed to the catalog's copy, so this is buyable
        if (controller.game.ownerId == 0) {
            var buy :Label = new Label();
            buy.text = Msgs.GAME.get("b.buy");
            buy.styleName = "lobbyLink";
            buy.addEventListener(MouseEvent.CLICK, function () :void {
                CommandEvent.dispatch(thisLobbyPanel, MsoyController.VIEW_ITEM, 
                    controller.game.getIdent());
            });
            tabsBox.addChild(buy);
        }
        var chatbox :ChatContainer = new ChatContainer(_ctx);
        chatbox.percentWidth = 100;
        chatbox.height = 100;
        tablesBox.addChild(chatbox);
    }

    protected function createTablesDisplay (tabsContainer :DisplayObjectContainer,
        tablesContainer :DisplayObjectContainer) :void
    {
        // our game table data
        var list :MsoyList = new MsoyList(_ctx);
        list.variableRowHeight = true;
        list.percentHeight = 100;
        list.percentWidth = 100;
        var factory :ClassFactory = new ClassFactory(TableRenderer);
        factory.properties = { ctx: _ctx, panel: this };
        list.itemRenderer = factory;
        list.dataProvider = _formingTables;

        // only display tabs for seated games
        if (controller.game.gameType == GameConfig.SEATED_GAME) {
            var tabBar :TabBar = new TabBar();
            tabsContainer.addChild(tabBar);

            var tabViews :ViewStack = new ViewStack();
            tabViews.percentHeight = 100;
            tabViews.percentWidth = 100;
            tablesContainer.addChild(tabViews);
            tabBar.dataProvider = tabViews;
            var formingBox :VBox = new VBox();
            formingBox.percentHeight = 100;
            formingBox.percentWidth = 100;
            formingBox.label = Msgs.GAME.get("t.forming");
            tabViews.addChild(formingBox);
            formingBox.addChild(list);
            var runningBox :VBox = new VBox();
            runningBox.percentHeight = 100;
            runningBox.percentWidth = 100;
            runningBox.label = Msgs.GAME.get("t.running");
            tabViews.addChild(runningBox);
        } else {
            tablesContainer.addChild(list);
        }
    }

    /** Buy one get one free. */
    protected var _ctx :WorldContext;

    /** Are we seated? */
    protected var _isSeated :Boolean;

    /** The currently forming tables. */
    protected var _formingTables :ArrayCollection = new ArrayCollection();

    /** The currently running tables. */
    protected var _runningTables :ArrayCollection = new ArrayCollection();
}
}
