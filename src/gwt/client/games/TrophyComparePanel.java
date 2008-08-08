//
// $Id$

package client.games;

import java.util.Date;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.item.data.all.TrophySource;
import com.threerings.msoy.web.data.MemberCard;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Displays a side-by-side comparison of the caller's trophies in a particular game with another
 * member's.
 */
public class TrophyComparePanel extends SmartTable
{
    public TrophyComparePanel (final int gameId, int targetId)
    {
        super("trophyCompare", 0, 10);
        setText(0, 0, _msgs.compareLoading());

        int[] memberIds = new int[] { targetId, CShell.getMemberId() };
        _gamesvc.compareTrophies(gameId, memberIds, new MsoyCallback<GameService.CompareResult>() {
            public void onSuccess (GameService.CompareResult result) {
                init(gameId, result);
            }
        });
    }

    protected void init (int gameId, GameService.CompareResult result)
    {
        if (result == null) {
            setText(0, 0, _msgs.compareNoSuchGame());
            return;
        }

        ClickListener onClick = Link.createListener(
            Pages.GAMES, Args.compose("d", gameId));
        setWidget(0, 0, new ThumbBox(result.gameThumb, onClick));
        setWidget(1, 0, MsoyUI.createActionLabel(result.gameName, "Game", onClick));
        centerCell(0, 0);
        centerCell(1, 0);

        for (int pp = 0; pp < result.members.length; pp++) {
            MemberCard card = result.members[pp];
            if (card == null) {
                continue;
            }
            setWidget(0, pp+1, new ThumbBox(card.photo, Link.createListener(
                                                Pages.PEOPLE, "" + card.name.getMemberId())));
            centerCell(0, pp+1);
            setWidget(1, pp+1, Link.memberView(card.name));
            centerCell(1, pp+1);
        }

        for (int ii = 0; ii < result.trophies.length; ii++) {
            Trophy trophy = result.trophies[ii];
            int row = 2+3*ii+1;

            setText(row-1, 0, "", 3, "Row");

            FlowPanel tname = new FlowPanel();
            tname.add(MsoyUI.createLabel(trophy.name, "Trophy"));
            if (trophy.description == null) {
                tname.add(MsoyUI.createLabel(_msgs.gameTrophySecret(), "Italic"));
            } else {
                tname.add(MsoyUI.createLabel(trophy.description, null));
            }
            setWidget(row, 0, tname, 1, "Descrip");
            getFlexCellFormatter().setRowSpan(row, 0, 2);
            // centerCell(row, 0);

            for (int pp = 0; pp < result.members.length; pp++) {
                Long earned = result.whenEarneds[pp][ii];
                String info;
                if (earned != null) {
                    setWidget(row, pp+1, new ThumbBox(trophy.trophyMedia, TrophySource.TROPHY_WIDTH,
                                                      TrophySource.TROPHY_HEIGHT, null));
                    info = _pfmt.format(new Date(earned.longValue()));
                } else {
                    info = _msgs.compareUnearned();
                    getFlexCellFormatter().addStyleName(row+1, pp, "Italic");
                }
                setText(row+1, pp, info, 1, "Earned");
                centerCell(row, pp+1);
                centerCell(row+1, pp);
            }
        }
    }

    protected void centerCell (int row, int col)
    {
        getFlexCellFormatter().setHorizontalAlignment(row, col, HasAlignment.ALIGN_CENTER);
        getFlexCellFormatter().setVerticalAlignment(row, col, HasAlignment.ALIGN_MIDDLE);
    }

    protected static final SimpleDateFormat _pfmt = new SimpleDateFormat("MMM dd, yyyy");
    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);
}
