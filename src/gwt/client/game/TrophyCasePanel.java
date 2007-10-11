//
// $Id$

package client.game;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;

import com.threerings.msoy.web.data.TrophyCase;

import client.shell.Page;
import client.util.HeaderBox;

/**
 * Displays all trophies owned by the specified player.
 */
public class TrophyCasePanel extends FlexTable
{
    public TrophyCasePanel (Page parent, int memberId)
    {
        _parent = parent;
        setCellPadding(0);
        setCellSpacing(10);

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
        _parent.setPageTitle(CGame.msgs.caseTitle(tcase.owner.toString()));

        if (tcase.shelves.length == 0) {
            setText(0, 0, CGame.msgs.caseEmpty());
            return;
        }

        for (int ii = 0; ii < tcase.shelves.length; ii++) {
            TrophyCase.Shelf shelf = tcase.shelves[ii];
            HeaderBox box = new HeaderBox();
            setWidget(ii/2, ii%2, box);
            box.setTitle(shelf.name);
            box.setContent(new TrophyGrid(shelf.trophies));
        }
    }

    protected Page _parent;
}
