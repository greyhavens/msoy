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
    }

    public function init (lobbyObj :LobbyObject) :void
    {
        _lobbyObj = lobbyObj;
        // add all preexisting tables
        for each (var table :Table in _lobbyObj.tables.toArray()) {
            tableAdded(table);
        }

        // fill in the UI bits
        var game :Game = getGame();
        _title.text = game.name;
        _about.text = Msgs.GAME.get("b.about");
        var thisLobbyPanel :LobbyPanel = this;
        _about.addEventListener(MouseEvent.CLICK, function () :void {
            CommandEvent.dispatch(thisLobbyPanel, MsoyController.VIEW_ITEM, game.getIdent());
        });
        // if ownerId = 0, we were pushed to the catalog's copy, so this is buyable
        if (game.ownerId == 0) {
            _buy.text = Msgs.GAME.get("b.buy");
            _buy.addEventListener(MouseEvent.CLICK, function () :void {
                CommandEvent.dispatch(thisLobbyPanel, MsoyController.VIEW_ITEM, game.getIdent());
            });
        } else {
            _buy.parent.removeChild(_buy);
        }

        _logo.addChild(new MediaWrapper(new MediaContainer(getGame().getThumbnailPath())));
        _info.text = game.description;

        _tablesBox.removeAllChildren();
        createTablesDisplay();
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
        if (_runningTables != null && table.playerCount == 1 && 
            getGame().getGameDefinition().gameType == GameConfig.SEATED_GAME) {
            _runningTables.addItem(table);
        } else {
            _formingTables.addItem(table);
        }
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

    override protected function createChildren () :void
    {
        super.createChildren();
        styleName = "lobbyPanel";
        percentHeight = 100;

        var titleBox :HBox = new HBox();
        titleBox.styleName = "titleBox";
        titleBox.percentWidth = 100;
        titleBox.height = 27;
        addChild(titleBox);
        _title = new Label();
        _title.styleName = "lobbyGameName";
        titleBox.addChild(_title);
        var padding :HBox = new HBox();
        padding.percentWidth = 100;
        padding.percentHeight = 100;
        titleBox.addChild(padding);
        _about = new Label();
        _about.styleName = "lobbyLink";
        titleBox.addChild(_about);
        _buy = new Label();
        _buy.styleName = "lobbyLink";
        titleBox.addChild(_buy);

        var descriptionBox :HBox = new HBox();
        descriptionBox.percentWidth = 100;
        descriptionBox.height = 120;
        descriptionBox.styleName = "descriptionBox";
        addChild(descriptionBox);
        _logo = new VBox();
        _logo.styleName = "lobbyLogoBox";
        _logo.width = 160;
        _logo.height = 120;
        descriptionBox.addChild(_logo);
        var infoBox :HBox = new HBox();
        infoBox.styleName = "infoBox";
        infoBox.percentWidth = 100;
        infoBox.percentHeight = 100;
        descriptionBox.addChild(infoBox);
        _info = new Text();
        _info.styleName = "lobbyInfo";
        _info.percentWidth = 100;
        _info.percentHeight = 100;
        infoBox.addChild(_info);

        _tablesBox = new VBox();
        _tablesBox.styleName = "tablesBox";
        _tablesBox.percentWidth = 100;
        _tablesBox.percentHeight = 100;
        addChild(_tablesBox);
        var loadingLabel :Label = new Label();
        loadingLabel.text = Msgs.GAME.get("l.gameLoading");
        _tablesBox.addChild(loadingLabel);

        // TODO: Temporary!
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

    protected function createTablesDisplay () :void
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
            var tabsBox :HBox = new HBox();
            tabsBox.styleName = "tabsBox";
            tabsBox.percentWidth = 100;
            tabsBox.height = 27;
            _tablesBox.addChild(tabsBox);
            var tabBar :TabBar = new TabBar();
            tabBar.percentHeight = 100;
            tabBar.styleName = "lobbyTabs";
            tabsBox.addChild(tabBar);

            var tabViews :ViewStack = new ViewStack();
            tabViews.percentHeight = 100;
            tabViews.percentWidth = 100;
            _tablesBox.addChild(tabViews);
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
            var bar :HBox = new HBox();
            // give  it the same style as the tabs bar, just shorter
            bar.styleName = "tabsBox"; 
            bar.percentWidth = 100;
            bar.height = 5;
            _tablesBox.addChild(bar);
            _tablesBox.addChild(list);
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

    // various UI bits that need filling in with data arrives
    protected var _logo :VBox;
    protected var _info :Text;
    protected var _title :Label;
    protected var _about :Label;
    protected var _buy :Label;
    protected var _tablesBox :VBox;
}
}
