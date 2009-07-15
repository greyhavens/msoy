//
// $Id: FBFeaturedGamePanel.java 17545 2009-07-14 22:55:09Z jamie $

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.gwt.ui.FloatPanel;
import com.threerings.msoy.game.gwt.MochiGameInfo;
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
public class FBMochiGamePanel extends AbsoluteCSSPanel
{
    public FBMochiGamePanel (MochiGameInfo[] games, FBArcadePanel panel)
    {
        super("fbfeaturedGame", "fixed");
        _panel = panel;
        _games = games;
        if (games.length > 0) {
            selectGame(0);
        }
    }

    protected void selectGame (int index)
    {
        clear();
        MochiGameInfo game = _games[index];
        add(MsoyUI.createLabel("Daily Games", "title"));

        SimplePanel thumbnail = new SimplePanel();
        thumbnail.addStyleName("thumbBox");
        DOM.setStyleAttribute(thumbnail.getElement(), "overflow", "hidden");
        thumbnail.add(new Image(game.thumbURL));

        add(thumbnail);
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

    protected AbsoluteCSSPanel createGameBits (final MochiGameInfo game)
    {
        AbsoluteCSSPanel bits = new AbsoluteCSSPanel("bits", "fixed");
        bits.add(MsoyUI.createLabel(game.name, "Name"));
        bits.add(MsoyUI.createLabel(game.categories, "Genre"));

        FlowPanel cpan = new FlowPanel();
        cpan.addStyleName("creator");
        cpan.add(new InlineLabel(game.author));
        bits.add(cpan);

//        bits.add(new Stars(game.rating, true, false, null));
//        if (game.playersOnline > 0) {
//            bits.add(MsoyUI.createLabel(_msgs.featuredOnline(""+game.playersOnline), "Online"));
//        }
        // TODO: make the description float around the play button (I played trial-and-error with
        // this for 45 minutes and nothing worked... time to move on!)
        bits.add(MsoyUI.createLabel(
            MsoyUI.truncateParagraph(game.desc, 100), "Description"));

        PushButton play = new PushButton();
        play.setStyleName("fbplayButton");
        play.addClickHandler(new ClickHandler() {
            public void onClick (ClickEvent event) {
                _panel.playMochiGame(game);
            }
        });
        bits.add(play);

        return bits;
    }

    protected FBArcadePanel _panel;

    protected MochiGameInfo[] _games;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
