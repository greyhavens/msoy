//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.web.data.FeaturedGameInfo;

import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyUI;
import client.util.ThumbBox;

/**
 * Displays a featured game.
 */
public class FeaturedGamePanel extends SmartTable
{
    public FeaturedGamePanel (FeaturedGameInfo[] games)
    {
        super("featuredGame", 0, 10);
        _games = games;
        selectGame(0);
    }

    protected void selectGame (final int index)
    {
        final FeaturedGameInfo game = _games[index];

        ClickListener onClick = new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.GAMES, Args.compose("d", game.gameId));
            }
        };

        VerticalPanel left = new VerticalPanel();
        left.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        FlowPanel sashBox = new FlowPanel();
        sashBox.setStyleName("SashBox");
        Image sash = MsoyUI.createActionImage("/images/game/featured_banner.png", onClick);
        sash.addStyleName("Sash");
        sashBox.add(sash);
        sashBox.add(new ThumbBox(game.getShotMedia(), Game.SHOT_WIDTH, Game.SHOT_HEIGHT, onClick));
        left.add(sashBox);
        if (game.playersOnline > 0) {
            left.add(WidgetUtil.makeShim(5, 5));
            left.add(MsoyUI.createLabel(
                         CGames.msgs.featuredOnline(""+game.playersOnline), "Online"));
        }
        left.add(WidgetUtil.makeShim(5, 5));
        left.add(new GameBitsPanel(game.minPlayers, game.maxPlayers, game.avgDuration, 0));

        if (_games.length > 1) {
            left.add(WidgetUtil.makeShim(10, 10));
            left.add(MsoyUI.createPrevNextButtons(new ClickListener() {
                public void onClick (Widget sender) {
                    selectGame((index+_games.length-1)%_games.length);
                }
            }, new ClickListener() {
                public void onClick (Widget sender) {
                    selectGame((index+1)%_games.length);
                }
            }));
        }
        setWidget(0, 0, left);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        getFlexCellFormatter().setWidth(0, 0, Game.SHOT_WIDTH + "px");

        VerticalPanel right = new GameNamePanel(
            game.name, game.genre, game.creator, game.description);
        right.add(WidgetUtil.makeShim(5, 10));
        right.add(new PlayPanel(game.gameId, game.minPlayers, game.maxPlayers));
        setWidget(0, 1, right);
        getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
    }

    protected FeaturedGameInfo[] _games;
}
