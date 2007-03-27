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

import com.threerings.flex.CommandButton;

import com.threerings.parlor.client.SeatednessObserver;
import com.threerings.parlor.client.TableDirector;
import com.threerings.parlor.client.TableObserver;

import com.threerings.parlor.data.Table;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.WorldContext;

import com.threerings.msoy.chat.client.ChatContainer;

import com.threerings.msoy.ui.MsoyList;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.item.web.Game;

/**
 * A panel that displays pending table games.
 */
public class LobbyPanel extends VBox 
    implements TableObserver, SeatednessObserver
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

        width = LOBBY_PANEL_WIDTH;
         
        // TODO display some kind of loading indicator
    }

    public function init (lobbyObj :LobbyObject) :void
    {
        _lobbyObj = lobbyObj;
        // add all preexisting tables
        for each (var table :Table in _lobbyObj.tables.toArray()) {
            tableAdded(table);
        }
        createUI();
    }

    /**
     * Returns the configuration for the game we're currently matchmaking.
     */
    public function getGame () :Game
    {
        return _lobbyObj != null ? _lobbyObj.game : null;
    }

    // from TableObserver
    public function tableAdded (table :Table) :void
    {
        // TODO check if this is a single-player table - if so, add directly to running tables
        _formingTables.addItem(table);
    }

    // from TableObserver
    public function tableUpdated (table :Table) :void
    {
        var idx :int = ArrayUtil.indexOf(_formingTables.source, table);
        if (idx >= 0) {
            if (table.gameOid != -1 && GameConfig.SEATED_GAME == 
                getGame().getGameDefinition().gameType) {
                _formingTables.removeItemAt(idx);
                _runningTables.addItem(table);
            } else {
                _formingTables.setItemAt(table, idx);
            }
        } else {
            idx = ArrayUtil.indexOf(_runningTables.source, table);
            if (idx >= 0) {
                _runningTables.setItemAt(table, idx);
            } else {
                log.warning("Never found table to update: " + table);
            }
        }
    }

    // from TableObserver
    public function tableRemoved (tableId :int) :void
    {
        var table :Table;
        for (var ii :int = 0; ii < _runningTables.length; ii++) {
            table = (_runningTables.getItemAt(ii) as Table);
            if (table.tableId == tableId) {
                _runningTables.removeItemAt(ii);
                return;
            }
        }
        for (ii = 0; ii < _formingTables.length; ii++) {
            table = (_formingTables.getItemAt(ii) as Table);
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
        if (_isSeated) {
            CommandEvent.dispatch(this, LobbyController.LEAVE_LOBBY);
        }
    }

    public function isSeated () :Boolean
    {
        return _isSeated;
    }

    protected function createUI () :void
    {
        super.createChildren();
        styleName = "lobbyPanel";
        percentHeight = 100;

        var descriptionBox :HBox = new HBox();
        descriptionBox.percentWidth = 100;
        descriptionBox.height = 120;
        descriptionBox.styleName = "descriptionBox";
        addChild(descriptionBox);
        var logo :VBox = new VBox();
        logo.styleName = "lobbyLogoBox";
        logo.width = 160;
        logo.height = 120;
        logo.addChild(new MediaWrapper(new MediaContainer(getGame().getThumbnailPath())));
        logo.setStyle("backgroundImage", "/media/static/game/logo_background.png");
        descriptionBox.addChild(logo);
        // TODO get new art from jon for this, and get it working in here
        //descriptionBox.addChild(new MediaWrapper(new MediaContainer(
            //"/media/static/game/info_top.png")));
        var info :Text = new Text();
        info.styleName = "lobbyInfo";
        info.percentWidth = 100;
        info.percentHeight = 100;
        info.text = getGame().description;
        descriptionBox.addChild(info);

        var tablesBox :VBox = new VBox();
        tablesBox.styleName = "tablesBox";
        tablesBox.percentWidth = 100;
        tablesBox.percentHeight = 100;
        addChild(tablesBox);
        var tabsBox :HBox = new HBox();
        tabsBox.styleName = "tabsBox";
        tabsBox.percentWidth = 100;
        tabsBox.height = 27;
        tablesBox.addChild(tabsBox);
        tabsBox.setStyle("backgroundImage", "/media/static/game/box_tile.png");
        tabsBox.setStyle("backgroundSize", "100%");
        tabsBox.setStyle("verticalAlign", "middle");
        var name :Label = new Label();
        name.text = getGame().name;
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
             CommandEvent.dispatch(
                 thisLobbyPanel, MsoyController.VIEW_ITEM, getGame().getIdent());
         });
         tabsBox.addChild(about);

         // if ownerId = 0, we were pushed to the catalog's copy, so this is buyable
         // TODO: make sure we can't get here with a game that's a gift in somebody's mailbox!
         if (getGame().ownerId == 0) {
             var buy :Label = new Label();
             buy.text = Msgs.GAME.get("b.buy");
             buy.styleName = "lobbyLink";
             buy.addEventListener(MouseEvent.CLICK, function () :void {
                 CommandEvent.dispatch(thisLobbyPanel, MsoyController.VIEW_ITEM, 
                     getGame().getIdent());
             });
             tabsBox.addChild(buy);
         }

        var buttonBox :HBox = new HBox();
        buttonBox.styleName = "buttonBox";
        buttonBox.percentWidth = 100;
        createBtn = new CommandButton(LobbyController.CREATE_TABLE);
        createBtn.height = 22;
        createBtn.label = Msgs.GAME.get("b.create");
        buttonBox.addChild(createBtn);
        var leaveBtn :CommandButton = new CommandButton(LobbyController.LEAVE_LOBBY);
        leaveBtn.height = 22;
        leaveBtn.label = Msgs.GAME.get("b.leave_lobby");
        buttonBox.addChild(leaveBtn);
        addChild(buttonBox);
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
        if (getGame().getGameDefinition().gameType == GameConfig.SEATED_GAME) {
            var tabBar :TabBar = new TabBar();
            tabBar.percentHeight = 100;
            tabBar.styleName = "lobbyTabs";
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

            var runningList :MsoyList = new MsoyList(_ctx);
            runningList.variableRowHeight = true;
            runningList.percentHeight = 100;
            runningList.percentWidth = 100;
            var runningFactory :ClassFactory = new ClassFactory(TableRenderer);
            runningFactory.properties = { ctx: _ctx, panel: this };
            runningList.itemRenderer = runningFactory;
            runningList.dataProvider = _runningTables;
            var runningBox :VBox = new VBox();
            runningBox.percentHeight = 100;
            runningBox.percentWidth = 100;
            runningBox.label = Msgs.GAME.get("t.running");
            runningBox.addChild(runningList);
            tabViews.addChild(runningBox);
        } else {
            tablesContainer.addChild(list);
        }
    }

    protected static const LOBBY_PANEL_WIDTH :int = 500; // in px

    /** Buy one get one free. */
    protected var _ctx :WorldContext;

    /** Our lobby object. */
    protected var _lobbyObj :LobbyObject;

    /** Are we seated? */
    protected var _isSeated :Boolean;

    /** The currently forming tables. */
    protected var _formingTables :ArrayCollection = new ArrayCollection();

    /** The currently running tables. */
    protected var _runningTables :ArrayCollection = new ArrayCollection();
}
}
