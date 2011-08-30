//
// $Id: FBFeaturedGamePanel.java 17545 2009-07-14 22:55:09Z jamie $

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.InlineLabel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.AbsoluteCSSPanel;
import com.threerings.gwt.ui.FloatPanel;

import com.threerings.msoy.game.gwt.MochiGameInfo;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.Link;

/**
 * A "temporary" copy of FBFeaturedGamePanel that displays mochi games instead of
 * featured games.
 * TODO TEMP TODO
 * If you are making changes:
 * - do you need to change the other file?
 * - why is this here still in use?
 * We should integrate mochi games more fully, not continue to hack this. Talk to Ray.
 */
public class FBMochiGamePanel extends AbsoluteCSSPanel
{
    public FBMochiGamePanel (MochiGameInfo[] games)
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
        MochiGameInfo game = _games[index];
        add(MsoyUI.createLabel("Daily Games", "title"));

        AbsoluteCSSPanel thumbnail = new AbsoluteCSSPanel("thumbBox", "fixed");
        // mochi images are 100x100, or smaller. Crap
        Image icon = new Image(game.thumbURL);
        DOM.setStyleAttribute(icon.getElement(), "left", (75/2) + "px");
        DOM.setStyleAttribute(icon.getElement(), "top", (25/2) + "px");
        thumbnail.add(icon);

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
        //bits.add(MsoyUI.createLabel(game.categories, "Genre"));

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
        play.addClickHandler(Link.createHandler(Pages.GAMES, "mochi", game.tag));
        bits.add(play);

        return bits;
    }

    protected MochiGameInfo[] _games;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
