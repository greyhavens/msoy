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
    public FeaturedGamePanel (FeaturedGameInfo game)
    {
        super("featuredGame", 0, 10);

        VerticalPanel left = new VerticalPanel();
        left.add(MediaUtil.createMediaView(
                     game.getShotMedia(), Game.SHOT_WIDTH, Game.SHOT_HEIGHT, null)); // TODO: link
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

//         // display our screenshot in column 1
//         contents.setText(0, 0, game.name, 1, "Name");
//         Widget link = Application.createLink(
//             CGames.msgs.featuredMoreInfo(), Page.GAMES, Args.compose("d", game.gameId));
//         contents.setWidget(2, 0, link, 1, "MoreInfo");

//         // display the game info in column 2
//         contents.setText(0, 1, 
//         SmartTable info = new SmartTable(0, 0);
//         info.addText(truncate(game.description), 1, "Descrip");
//         info.addWidget(WidgetUtil.makeShim(5, 5), 1, null);
//         info.addWidget(new GameBitsPanel(null, game.genre, game.minPlayers,
//                                          game.maxPlayers, game.avgDuration, 0), 1, null);
//         contents.setWidget(1, 1, info, 1, "Info");
//         contents.getFlexCellFormatter().setRowSpan(1, 1, 2);

//         // display play now buttons in column 3
//         contents.setText(0, 2, CGames.msgs.gdpPlay(), 1, "Play");
//         PlayPanel play = new PlayPanel(false, game.gameId, game.minPlayers, game.maxPlayers);
//         contents.setWidget(1, 2, play, 1, "Buttons");
//         contents.getFlexCellFormatter().setRowSpan(1, 2, 2);
    }

    protected static String truncate (String descrip)
    {
        return (descrip.length() <= MAX_DESCRIP_LENGTH) ? descrip :
            descrip.substring(0, MAX_DESCRIP_LENGTH-3) + "...";
    }

    protected static final int MAX_DESCRIP_LENGTH = 150;
}
