//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.web.data.FeaturedGameInfo;

import client.shell.Application;
import client.shell.Page;
import client.shell.Args;
import client.util.MediaUtil;

/**
 * Displays a featured game.
 */
public class FeaturedGamePanel extends SmartTable
{
    public FeaturedGamePanel (FeaturedGameInfo game)
    {
        super("featuredGame", 0, 0);

        SmartTable title = new SmartTable("Title", 0, 0);
        title.getFlexCellFormatter().setStyleName(0, 0, "Star");
        title.setText(0, 1, CGame.msgs.featuredTitle(game.name), 1, "Text");
        title.getFlexCellFormatter().setStyleName(0, 2, "Star");
        setWidget(0, 0, title, 3, null);

        // display our screenshot in column 1
        FlowPanel shot = new FlowPanel();
        shot.add(MediaUtil.createMediaView(
                     game.getShotMedia(), Game.SHOT_WIDTH, Game.SHOT_HEIGHT, null));
        shot.add(Application.createLink(CGame.msgs.arcadeMoreInfo(), Page.GAMES,
                                        Args.compose("d", game.gameId)));
        setWidget(1, 0, shot, 1, "Shot");

        // display the game info in column 2
        int row = 0;
        SmartTable info = new SmartTable(0, 0);
        info.setText(row++, 0, CGame.msgs.featuredOnline(""+game.playersOnline), 2, "Online");
        info.setWidget(row++, 0, WidgetUtil.makeShim(5, 5));
        info.setText(row++, 0, truncate(game.description), 2, "Descrip");
        info.setWidget(row++, 0, WidgetUtil.makeShim(5, 5));
        info.setWidget(row++, 0, new GameBitsPanel(null, game.genre, game.minPlayers,
                                                   game.maxPlayers, game.avgDuration, 0));
        setWidget(1, 1, info, 1, "Info");

        // display play now buttons in column 3
        setWidget(1, 2, new PlayPanel(game.gameId, game.minPlayers, game.maxPlayers), 1, "Buttons");
    }

    protected static String truncate (String descrip)
    {
        return (descrip.length() <= MAX_DESCRIP_LENGTH) ? descrip :
            descrip.substring(0, MAX_DESCRIP_LENGTH-3) + "...";
    }

    protected static final int MAX_DESCRIP_LENGTH = 100;
}
