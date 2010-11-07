//
// $Id$

package client.game;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.game.gwt.ArcadeData;
import com.threerings.msoy.game.gwt.GameCard;
import com.threerings.msoy.game.gwt.GameGenre;
import com.threerings.msoy.game.gwt.GameInfo;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays the title of the page, "find a game fast" and "search for games" boxes
 */
public class GameHeaderPanel extends FlowPanel
{
    public GameHeaderPanel (ArcadeData.Portal portal, String titleText, GameGenre genre,
        final GameInfo.Sort sort)
    {
        setStyleName("gameHeaderPanel");
        _genre = genre;

        FlowPanel absbits = MsoyUI.createFlowPanel("Absolute");
        add(absbits);
        absbits.add(MsoyUI.createLabel(titleText, "GenreTitle"));

        // find a game fast dropdown box
        FlowPanel findGame = MsoyUI.createFlowPanel("FindGame");
        findGame.add(MsoyUI.createLabel(_msgs.genreFindGame(), "Title"));
        absbits.add(findGame);
        _findGameBox = new ListBox();
        _findGameBox.addItem("", "");
        _findGameBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                ListBox listBox = (ListBox) event.getSource();
                String selectedValue = listBox.getValue(listBox.getSelectedIndex());
                if (!selectedValue.equals("")) {
                    Link.go(Pages.GAMES, "d", selectedValue);
                }
            }
        });
        findGame.add(_findGameBox);

        // search for games
        FlowPanel search = MsoyUI.createFlowPanel("Search");
        search.add(MsoyUI.createLabel(_msgs.genreSearch(), "Title"));
        absbits.add(search);
        _searchBox = MsoyUI.createTextBox("", 30, 20);
        ClickHandler searchListener = new ClickHandler() {
            public void onClick (ClickEvent event) {
                Link.go(Pages.GAMES, "g", _genre.toByte(), sort.toToken(), getQuery());
            }
        };
        EnterClickAdapter.bind(_searchBox, searchListener);
        search.add(_searchBox);
        search.add(MsoyUI.createImageButton("GoButton", searchListener));

        // add a link to the genre links
        FlowPanel genreLinks = MsoyUI.createFlowPanel("GenreLinks");
        add(genreLinks);
        for (GameGenre gcode : GameGenre.DISPLAY_GENRES) {
            if (!portal.showGenre(gcode)) {
                continue;
            }
            if (genreLinks.getWidgetCount() > 0) {
                genreLinks.add(new InlineLabel("|"));
            }
            genreLinks.add(Link.create(_dmsgs.xlate("genre_" + gcode),
                                       Pages.GAMES, "g", gcode.toByte()));
        }
    }

    public void setQuery (String query)
    {
        _searchBox.setText(query);
    }

    public String getQuery ()
    {
        return _searchBox.getText().trim();
    }

    public void initWithCards (List<GameCard> games)
    {
        for (GameCard game : games) {
            _findGameBox.addItem(game.name, game.gameId+"");
        }
    }

    protected void initWithInfos (List<GameInfo> games)
    {
        for (GameInfo game : games) {
            _findGameBox.addItem(game.name, game.gameId+"");
        }
    }

    protected GameGenre _genre;
    protected ListBox _findGameBox;
    protected TextBox _searchBox;

    protected static final GameMessages _msgs = GWT.create(GameMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
