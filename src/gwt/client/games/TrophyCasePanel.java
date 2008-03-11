//
// $Id$

package client.games;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.web.data.TrophyCase;

import client.shell.Frame;
import client.util.TongueBox;

/**
 * Displays all trophies owned by the specified player.
 */
public class TrophyCasePanel extends VerticalPanel
{
    public TrophyCasePanel (int memberId)
    {
        setStyleName("trophyCase");

        if (memberId <= 0) {
            setHeader(CGames.msgs.noSuchPlayer());
            return;
        }

        setHeader(CGames.msgs.caseLoading());
        CGames.gamesvc.loadTrophyCase(CGames.ident, memberId, new AsyncCallback() {
            public void onSuccess (Object result) {
                setTrophyCase((TrophyCase)result);
            }
            public void onFailure (Throwable cause) {
                setHeader(CGames.serverError(cause));
            }
        });
    }

    protected void setTrophyCase (TrophyCase tcase)
    {
        if (tcase == null) {
            setHeader(CGames.msgs.noSuchPlayer());
            return;
        }

        Frame.setTitle(CGames.msgs.caseTitle(tcase.owner.toString()));
        if (tcase.shelves.length == 0) {
            setHeader((CGames.getMemberId() == tcase.owner.getMemberId()) ?
                     CGames.msgs.caseEmptyMe() : CGames.msgs.caseEmpty());
            return;
        }

        setHeader(CGames.msgs.caseBlurb());
        for (int ii = 0; ii < tcase.shelves.length; ii++) {
            TrophyCase.Shelf shelf = tcase.shelves[ii];
            add(new TongueBox(shelf.name, new TrophyGrid(shelf.trophies)));
        }
    }

    protected void setHeader (String title)
    {
        clear();
        add(new TongueBox(null, title, false));
    }
}
