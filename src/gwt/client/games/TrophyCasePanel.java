//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.game.gwt.TrophyCase;

import client.shell.Args;
import client.shell.Page;
import client.ui.TongueBox;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

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
        _gamesvc.loadTrophyCase(CGames.ident, memberId, new MsoyCallback<TrophyCase>() {
            public void onSuccess (TrophyCase tc) {
                setTrophyCase(tc);
            }
        });
    }

    protected void setTrophyCase (TrophyCase tcase)
    {
        if (tcase == null) {
            setHeader(CGames.msgs.noSuchPlayer());
            return;
        }

        CGames.frame.setTitle(CGames.msgs.caseTitle(tcase.owner.toString()));
        if (tcase.shelves.length == 0) {
            setHeader((CGames.getMemberId() == tcase.owner.getMemberId()) ?
                     CGames.msgs.caseEmptyMe() : CGames.msgs.caseEmpty());
            return;
        }

        setHeader(CGames.msgs.caseBlurb());
        for (int ii = 0; ii < tcase.shelves.length; ii++) {
            TrophyCase.Shelf shelf = tcase.shelves[ii];
            TongueBox box = new TongueBox(shelf.name, new TrophyGrid(shelf.trophies));
            int ownerId = tcase.owner.getMemberId();
            if (!CGames.isGuest() && CGames.getMemberId() != ownerId) {
                box.setFooterLink(CGames.msgs.caseCompare(),
                                  Page.GAMES, Args.compose("ct", ""+shelf.gameId, ""+ownerId));
            }
            add(box);
        }
    }

    protected void setHeader (String title)
    {
        clear();
        add(new TongueBox(null, title, false));
    }

    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
