//
// $Id$

package client.game;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.TrophyCase;

import client.util.MediaUtil;

/**
 * Displays all trophies owned by the specified player.
 */
public class TrophyCasePanel extends FlexTable
{
    public TrophyCasePanel (int memberId)
    {
        setStyleName("trophyCase");
        setText(0, 0, "Loading trophies...");
        CGame.gamesvc.loadTrophyCase(CGame.ident, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                setTrophyCase((TrophyCase)result);
            }
            public void onFailure (Throwable cause) {
                setText(0, 0, CGame.serverError(cause));
            }
        });
    }

    // TODO: stylings
    protected void setTrophyCase (TrophyCase tcase)
    {
        int row = 0;
        for (int ii = 0; ii < tcase.shelves.length; ii++) {
            TrophyCase.Shelf shelf = tcase.shelves[ii];
            setText(row, 0, shelf.name);
            getFlexCellFormatter().setColSpan(row++, 0, COLUMNS);

            for (int tt = 0; tt < shelf.trophies.length; tt++) {
                if (tt > 0 && tt % COLUMNS == 0) {
                    row += 2;
                }
                int col = tt % COLUMNS;
                setWidget(row, col, MediaUtil.createMediaView(
                              shelf.trophies[tt].trophyMedia, MediaDesc.HALF_THUMBNAIL_SIZE));
                setText(row+1, col,  shelf.trophies[tt].name);
            }
        }
    }

    protected static final int COLUMNS = 6;
}
