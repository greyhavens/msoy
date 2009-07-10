//
// $Id$

package client.games;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;

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
public class FBGameHeaderPanel extends FlowPanel
{
    public FBGameHeaderPanel ()
    {
        setStyleName("fbgameHeaderPanel");

        SmartTable contents = new SmartTable(0, 0);
        add(contents);

        // genre links
        FlowPanel genreLinks = MsoyUI.createFlowPanel("GenreLinks");
        contents.setWidget(0, 0, genreLinks, 1, "GenreLinksCell");
        genreLinks.add(Link.create(_dmsgs.xlate("genreBrief_" + GameGenre.ALL), Pages.GAMES, "g"));
        for (GameGenre gcode : GameGenre.DISPLAY_GENRES) {
            if (!ArcadeData.Portal.FACEBOOK.showGenre(gcode)) {
                continue;
            }
            genreLinks.add(new InlineLabel("|"));
            genreLinks.add(Link.create(_dmsgs.xlate("genreBrief_" + gcode),
                                       Pages.GAMES, "g", gcode.toByte()));
        }

        // find a game fast dropdown box
        FlowPanel findGame = MsoyUI.createFlowPanel("FindGame");
        contents.setWidget(0, 1, findGame, 1, "FindGameCell");
        _findGameBox = new ListBox();
        _findGameBox.addItem("Jump To A Game Here", "");
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

    protected ListBox _findGameBox;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
