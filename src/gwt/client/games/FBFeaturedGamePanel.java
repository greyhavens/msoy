//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.gwt.ui.FloatPanel;
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
public class FBFeaturedGamePanel extends AbsoluteCSSPanel
{
    public FBFeaturedGamePanel (GameInfo[] games)
    {
        super("fbfeaturedGame", "fixed");
        _games = games;
        if (games.length > 0) {
            selectGame(0);
        }
    }

    protected void selectGame (int index)
    {
        clear();
        GameInfo game = _games[index];
        add(MsoyUI.createLabel("Daily Games", "title"));
        add(new ThumbBox(game.shotMedia, MediaDesc.GAME_SHOT_SIZE, Pages.GAMES, "d", game.gameId));
        add(createScroller(index));
        add(createGameBits(game));
    }

    protected Widget createScroller (final int currentSelection)
    {
        FloatPanel scroller = new FloatPanel("scroller");
        scroller.add(MsoyUI.createImageButton("fbscrollLeft", new ClickHandler() {
            public void onClick (ClickEvent event) {
                selectGame((currentSelection + _games.length-1) % _games.length);
            }
        }));
        final int SCROLL_COUNT = 5;
        for (int ii = 0; ii < SCROLL_COUNT; ++ii) {
            if (ii == currentSelection) {
                scroller.add(MsoyUI.createImageButton("fbscrollSelected", null));
            } else if (ii < _games.length) {
                final int fii = ii;
                scroller.add(MsoyUI.createImageButton("fbscrollDeselected", new ClickHandler() {
                    public void onClick (ClickEvent event) {
                        selectGame(fii);
                    }
                }));
            }
        }
        scroller.add(MsoyUI.createImageButton("fbscrollRight", new ClickHandler() {
            public void onClick (ClickEvent event) {
                selectGame((currentSelection + 1) % _games.length);
            }
        }));
        return scroller;
    }

    protected static AbsoluteCSSPanel createGameBits (GameInfo game)
    {
        AbsoluteCSSPanel bits = new AbsoluteCSSPanel("bits", "fixed");
        bits.add(MsoyUI.createLabel(game.name, "Name"));
        bits.add(MsoyUI.createLabel(_dmsgs.xlate("genre_" + game.genre), "Genre"));
        bits.add(new CreatorLabel(game.creator));
        bits.add(new Stars(game.rating, true, false, null));
        if (game.playersOnline > 0) {
            bits.add(MsoyUI.createLabel(_msgs.featuredOnline(""+game.playersOnline), "Online"));
        }
        // TODO: make the description float around the play button (I played trial-and-error with
        // this for 45 minutes and nothing worked... time to move on!)
        bits.add(MsoyUI.createLabel(
            MsoyUI.truncateParagraph(game.description, 100), "Description"));
        bits.add(PlayButton.createCustom(game.gameId, "fbplayButton"));
        return bits;
    }

    protected GameInfo[] _games;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
