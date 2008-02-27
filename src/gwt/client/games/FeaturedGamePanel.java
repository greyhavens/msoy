//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.web.data.FeaturedGameInfo;

import client.shell.Application;
import client.shell.Page;
import client.shell.Args;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Displays a featured game.
 */
public class FeaturedGamePanel extends SmartTable
{
    public FeaturedGamePanel (FeaturedGameInfo[] games)
    {
        super("featuredGame", 0, 10);

        // TODO: allow next/prev
        final FeaturedGameInfo game = games[0];

        VerticalPanel left = new VerticalPanel();
        left.setHorizontalAlignment(HasAlignment.ALIGN_CENTER);
        left.add(MediaUtil.createMediaView(game.getShotMedia(), Game.SHOT_WIDTH, Game.SHOT_HEIGHT,
                                           new ClickListener() {
            public void onClick (Widget sender) {
                Application.go(Page.GAMES, Args.compose("d", game.gameId));
            }
        }));
        left.add(WidgetUtil.makeShim(5, 5));
        // TODO: add by Foozle
        left.add(new GameBitsPanel(null, game.genre, game.minPlayers, game.maxPlayers,
                                   game.avgDuration, 0));
        // TODO: add next, prev
        setWidget(0, 0, left);
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);

        VerticalPanel right = new VerticalPanel();
        right.add(MsoyUI.createLabel(game.name, "Name"));
        right.add(MsoyUI.createLabel(CGames.dmsgs.getString("genre" + game.genre), "Genre"));
        right.add(WidgetUtil.makeShim(5, 5));
        right.add(MsoyUI.createLabel(truncate(game.description), "Descrip"));
        right.add(MsoyUI.createLabel(CGames.msgs.featuredOnline(""+game.playersOnline), "Online"));
        right.add(WidgetUtil.makeShim(5, 10));
        right.add(new PlayPanel(game.gameId, game.minPlayers, game.maxPlayers));
        setWidget(0, 1, right);
        getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
    }

    protected static String truncate (String descrip)
    {
        return (descrip.length() <= MAX_DESCRIP_LENGTH) ? descrip :
            descrip.substring(0, MAX_DESCRIP_LENGTH-3) + "...";
    }

    protected static final int MAX_DESCRIP_LENGTH = 150;
}
