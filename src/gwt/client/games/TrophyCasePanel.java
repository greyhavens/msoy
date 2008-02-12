//
// $Id$

package client.games;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.web.data.TrophyCase;

import client.shell.Frame;
import client.util.HeaderBox;

/**
 * Displays all trophies owned by the specified player.
 */
public class TrophyCasePanel extends SmartTable
{
    public TrophyCasePanel (int memberId)
    {
        super("trophyCase", 0, 5);
        Frame.setTitle(CGames.msgs.caseTitle());

        if (memberId == 0) {
            setText(0, 0, CGames.msgs.noSuchPlayer());
            return;
        }

        setText(0, 0, CGames.msgs.caseLoading());
        CGames.gamesvc.loadTrophyCase(CGames.ident, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                setTrophyCase((TrophyCase)result);
            }
            public void onFailure (Throwable cause) {
                setText(0, 0, CGames.serverError(cause));
            }
        });
    }

    protected void setTrophyCase (TrophyCase tcase)
    {
        if (tcase == null) {
            setText(0, 0, CGames.msgs.noSuchPlayer());
            return;
        }

        Frame.setTitle(CGames.msgs.caseTitle(), tcase.owner.toString());
        if (tcase.shelves.length == 0) {
            setText(0, 0, (CGames.getMemberId() == tcase.owner.getMemberId()) ?
                    CGames.msgs.caseEmptyMe() : CGames.msgs.caseEmpty());
            return;
        }

        setText(0, 0, CGames.msgs.caseBlurb(), 2, null);

        for (int ii = 0; ii < tcase.shelves.length; ii++) {
            TrophyCase.Shelf shelf = tcase.shelves[ii];
            HeaderBox box = new HeaderBox();
            int row = ii/2+1, col = ii%2;
            setWidget(row, col, box);
            getFlexCellFormatter().setVerticalAlignment(row, col, HasAlignment.ALIGN_TOP);
            box.setTitle(shelf.name);
            box.setContent(new TrophyGrid(shelf.trophies));
        }
    }
}
