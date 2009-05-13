//
// $Id$

package client.games;

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

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.game.data.all.GameGenre;
import com.threerings.msoy.game.gwt.GameCard;
import com.threerings.msoy.game.gwt.GameInfo;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * Displays the title of the page, "find a game fast" and "search for games" boxes
 */
public class GameHeaderPanel extends FlowPanel
{
    public GameHeaderPanel (
        final byte genre, final byte sortMethod, final String query, String titleText)
    {
        setStyleName("gameHeaderPanel");
        _genre = genre;

        add(MsoyUI.createLabel(titleText, "GenreTitle"));

        // find a game fast dropdown box
        FlowPanel findGame = MsoyUI.createFlowPanel("FindGame");
        findGame.add(MsoyUI.createLabel(_msgs.genreFindGame(), "Title"));
        add(findGame);
        _findGameBox = new ListBox();
        _findGameBox.addItem("", "");
        _findGameBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                ListBox listBox = (ListBox) event.getSource();
                String selectedValue = listBox.getValue(listBox.getSelectedIndex());
                if (!selectedValue.equals("")) {
                    Link.go(Pages.GAMES, Args.compose("d", selectedValue));
                }
            }
        });
        findGame.add(_findGameBox);

        // search for games
        FlowPanel search = MsoyUI.createFlowPanel("Search");
        search.add(MsoyUI.createLabel(_msgs.genreSearch(), "Title"));
        add(search);
        final TextBox searchBox = new TextBox();
        searchBox.setVisibleLength(20);
        if (query != null) {
            searchBox.setText(query);
        }
        ClickHandler searchListener = new ClickHandler() {
            public void onClick (ClickEvent event) {
                String newQuery = searchBox.getText().trim();
                Link.go(Pages.GAMES, Args.compose("g", genre, sortMethod, newQuery));
            }
        };
        searchBox.addKeyPressHandler(new EnterClickAdapter(searchListener));
        search.add(searchBox);
        search.add(MsoyUI.createImageButton("GoButton", searchListener));

        // add a link to the genre links
        FlowPanel genreLinks = MsoyUI.createFlowPanel("GenreLinks");
        add(genreLinks);
        for (byte gcode : GameGenre.GENRES) {
            if (genreLinks.getWidgetCount() > 0) {
                genreLinks.add(new InlineLabel("|"));
            }
            genreLinks.add(Link.create(_dmsgs.xlate("genre" + gcode), Pages.GAMES,
                                       Args.compose("g", gcode)));
        }
    }

    protected void initWithCards (List<GameCard> games)
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

    /** Dropdown of all games */
    protected ListBox _findGameBox;

    /** Genre ID or -1 for All Games page */
    protected byte _genre;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
