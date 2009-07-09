//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;
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
    public static void createBackground (AbsolutePanel parent, String title)
    {
        String base = "bg";
        for (String row : new String[]{"top", "mid", "bot"}) {
            for (String col : new String[]{"left", "mid", "right"}) {
                FlowPanel cell = new FlowPanel();
                cell.addStyleName(base + "_" + row + "_" + col);
                cell.addStyleName(base + "_" + row + "_" + "row");
                cell.addStyleName(base + "_" + col + "_" + "col");
                DOM.setStyleAttribute(cell.getElement(), "position", "absolute");
                parent.add(cell);
            }
        }
        Widget label = MsoyUI.createLabel(title, base + "_title");
        DOM.setStyleAttribute(label.getElement(), "position", "absolute");
        parent.add(label);
    }

    public static SmartTable createTitleBar (String style, String title)
    {
        SmartTable box = new SmartTable(0, 0);
        box.setStyleName(style);
        box.setText(0, 0, "", 1, "nw");
        box.setText(0, 1, title, 1, "n");
        box.setText(0, 2, "", 1, "ne");

        FlexCellFormatter fmt = box.getFlexCellFormatter();
        fmt.addStyleName(0, 0, "left");
        fmt.addStyleName(0, 1, "center");
        fmt.addStyleName(0, 1, "title");
        fmt.addStyleName(0, 2, "right");
        return box;
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
        createBackground(this, "Daily Games");
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
