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

import com.threerings.flash.TextFieldUtil;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandLinkButton;

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

import com.threerings.msoy.ui.MsoyUI;
import com.threerings.msoy.ui.SkinnableImage;
import com.threerings.msoy.ui.ThumbnailPanel;

import com.threerings.msoy.game.data.LobbyObject;
import com.threerings.msoy.game.data.MsoyMatchConfig;
import com.threerings.msoy.game.data.MsoyTable;
import com.threerings.msoy.item.data.all.Game;

/**
 * A panel that displays pending table games.
 */
public class LobbyPanel extends VBox
    implements TableObserver, SeatednessObserver, AttributeChangeListener
{
    /** The width of the lobby panel. */
    public static const LOBBY_PANEL_WIDTH :int = 500; // in px

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
        _about.label = Msgs.GAME.get("b.about");
        _about.setCommand(MsoyController.VIEW_GAME, game.gameId);
        // if ownerId = 0, we were pushed to the catalog's copy, so this is buyable
        if (game.ownerId == 0) {
            _buy.label = Msgs.GAME.get("b.buy");
            _buy.setCommand(MsoyController.VIEW_ITEM, game.getIdent());
        } else {
            _buy.parent.removeChild(_buy);
        }

        _logo.setItem(getGame());
        _info.text = game.description;

        // determine our informational messages
        var noPendersMsg :String, pendersHeader :String, runningHeader :String;
        if (GameConfig.SEATED_GAME == _lobbyObj.gameDef.match.getMatchType()) {
            noPendersMsg = Msgs.GAME.get(
                _friendsOnly ? "m.no_friends_seated" : "m.no_penders_seated");
            pendersHeader = Msgs.GAME.get("l.penders_header_seated");
            if ((_lobbyObj.gameDef.match as MsoyMatchConfig).unwatchable) {
                runningHeader = Msgs.GAME.get("l.running_header_seated_nowatch");
            } else {
                runningHeader = Msgs.GAME.get("l.running_header_seated");
            }

        } else {
            noPendersMsg = Msgs.GAME.get(
                _friendsOnly ? "m.no_friends_party" : "m.no_penders_party");
            pendersHeader = ""; // this is never used party games start immediately
            runningHeader = Msgs.GAME.get("l.running_header_party");
        }

        _noTablesLabel = MsoyUI.createLabel(noPendersMsg, "tableMessage");
        _tableList.addChild(_noTablesLabel);

        _tableList.addChild(_pendingList = new VBox());
        _pendingList.styleName = "pendingTableList";
        _pendingList.percentWidth = 100;
        var header :HBox = new HBox();
        header.percentWidth = 100;
        header.styleName = "tableHeader";
        header.addChild(MsoyUI.createLabel(pendersHeader));
        _pendingList.addChild(header);

        _tableList.addChild(_runningList = new VBox());
        _runningList.styleName = "runningTableList";
        _runningList.percentWidth = 100;
        header = new HBox();
        header.percentWidth = 100;
        header.styleName = "tableHeader";
        header.addChild(MsoyUI.createLabel(runningHeader));
        _runningList.addChild(header);

        // create our table creation panel now that we have our game config
        _creationPanel = new TableCreationPanel(_gctx, this);
        _creationPanel.enabled = !isSeated();

        for each (var table :Table in _lobbyObj.tables.toArray()) {
            tableAdded(table);
        }
        updateTableList();
        if (_pendingList.numChildren == 1 && _runningList.numChildren == 1) {
            showCreateGame();
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

    /**
     * Returns true if we're seated at ANY table, even in another lobby.
     */
    public function isSeated () :Boolean
    {
        return _isSeated || (_wctx.getMemberObject().game != null);
    }

    /**
     * Hides the header display and shows the create game interface.
     */
    public function showCreateGame () :void
    {
        if (_headerBox.parent != null) {
            _contents.removeChild(_headerBox);
        }
        if (_creationPanel.parent == null) {
            _contents.addChildAt(_creationPanel, 0);
            _creationPanel.updateOnlineFriends();
        }
    }

    /**
     * Hides the create game interface and shows the header display.
     */
    public function hideCreateGame () :void
    {
        if (_creationPanel.parent != null) {
            _contents.removeChild(_creationPanel);
        }
        if (_headerBox.parent == null) {
            _contents.addChildAt(_headerBox, 0);
        }
    }

    // from AttributeChangeListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (event.getName() == MemberObject.GAME) {
            _createBtn.enabled = !isSeated();
            if (isSeated()) {
                hideCreateGame();
            }
            // we might end up in here before we have our lobby object because this event came in
            // on the member object
            if (_lobbyObj != null) {
                for each (var table :MsoyTable in _lobbyObj.tables.toArray()) {
                    var panel :TablePanel = getTablePanel(table.tableId);
                    if (panel != null) {
                        panel.update(table, isSeated());
                    }
                }
            }
        }
    }

    // from TableObserver
    public function tableAdded (table :Table) :void
    {
        // if we're in friends only mode and this table does not contain a friend, skip it
        if (_friendsOnly && (table as MsoyTable).countFriends(_wctx.getMemberObject()) == 0) {
            return;
        }

        // add the table at the bottom of the list
        var list :VBox = (table.gameOid > 0) ? _runningList : _pendingList;
        list.addChild(new TablePanel(_gctx, this, table as MsoyTable));
        updateTableList();
    }

    // from TableObserver
    public function tableUpdated (table :Table) :void
    {
        var panel :TablePanel = getTablePanel(table.tableId);

        // if we're in friends only mode, this table may now be visible or not
        if (_friendsOnly) {
            var count :int = (table as MsoyTable).countFriends(_wctx.getMemberObject());
            if (count > 0 && panel == null) {
                tableAdded(table);
                return;
            }
            if (count == 0 && panel != null) {
                tableRemoved(table.tableId);
                return;
            }
        }

        // if we have no ui for it, no problem, stop here
        if (panel == null) {
            return;
        }

        // if the table switched from pending to running, move it
        if (table.gameOid > 0 && panel.parent == _pendingList) {
            _pendingList.removeChild(panel);
            _runningList.addChild(panel);
            updateTableList();
        }

        // and update it
        panel.update(table as MsoyTable, isSeated());
    }

    // from TableObserver
    public function tableRemoved (tableId :int) :void
    {
        var panel :TablePanel = getTablePanel(tableId);
        if (panel != null) {
            panel.parent.removeChild(panel);
            updateTableList();
        }
    }

    // from SeatednessObserver
    public function seatednessDidChange (nowSeated :Boolean) :void
    {
        _isSeated = nowSeated;
        _createBtn.enabled = !isSeated();
        if (isSeated()) {
            hideCreateGame();
        }
        if (_isSeated) {
            CommandEvent.dispatch(this, LobbyController.SAT_AT_TABLE);
        }
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
        _title = MsoyUI.createLabel("", "locationName");
        _title.width = 160;
        titleBox.addChild(_title);
        var padding :HBox = new HBox();
        padding.percentWidth = 100;
        padding.percentHeight = 100;
        titleBox.addChild(padding);

        var embedBtnBox :HBox = new HBox();
        _about = new CommandLinkButton();
        _about.styleName = "headerLink";
        _buy = new CommandLinkButton();
        _buy.styleName = "headerLink";
        embedBtnBox.addChild(_about);
        embedBtnBox.addChild(_buy);
        embedBtnBox.styleName = "headerEmbedBox";
        embedBtnBox.percentHeight = 100;
        titleBox.addChild(embedBtnBox);
        var embedBtn :CommandLinkButton = new CommandLinkButton();
        embedBtn.styleName = "headerLink";
        embedBtn.label = Msgs.GENERAL.get("b.share")
        embedBtn.setCallback(function () :void {
            new EmbedDialog(_wctx);
        });
        embedBtnBox.addChild(embedBtn);

        var leaveBtnBox :VBox = new VBox();
        leaveBtnBox.styleName = "lobbyCloseBox";
        leaveBtnBox.percentHeight = 100;
        titleBox.addChild(leaveBtnBox);
        var leaveBtn :CommandButton = new CommandButton(LobbyController.CLOSE_LOBBY);
        leaveBtn.styleName = "closeButton";
        leaveBtnBox.addChild(leaveBtn);

        _contents = new VBox();
        addChild(_contents);
        _contents.styleName = "contentsBox";
        _contents.percentWidth = 100;
        _contents.percentHeight = 100;

        _headerBox = new HBox();
        _headerBox.percentWidth = 100;
        _headerBox.styleName = "descriptionBox";
        _contents.addChild(_headerBox);

        _headerBox.addChild(_logo = new ThumbnailPanel());

        var infoBox :HBox = new HBox();
        infoBox.styleName = "infoBox";
        infoBox.percentWidth = 100;
        infoBox.percentHeight = 100;
        _info = new Text();
        _info.styleName = "lobbyInfo";
        _info.percentWidth = 100;
        _info.percentHeight = 100;
        infoBox.addChild(_info);
        _headerBox.addChild(infoBox);

        var startBox :HBox = new HBox();
        startBox.styleName = "startBox";
        startBox.percentHeight = 100;
        _createBtn = new CommandButton();
        _createBtn.label = Msgs.GAME.get("b.start_game");
        _createBtn.setCallback(function () :void {
            showCreateGame();
        });
        startBox.addChild(_createBtn);
        _headerBox.addChild(startBox);

        var tablesHeader :HBox = new HBox();
        tablesHeader.setStyle("horizontalGap", 0);
        tablesHeader.percentWidth = 100;
        tablesHeader.addChild(new SkinnableImage("tablesStar"));
        var thCenter :HBox = new HBox();
        thCenter.styleName = "tablesTitle";
        thCenter.percentWidth = 100;
        thCenter.height = 20;
        thCenter.addChild(MsoyUI.createLabel("Game Tables"));
        tablesHeader.addChild(thCenter);
        tablesHeader.addChild(new SkinnableImage("tablesStar"));
        _contents.addChild(tablesHeader);

        _tableList = new VBox();
        _tableList.styleName = "lobbyTableList";
        _tableList.percentWidth = 100;
        _tableList.percentHeight = 100;
        _contents.addChild(_tableList);
    }

    protected function updateTableList () :void
    {
        var havePending :Boolean = (_pendingList.numChildren > 1);
        var haveRunning :Boolean = (_runningList.numChildren > 1);
        _pendingList.visible = _pendingList.includeInLayout = havePending;
        _runningList.visible = _runningList.includeInLayout = haveRunning;
        _noTablesLabel.visible = _noTablesLabel.includeInLayout = !(havePending || haveRunning);
    }

    protected function getTablePanel (tableId :int) :TablePanel
    {
        for (var ii :int = 0; ii < _pendingList.numChildren; ii++) {
            var child :TablePanel = (_pendingList.getChildAt(ii) as TablePanel);
            if (child != null && child.tableId == tableId) {
                return child;
            }
        }
        for (ii = 0; ii < _runningList.numChildren; ii++) {
            child = (_runningList.getChildAt(ii) as TablePanel);
            if (child != null && child.tableId == tableId) {
                return child;
            }
        }
        return null;
    }

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

    // various UI bits that need filling in with data arrives
    protected var _headerBox :HBox;
    protected var _contents :VBox;
    protected var _logo :ThumbnailPanel;
    protected var _info :Text;
    protected var _title :Label;
    protected var _about :CommandLinkButton;
    protected var _buy :CommandLinkButton;
    protected var _createBtn :CommandButton;

    protected var _noTablesLabel :Label;
    protected var _tableList :VBox;
    protected var _pendingList :VBox;
    protected var _runningList :VBox;

    /** Our log. */
    private const log :Log = Log.getLog(LobbyPanel);
}
}
