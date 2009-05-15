//
// $Id$

package client.landing;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Pages;

import client.game.GameNamePanel;
import client.game.PlayButton;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.MediaUtil;

/**
 * Displays a list of popular games for the landing page, and randomly selects one of them to
 * default to.
 */
public class TopGamesPanel extends AbsolutePanel
{
    public TopGamesPanel ()
    {
        setStyleName("TopGamesPanel");
    }

    /**
     * Called when the list of featured games has been fetched.
     */
    public void setGames (GameInfo[] games)
    {
        // take the first 5 featured games maximum
        if (games.length > 5) {
           _games = new GameInfo[5];
           for (int i = 0; i < 5; i++) {
               _games[i] = games[i];
           }
        }
        else {
            _games = games;
        }
        if (games.length == 0) {
            return;
        }

        // game info panel
        final SimplePanel gameInfoBox = new SimplePanel();
        gameInfoBox.setStyleName("GameInfoContainer");
        _gameInfo = new SimplePanel();
        _gameInfo.setStyleName("GameInfo");
        gameInfoBox.add(_gameInfo);
        add(gameInfoBox, 0, 0);

        // top games list
        _topGamesTable = new SmartTable(0, 0);
        for (int i = 0; i < _games.length; i++) {
            _topGamesTable.setWidget(i, 0, createGameListItem(i));
        }
        add(_topGamesTable, 445, 25);

        // top games header
        FlowPanel topGamesHeader = new FlowPanel();
        topGamesHeader.setStyleName("TopGamesHeader");
        add(topGamesHeader, 445, 0);

        int randomGameIndex = (int)(Math.random() * _games.length);
        showGame(randomGameIndex);
    }

    /**
     * Create and return a panel with the game thumbnail and name
     */
    protected Widget createGameListItem (final int index)
    {
        GameInfo game = _games[index];

        // Outer panel with onclick - change the game
        FocusPanel gamePanel = new FocusPanel();
        gamePanel.setStyleName("GameListItem");
        ClickHandler gameClick = new ClickHandler() {
            public void onClick (ClickEvent event) {
                showGame(index);
            }
        };
        gamePanel.addClickHandler(gameClick);

        // Inner flow panel with thumbnail and name
        SmartTable gamePanelInner = new SmartTable(0, 0);

        SimplePanel thumbnail = new SimplePanel();
        Widget thumbnailImage = MediaUtil.createMediaView(
            game.thumbMedia, MediaDesc.HALF_THUMBNAIL_SIZE);
        thumbnail.setStyleName("Thumbnail");
        thumbnail.add(thumbnailImage);
        gamePanelInner.setWidget(0, 0, thumbnail);
        gamePanelInner.getFlexCellFormatter().setHorizontalAlignment(
            0, 0, HasAlignment.ALIGN_CENTER);
        gamePanelInner.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_MIDDLE);

        SimplePanel name = new SimplePanel();
        name.setStyleName("Name");
        name.add(MsoyUI.createHTML(game.name, null));
        gamePanelInner.setWidget(0, 1, name);
        gamePanelInner.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_MIDDLE);

        gamePanel.add(gamePanelInner);

        return gamePanel;
    }

    /**
     * Select and display one of the featured games.
     */
    protected void showGame (final int index)
    {
        final GameInfo game = _games[index];

        // select the game being shown
        for (int i = 0; i < _games.length; i++) {
            FocusPanel gamePanel = (FocusPanel)_topGamesTable.getWidget(i, 0);
            if (i == index) {
                gamePanel.setStyleName("GameListItemSelected");
            }
            else {
                gamePanel.setStyleName("GameListItem");
            }
        }

        SmartTable gameInfoTable = new SmartTable("FeaturedGame", 0, 0);

        VerticalPanel left = new VerticalPanel();
        left.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        left.add(new ThumbBox(game.shotMedia, MediaDesc.GAME_SHOT_SIZE,
                              Pages.GAMES, "d", game.gameId));

        if (game.playersOnline > 0) {
            left.add(WidgetUtil.makeShim(10, 10));
            left.add(MsoyUI.createLabel(_msgs.topGamesOnline("" + game.playersOnline), "Online"));
        }

        // left and right arrows
        left.add(WidgetUtil.makeShim(10, 10));
        left.add(MsoyUI.createPrevNextButtons(new ClickHandler() {
            public void onClick (ClickEvent event) {
                showGame((index+_games.length-1)%_games.length);
            }
        }, new ClickHandler() {
            public void onClick (ClickEvent event) {
                showGame((index+1)%_games.length);
            }
        }));
        left.add(WidgetUtil.makeShim(10, 10));

        gameInfoTable.setWidget(0, 0, left);
        gameInfoTable.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        gameInfoTable.getFlexCellFormatter().setWidth(
            0, 0, MediaDesc.getWidth(MediaDesc.GAME_SHOT_SIZE) + "px");
        gameInfoTable.setWidget(0, 1, WidgetUtil.makeShim(10, 10));

        // game text info on the right
        FlowPanel right = new FlowPanel();
        right.setStyleName("RightPanel");
        VerticalPanel gameName = new GameNamePanel(
            game.name, game.genre, game.creator, game.description);
        right.add(gameName);

        // play button
        right.add(PlayButton.create(game, "", PlayButton.Size.MEDIUM));

        // more games button
        right.add(MsoyUI.createImageButton("MoreGames", Link.createHandler(Pages.GAMES, "")));

        gameInfoTable.setWidget(0, 2, right);
        gameInfoTable.getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        gameInfoTable.setWidget(1, 0, WidgetUtil.makeShim(5, 5));
        gameInfoTable.getFlexCellFormatter().setColSpan(1, 0, 3);

        // replace the game details
        if (_gameInfo.getWidget() != null) {
            _gameInfo.remove(_gameInfo.getWidget());
        }
        _gameInfo.add(gameInfoTable);
    }

    /** Game data */
    protected GameInfo[] _games;

    /** Game list table; every row is a game whose class changes when selected */
    protected SmartTable _topGamesTable;

    /** Game info panel; changes when a new game is selected */
    protected SimplePanel _gameInfo;

    protected static final LandingMessages _msgs = GWT.create(LandingMessages.class);
}
