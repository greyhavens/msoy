//
// $Id$

package client.game;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.msoy.web.data.TrophyCase;

import client.shell.Frame;
import client.util.HeaderBox;

/**
 * Displays all trophies owned by the specified player.
 */
public class TrophyCasePanel extends FlexTable
{
    public TrophyCasePanel (int memberId)
    {
        Frame.setTitle(CGame.msgs.caseTitle());
        setCellPadding(0);
        setCellSpacing(5);

        setText(0, 0, CGame.msgs.caseLoading());
        CGame.gamesvc.loadTrophyCase(CGame.ident, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                setTrophyCase((TrophyCase)result);
            }
            public void onFailure (Throwable cause) {
                setText(0, 0, CGame.serverError(cause));
            }
        });
    }

    protected void setTrophyCase (TrophyCase tcase)
    {
        Frame.setTitle(CGame.msgs.caseTitle(), tcase.owner.toString());

        if (tcase.shelves.length == 0) {
            setText(0, 0, CGame.msgs.caseEmpty());
            return;
        }

        for (int ii = 0; ii < tcase.shelves.length; ii++) {
            TrophyCase.Shelf shelf = tcase.shelves[ii];
            HeaderBox box = new HeaderBox();
            int row = ii/2, col = ii%2;
            setWidget(row, col, box);
            getFlexCellFormatter().setVerticalAlignment(row, col, HasAlignment.ALIGN_TOP);
            box.setTitle(shelf.name);
            box.setContent(new TrophyGrid(shelf.trophies));
        }
    }
}
