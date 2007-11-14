//
// $Id$

package com.threerings.msoy.game.client {

import flash.events.Event;
import flash.events.MouseEvent;

import mx.collections.ArrayCollection;

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.List;
import mx.controls.Text;

import mx.core.ClassFactory;

import com.threerings.util.ArrayUtil;
import com.threerings.util.CommandEvent;
import com.threerings.util.Log;

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
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.ui.MediaWrapper;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyTable;
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
    public function LobbyPanel (ctx :GameContext, ctrl :LobbyController)
    {
        _gctx = ctx;
        _wctx = ctx.getWorldContext();
        controller = ctrl;

        width = LOBBY_PANEL_WIDTH;

        addEventListener(Event.ADDED_TO_STAGE, handleAdded);
        addEventListener(Event.REMOVED_FROM_STAGE, handleRemoved);
    }

    public function init (lobbyObj :LobbyObject, friendsOnly :Boolean) :void
    {
        _lobbyObj = lobbyObj;
        _friendsOnly = friendsOnly;

        // fill in the UI bits
        var game :Game = getGame();
        _title.text = game.name;
        _title.validateNow();
        _title.width = _title.textWidth + TextFieldUtil.WIDTH_PAD;
        _about.text = Msgs.GAME.get("b.about");
        var thisLobbyPanel :LobbyPanel = this;
        _about.addEventListener(MouseEvent.CLICK, function () :void {
            CommandEvent.dispatch(thisLobbyPanel, MsoyController.VIEW_GAME, game.gameId);
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

        // determine our informational messages
        if (GameConfig.SEATED_GAME == _lobbyObj.gameDef.match.getMatchType()) {
            _pendersHeader = Msgs.GAME.get("l.penders_header_seated");
            _noPendersMsg = Msgs.GAME.get(
                _friendsOnly ? "m.no_friends_seated" : "m.no_penders_seated");
        } else {
            _pendersHeader = Msgs.GAME.get("l.penders_header_party");
            _noPendersMsg = Msgs.GAME.get(
                _friendsOnly ? "m.no_friends_party" : "m.no_penders_party");
        }

        _tablesBox.removeAllChildren();
        createTablesDisplay();

        for each (var table :Table in _lobbyObj.tables.toArray()) {
            tableAdded(table);
        }
        if (_tables.length == 0) {
            _tables.addItem("M" + _noPendersMsg);
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
     * Returns thae definition for the game we're currently matchmaking.
     */
    public function getGameDefinition () :GameDefinition
    {
        return _lobbyObj != null ? _lobbyObj.gameDef : null;
    }

    // from AttributeChangeListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (event.getName() == MemberObject.GAME) {
            _creationPanel.setEnabled(!isSeated());
            _tables.refresh();
        }
    }

    // from TableObserver
    public function tableAdded (table :Table) :void
    {
        // if this table is running and we're a seated game, ignore it
        if (table.gameOid > 0 && GameConfig.SEATED_GAME == _lobbyObj.gameDef.match.getMatchType()) {
            return;
        }

        // if we're in friends only mode and this table does not contain a friend, skip it
        if (_friendsOnly && (table as MsoyTable).countFriends(_wctx.getMemberObject()) == 0) {
            return;
        }

        // if we're adding the first table, remove the "no tables" message and add the header
        if (_tables.length == 1 && _tables.getItemAt(0) is String) {
            _tables.removeItemAt(0);
        }
        if (_tables.length == 0) {
            _tables.addItem("H" + _pendersHeader);
        }

        // finally add the table at the bottom of the list
        _tables.addItem(table);
    }

    // from TableObserver
    public function tableUpdated (table :Table) :void
    {
        // if this table is running and we're a seated game, remove it
        if (table.gameOid > 0 && GameConfig.SEATED_GAME == _lobbyObj.gameDef.match.getMatchType()) {
            tableRemoved(table.tableId);
            return;
        }

        // if we're in friends only mode, this table may now be visible or not
        var idx :int = ArrayUtil.indexOf(_tables.source, table);
        if (_friendsOnly) {
            var count :int = (table as MsoyTable).countFriends(_wctx.getMemberObject());
            if (count > 0 && idx == -1) {
                tableAdded(table);
                return;
            }
            if (count == 0 && idx != -1) {
                tableRemoved(table.tableId);
                return;
            }
        }

        // otherwise update it
        if (idx >= 0) {
            _tables.setItemAt(table, idx);
        }
    }

    // from TableObserver
    public function tableRemoved (tableId :int) :void
    {
        var table :Table;
        for (var ii :int = 0; ii < _tables.length; ii++) {
            table = (_tables.getItemAt(ii) as Table);
            if (table != null && table.tableId == tableId) {
                _tables.removeItemAt(ii);
                if (_tables.length == 1 && _tables.getItemAt(0) is String) {
                    _tables.removeItemAt(0);
                    _tables.addItem("M" + _noPendersMsg);
                }
                return;
            }
        }
    }

    // from SeatednessObserver
    public function seatednessDidChange (nowSeated :Boolean) :void
    {
        _isSeated = nowSeated;
        _creationPanel.setEnabled(!isSeated());
        if (_isSeated) {
            CommandEvent.dispatch(this, LobbyController.LEAVE_LOBBY);
        }
    }

    /**
     * Returns true if we're seated at ANY table, even in another lobby.
     */
    public function isSeated () :Boolean
    {
        return _isSeated || (_wctx.getMemberObject().game != null);
    }

    /**
     * Handle Event.ADDED_TO_STAGE.
     */
    protected function handleAdded (... ignored) :void
    {
        _wctx.getMemberObject().addListener(this);
    }

    /**
     * Handle Event.REMOVED_FROM_STAGE.
     */
    protected function handleRemoved (... ignored) :void
    {
        _wctx.getMemberObject().removeListener(this);
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
            new EmbedDialog(_wctx);
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

        _contents = new VBox();
        addChild(_contents);
        _contents.styleName = "borderedBox";
        _contents.percentWidth = 100;
        _contents.percentHeight = 100;

        var descriptionBox :HBox = new HBox();
        descriptionBox.percentWidth = 100;
        descriptionBox.height = 124; // make room for padding at top
        descriptionBox.styleName = "descriptionBox";
        _contents.addChild(descriptionBox);
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
        _contents.addChild(_tablesBox);

        var loadingLabel :Label = new Label();
        loadingLabel.text = Msgs.GAME.get("l.gameLoading");
        _tablesBox.addChild(loadingLabel);
    }

    protected function createTablesDisplay () :void
    {
        // our game table data
        var list :List = new List();
        list.styleName = "lobbyTableList";
        list.variableRowHeight = true;
        list.percentHeight = 100;
        list.percentWidth = 100;
        list.selectable = false;
        var factory :ClassFactory = new ClassFactory(TableRenderer);
        factory.properties = { gctx: _gctx, panel: this };
        list.itemRenderer = factory;
        list.dataProvider = _tables;

        var bar :HBox = new HBox();
        bar.styleName = "tabsFillerBox";
        bar.percentWidth = 100;
        bar.height = 9;
        _tablesBox.addChild(bar);
        _tablesBox.addChild(list);

        _creationPanel = new TableCreationPanel(_gctx, this);
        _tablesBox.addChild(_creationPanel);
        _creationPanel.setEnabled(!isSeated());
    }

    protected static const LOBBY_PANEL_WIDTH :int = 500; // in px

    /** Provides world client services. */
    protected var _wctx :WorldContext;

    /** Buy one get one free. */
    protected var _gctx :GameContext;

    /** Our lobby object. */
    protected var _lobbyObj :LobbyObject;

    /** Are we showing only our friends' tables? */
    protected var _friendsOnly :Boolean;

    /** Are we seated? */
    protected var _isSeated :Boolean;

    /** The create a table interface. */
    protected var _creationPanel :TableCreationPanel;

    /** All joinable game tables. */
    protected var _tables :ArrayCollection = new ArrayCollection();

    // various UI bits that need filling in with data arrives
    protected var _contents :VBox;
    protected var _logo :VBox;
    protected var _info :Text;
    protected var _title :Label;
    protected var _about :Label;
    protected var _buy :Label;
    protected var _tablesBox :VBox;

    protected var _pendersHeader :String;
    protected var _noPendersMsg :String;
}
}
