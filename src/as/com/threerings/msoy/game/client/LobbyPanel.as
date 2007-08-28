//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;

import flash.events.Event;
import flash.events.MouseEvent;
import flash.events.TextEvent;

import mx.collections.ArrayCollection;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.containers.ViewStack;
import mx.controls.Label;
import mx.controls.Alert;
import mx.controls.Text;
import mx.controls.TabBar;

import mx.core.Container;
import mx.core.ClassFactory;

import com.threerings.util.ArrayUtil;
import com.threerings.util.CommandEvent;

import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.AttributeChangeListener;

import com.threerings.flash.MediaContainer;
import com.threerings.flash.TextFieldUtil;

import com.threerings.flex.CommandButton;

import com.threerings.parlor.client.SeatednessObserver;
import com.threerings.parlor.client.TableObserver;
import com.threerings.parlor.data.Table;
import com.threerings.parlor.game.data.GameConfig;

import com.threerings.ezgame.data.GameDefinition;

import com.threerings.msoy.client.EmbedDialog;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.HeaderBarController;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.chat.client.ChatContainer;

import com.threerings.msoy.ui.MsoyList;
import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.item.data.all.Game;

/**
 * A panel that displays pending table games.
 */
public class LobbyPanel extends VBox 
    implements TableObserver, SeatednessObserver, AttributeChangeListener
{
    /** Our log. */
    private const log :Log = Log.getLog(LobbyPanel);

    /** The lobby controller. */
    public var controller :LobbyController;

    /**
     * Create a new LobbyPanel.
     */
    public function LobbyPanel (ctx :WorldContext, gctx :GameContext, ctrl :LobbyController)
    {
        _ctx = ctx;
        _gctx = gctx;
        controller = ctrl;

        width = LOBBY_PANEL_WIDTH;

        addEventListener(Event.ADDED_TO_STAGE, handleAdded);
        addEventListener(Event.REMOVED_FROM_STAGE, handleRemoved);
    }

    public function init (lobbyObj :LobbyObject) :void
    {
        _lobbyObj = lobbyObj;

        // fill in the UI bits
        var game :Game = getGame();
        _title.text = game.name;
        _title.validateNow();
        _title.width = _title.textWidth + TextFieldUtil.WIDTH_PAD;
        _about.text = Msgs.GAME.get("b.about");
        var thisLobbyPanel :LobbyPanel = this;
        _about.addEventListener(MouseEvent.CLICK, function () :void {
            CommandEvent.dispatch(thisLobbyPanel, MsoyController.VIEW_ITEM, game.getIdent());
        });
        _about.buttonMode = true;
        _about.useHandCursor = true;
        _about.mouseChildren = false;
        // if ownerId = 0, we were pushed to the catalog's copy, so this is buyable
        if (game.ownerId == 0) {
            _buy.text = Msgs.GAME.get("b.buy");
            _buy.addEventListener(MouseEvent.CLICK, function () :void {
                CommandEvent.dispatch(thisLobbyPanel, MsoyController.VIEW_ITEM, game.getIdent());
            });
            _buy.buttonMode = true;
            _buy.useHandCursor = true;
            _buy.mouseChildren = false;
        } else {
            _buy.parent.removeChild(_buy);
        }

        _logo.addChild(new MediaWrapper(new MediaContainer(getGame().getThumbnailPath())));
        _info.text = game.description;

        _tablesBox.removeAllChildren();
        createTablesDisplay();
        // add all preexisting tables
        for each (var table :Table in _lobbyObj.tables.toArray()) {
            tableAdded(table);
        }
    }

    /**
     * Returns the configuration for the game we're currently matchmaking.
     */
    public function getGame () :Game
    {
        return _lobbyObj != null ? _lobbyObj.game : null;
    }

    /**
     * Returns the definition for the game we're currently matchmaking.
     */
    public function getGameDefinition () :GameDefinition
    {
        return _lobbyObj != null ? _lobbyObj.gameDef : null;
    }

    /**
     * Called to set the creation button.
     */
    public function setCreateButton (btn :CommandButton) :void
    {
        _createBtn = btn;
        updateCreateButton();
    }

    // from AttributeChangeListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (event.getName() == MemberObject.GAME) {
            updateCreateButton();
            if (_formingTables != null) {
                _formingTables.refresh();
            }
        }
    }

    // from TableObserver
    public function tableAdded (table :Table) :void
    {
        if (_runningTables != null && table.gameOid > 0) {
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
            if (table.gameOid > 0 && GameConfig.SEATED_GAME ==
                _lobbyObj.gameDef.match.getMatchType()) {
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
        for (var ii :int = 0; _runningTables != null && ii < _runningTables.length; ii++) {
            table = (_runningTables.getItemAt(ii) as Table);
            if (table.tableId == tableId) {
                _runningTables.removeItemAt(ii);
                return;
            }
        }
        for (ii = 0; ii < _formingTables.length; ii++) {
            table = (_formingTables.getItemAt(ii) as Table);
            if (table != null && table.tableId == tableId) {
                _formingTables.removeItemAt(ii);
                return;
            }
        }

        log.warning("Never found table to remove: " + tableId);
    }

    // from SeatednessObserver
    public function seatednessDidChange (nowSeated :Boolean) :void
    {
        _isSeated = nowSeated;
        updateCreateButton();
        if (_isSeated) {
            CommandEvent.dispatch(this, LobbyController.LEAVE_LOBBY);
        }
    }

    /**
     * Returns true if we're seated at ANY table, even in another lobby.
     */
    public function isSeated () :Boolean
    {
        return _isSeated || (_ctx.getMemberObject().game != null);
    }

    /**
     * Update the state of the create button.
     */
    protected function updateCreateButton () :void
    {
        if (_createBtn != null) {
            _createBtn.enabled = !isSeated();
        }
    }

    /**
     * Handle Event.ADDED_TO_STAGE.
     */
    protected function handleAdded (... ignored) :void
    {
        _ctx.getMemberObject().addListener(this);
    }

    /**
     * Handle Event.REMOVED_FROM_STAGE.
     */
    protected function handleRemoved (... ignored) :void
    {
        _ctx.getMemberObject().removeListener(this);
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        styleName = "lobbyPanel";
        percentHeight = 100;

        var titleBox :HBox = new HBox();
        titleBox.styleName = "titleBox";
        titleBox.percentWidth = 100;
        titleBox.height = 20;
        addChild(titleBox);
        _title = new Label();
        _title.styleName = "locationName";
        _title.width = 160;
        titleBox.addChild(_title);
        var padding :HBox = new HBox();
        padding.percentWidth = 100;
        padding.percentHeight = 100;
        titleBox.addChild(padding);

        var embedBtnBox :HBox = new HBox();
        _about = new Label();
        _about.styleName = "embedButton";
        embedBtnBox.addChild(_about);
        _buy = new Label();
        _buy.styleName = "embedButton";
        embedBtnBox.addChild(_buy);
        embedBtnBox.styleName = "headerEmbedBox";
        embedBtnBox.percentHeight = 100;
        titleBox.addChild(embedBtnBox);
        var embedBtn :Label = new Label();
        embedBtn.styleName = "embedButton";
        embedBtn.text = Msgs.GENERAL.get("l.share");
        var thisLobbyPanel :LobbyPanel = this;
        embedBtn.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
            new EmbedDialog(_ctx);
        });
        embedBtn.buttonMode = true;
        embedBtn.useHandCursor = true;
        embedBtn.mouseChildren = false;
        embedBtnBox.addChild(embedBtn);
        
        var leaveBtnBox :VBox = new VBox();
        leaveBtnBox.styleName = "lobbyCloseBox";
        leaveBtnBox.percentHeight = 100;
        titleBox.addChild(leaveBtnBox);
        var leaveBtn :CommandButton = new CommandButton(LobbyController.LEAVE_LOBBY);
        leaveBtn.styleName = "closeButton";
        leaveBtnBox.addChild(leaveBtn);

        var borderedBox :VBox = new VBox();
        addChild(borderedBox);
        borderedBox.styleName = "borderedBox";
        borderedBox.percentWidth = 100;
        borderedBox.percentHeight = 100;
        var descriptionBox :HBox = new HBox();
        descriptionBox.percentWidth = 100;
        descriptionBox.height = 124; // make room for padding at top
        descriptionBox.styleName = "descriptionBox";
        borderedBox.addChild(descriptionBox);
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
        borderedBox.addChild(_tablesBox);
        var loadingLabel :Label = new Label();
        loadingLabel.text = Msgs.GAME.get("l.gameLoading");
        _tablesBox.addChild(loadingLabel);
    }

    protected function createTablesDisplay () :void
    {
        // our game table data
        var list :MsoyList = new MsoyList(_ctx);
        list.styleName = "lobbyTableList";
        list.variableRowHeight = true;
        list.percentHeight = 100;
        list.percentWidth = 100;
        list.selectable = false;
        var factory :ClassFactory = new ClassFactory(TableRenderer);
        factory.properties = { gctx: _gctx, panel: this };
        list.itemRenderer = factory;
        _formingTables = new ArrayCollection();
        list.dataProvider = _formingTables;

        // only display tabs for seated games
        if (_lobbyObj.gameDef.match.getMatchType() == GameConfig.SEATED_GAME) {
            var tabsBox :HBox = new HBox();
            tabsBox.styleName = "tabsBox";
            tabsBox.percentWidth = 100;
            tabsBox.height = 20;
            _tablesBox.addChild(tabsBox);
            var tabFiller :HBox = new HBox();
            tabFiller.styleName = "tabsFillerBox";
            tabFiller.width = 5;
            tabFiller.height = 9;
            tabsBox.addChild(tabFiller);
            var tabBar :TabBar = new TabBar();
            tabBar.percentHeight = 100;
            tabsBox.addChild(tabBar);
            tabFiller = new HBox();
            tabFiller.styleName = "tabsFillerBox";
            tabFiller.percentWidth = 100;
            tabFiller.height = 9;
            tabsBox.addChild(tabFiller);

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
            runningList.styleName = "lobbyTableList";
            runningList.variableRowHeight = true;
            runningList.percentHeight = 100;
            runningList.percentWidth = 100;
            runningList.selectable = false;
            var runningFactory :ClassFactory = new ClassFactory(TableRenderer);
            runningFactory.properties = { gctx: _gctx, panel: this };
            runningList.itemRenderer = runningFactory;
            _runningTables = new ArrayCollection();
            runningList.dataProvider = _runningTables;
            var runningBox :VBox = new VBox();
            runningBox.percentHeight = 100;
            runningBox.percentWidth = 100;
            runningBox.label = Msgs.GAME.get("t.running");
            runningBox.addChild(runningList);
            tabViews.addChild(runningBox);

        } else {
            var bar :HBox = new HBox();
            bar.styleName = "tabsFillerBox"; 
            bar.percentWidth = 100;
            bar.height = 9;
            _tablesBox.addChild(bar);
            _tablesBox.addChild(list);
        }

        if (_formingTables.source.length > 0) {
            _formingTables.setItemAt(null, 0);
        } else {
            _formingTables.addItem(null);
        }
    }

    protected static const LOBBY_PANEL_WIDTH :int = 500; // in px

    /** Provides world client services. */
    protected var _ctx :WorldContext;

    /** Buy one get one free. */
    protected var _gctx :GameContext;

    /** Our lobby object. */
    protected var _lobbyObj :LobbyObject;

    /** Are we seated? */
    protected var _isSeated :Boolean;

    /** The create-a-table button. */
    protected var _createBtn :CommandButton;

    /** The currently forming tables. */
    protected var _formingTables :ArrayCollection;

    /** The currently running tables. */
    protected var _runningTables :ArrayCollection;

    // various UI bits that need filling in with data arrives
    protected var _logo :VBox;
    protected var _info :Text;
    protected var _title :Label;
    protected var _about :Label;
    protected var _buy :Label;
    protected var _tablesBox :VBox;
}
}
