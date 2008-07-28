//
// $Id$

package client.games;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.data.all.Trophy;

import client.shell.Page;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.MediaUtil;
import client.util.NaviUtil;

/**
 * Displays a grid of trophies, each of which link to their respective game's detail page.
 */
public class TrophyGrid extends FlexTable
{
    public static final int COLUMNS = 6;

    public static void populateTrophyGrid (FlexTable grid, Trophy[] trophies)
    {
        grid.addStyleName("trophyGrid");

        int row = 0;
        for (int ii = 0; ii < trophies.length; ii++) {
            if (ii > 0 && ii % COLUMNS == 0) {
                row += 2;
            }
            int col = ii % COLUMNS;

            final Trophy trophy = trophies[ii];
            ClickListener trophyClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Page.GAMES, NaviUtil.gameDetail(
                                trophy.gameId, NaviUtil.GameDetails.TROPHIES));
                }
            };

            grid.setWidget(row, col, MediaUtil.createMediaView(
                               trophy.trophyMedia, MediaDesc.THUMBNAIL_SIZE, trophyClick));
            grid.getFlexCellFormatter().setStyleName(row, col, "Trophy");
            grid.setWidget(row+1, col, MsoyUI.createActionLabel(trophy.name, trophyClick));
            grid.getFlexCellFormatter().setStyleName(row+1, col, "Name");
        }

        // if they have only one or two trophies total, we need to add blank cells to ensure that
        // things are only as wide as appropriate
        for (int ii = trophies.length; ii < COLUMNS; ii++) {
            grid.setText(0, ii, "");
            grid.getFlexCellFormatter().setStyleName(0, ii, "Trophy");
        }
    }

    public TrophyGrid (Trophy[] trophies)
    {
        setCellSpacing(5);
        setCellPadding(0);
        populateTrophyGrid(this, trophies);
    }
}
