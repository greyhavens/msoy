//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlexTable.FlexCellFormatter;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameInfo;
import com.threerings.msoy.web.gwt.Pages;

import client.game.PlayButton;
import client.shell.DynamicLookup;
import client.ui.CreatorLabel;
import client.ui.MsoyUI;
import client.ui.Stars;
import client.ui.ThumbBox;

/**
 * Displays a list of featured games, defaulting to the first one
 */
public class FBFeaturedGamePanel extends AbsolutePanel
{
    public static SmartTable createBox (String style, String title, boolean withContents)
    {
        SmartTable box = new SmartTable(0, 0);
        box.setStyleName(style);
        box.setText(0, 0, "", 1, "nw");
        box.setText(0, 1, title, 1, "n");
        box.setText(0, 2, "", 1, "ne");

        if (withContents) {
            box.setText(1, 0, "", 1, "w");
            box.setText(1, 1, "", 1, "c");
            box.setText(1, 2, "", 1, "e");
            box.setText(2, 0, "", 1, "sw");
            box.setText(2, 1, "", 1, "s");
            box.setText(2, 2, "", 1, "se");
        }

        FlexCellFormatter fmt = box.getFlexCellFormatter();
        fmt.addStyleName(0, 0, "toprow");
        fmt.addStyleName(0, 1, "toprow");
        fmt.addStyleName(0, 2, "toprow");

        if (withContents) {
            fmt.addStyleName(1, 0, "centerrow");
            fmt.addStyleName(1, 1, "centerrow");
            fmt.addStyleName(1, 2, "centerrow");
        }

        if (withContents) {
            fmt.addStyleName(2, 0, "bottomrow");
            fmt.addStyleName(2, 1, "bottomrow");
            fmt.addStyleName(2, 2, "bottomrow");
        }

        fmt.addStyleName(0, 0, "leftcol");
        if (withContents) {
            fmt.addStyleName(1, 0, "leftcol");
            fmt.addStyleName(2, 0, "leftcol");
        }

        fmt.addStyleName(0, 1, "centercol");
        if (withContents) {
            fmt.addStyleName(1, 1, "centercol");
            fmt.addStyleName(2, 1, "centercol");
        }

        fmt.addStyleName(0, 2, "rightcol");
        if (withContents) {
            fmt.addStyleName(1, 2, "rightcol");
            fmt.addStyleName(2, 2, "rightcol");
        }

        fmt.addStyleName(0, 1, "title");

        return box;
    }

    public static SmartTable createBox (String style, String title)
    {
        return createBox(style, title, true);
    }

    public static SmartTable createTitleBar (String style, String title)
    {
        return createBox(style, title, false);
    }

    public FBFeaturedGamePanel (GameInfo[] games)
    {
        setStyleName("fbfeaturedGame");
        _games = games;
        if (games.length > 0) {
            selectGame(0);
        }
    }

    protected void selectGame (int index)
    {
        clear();
        GameInfo game = _games[index];
        add(createBox("background", "Daily Games"), 0, 0);
        add(new ThumbBox(game.shotMedia, MediaDesc.GAME_SHOT_SIZE, Pages.GAMES, "d", game.gameId),
            10, 37);
        add(createScroller(index), 10, 165);
        add(createGameBits(game), 198, 37);
    }

    protected SmartTable createScroller (final int currentSelection)
    {
        String imgBase = "/images/facebook/scroller_";
        SmartTable scroller = new SmartTable(0, 0);
        scroller.setStyleName("scroller");
        scroller.setWidget(0, 0, MsoyUI.createActionImage(imgBase + "left.png",
            new ClickHandler() {
            public void onClick (ClickEvent event) {
                selectGame((currentSelection + _games.length-1) % _games.length);
            }
        }), 1, "left");
        final int SCROLL_COUNT = 5;
        for (int ii = 0; ii < SCROLL_COUNT; ++ii) {
            if (ii == currentSelection) {
                scroller.setWidget(0, ii + 1,
                    MsoyUI.createImage(imgBase + "selected.png", null), 1, "dot");
            } else if (ii < _games.length) {
                final int fii = ii;
                scroller.setWidget(0, ii + 1,
                    MsoyUI.createActionImage(imgBase + "deselected.png",
                        new ClickHandler() {
                        public void onClick (ClickEvent event) {
                            selectGame(fii);
                        }
                    }), 1, "dot");
            }
        }
        scroller.setWidget(0, SCROLL_COUNT + 1, MsoyUI.createActionImage(imgBase + "right.png",
            new ClickHandler() {
            public void onClick (ClickEvent event) {
                selectGame((currentSelection + 1) % _games.length);
            }
        }), 1, "right");
        return scroller;
    }

    protected static SmartTable createGameBits (GameInfo game)
    {
        SmartTable bits = new SmartTable(0, 0);
        bits.setStyleName("bits");
        bits.setText(0, 0, game.name, 1, "name");
        String genre = _dmsgs.xlate("genre_" + game.genre);
        bits.setText(1, 0, genre, 1, "genre");
        bits.setWidget(2, 0, new CreatorLabel(game.creator), 1, "creator");

        bits.setWidget(0, 1, new Stars(game.rating, true, false, null), 1, "starsCell");
        bits.getFlexCellFormatter().setRowSpan(0, 1, 2);
        if (game.playersOnline > 0) {
            bits.setText(2, 1, _msgs.featuredOnline(""+game.playersOnline), 1, "online");
        }
        // TODO: make the description float around the play button (I played trial-and-error with
        // this for 45 minutes and nothing worked... time to move on!)
        bits.setText(3, 0, MsoyUI.truncateParagraph(game.description, 100), 1, "description");
        bits.setWidget(3, 1, PlayButton.createCustom(game.gameId, "play"), 1, "playCell");
        return bits;
    }

    protected GameInfo[] _games;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
