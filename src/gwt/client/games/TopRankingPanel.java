//
// $Id$

package client.games;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;
import com.threerings.msoy.game.gwt.PlayerRating;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.ThumbBox;
import client.util.Link;
import client.util.ServiceUtil;

/**
 * Displays top-rankings for a particular game.
 */
public class TopRankingPanel extends VerticalPanel
{
    public TopRankingPanel (int gameId, boolean onlyMyFriends)
    {
        setStyleName("topRankingPanel");
        _gameId = gameId;
        _onlyMyFriends = onlyMyFriends;
    }

    @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _gameId == 0) {
            return;
        }

        // it's possible to have this tab shown and be a guest; so we avoid freakoutage
        if (_onlyMyFriends && CShell.isGuest()) {
            addNote(_msgs.trpLogon());
            return;
        }

        addNote(_msgs.trpLoading());
        _gamesvc.loadTopRanked(_gameId, _onlyMyFriends, new AsyncCallback<PlayerRating[][]>() {
            public void onSuccess (PlayerRating[][] topRanked) {
                gotRankings(topRanked);
            }
            public void onFailure (Throwable caught) {
                CShell.log("getTopRanked failed", caught);
                addNote(CShell.serverError(caught));
            }
        });
        _gameId = 0; // note that we've asked for our data
    }

    protected void gotRankings (PlayerRating[][] results)
    {
        clear();

        int totalRows = Math.max(results[0].length, results[1].length);
        if (totalRows == 0) {
            addNote(_onlyMyFriends ? _msgs.trpMyNoRankings() : _msgs.trpTopNoRankings());
            return;
        }

        add(_grid = new SmartTable("Grid", 3, 0));

        int col = 0;
        if (results[0].length > 0) {
            displayRankings(_msgs.trpSingleHeader(), col, results[0], totalRows);
            col += COLUMNS;
        }
        if (results[1].length > 0) {
            displayRankings(_msgs.trpMultiHeader(), col, results[1], totalRows);
            col += COLUMNS;
        }

        String text = _onlyMyFriends ? _msgs.trpSingleTip() : _msgs.trpMultiTip();
        add(MsoyUI.createLabel(text, "Footer"));
    }

    protected void displayRankings (String header, int col, PlayerRating[] results, int totalRows)
    {
        for (int cc = 0; cc < COLUMNS; cc++) {
            int hcol = (cc + col);
            switch (cc) {
            case 2: _grid.setText(0, hcol, header); break;
            case 3: _grid.setText(0, hcol, _msgs.trpRatingHeader()); break;
            default: _grid.setHTML(0, hcol, "&nbsp;"); break;
            }
            _grid.getFlexCellFormatter().setStyleName(0, hcol, (cc == 3) ? "HeaderTip" : "Header");
        }

        for (int ii = 0; ii < totalRows; ii++) {
            final PlayerRating rating = results[ii];
            int row = 1 + ii*2;
            if (ii >= results.length || rating.name == null) {
                for (int cc = 0; cc < COLUMNS; cc++) {
                    _grid.setHTML(row, cc+col, "&nbsp;", 1, "Cell");
                }
                continue;
            }

            _grid.setText(row, col, _msgs.gameRank("" + (ii+1)), 1, "Cell");
            _grid.getFlexCellFormatter().setHorizontalAlignment(row, col, HasAlignment.ALIGN_RIGHT);

            ThumbBox box = new ThumbBox(rating.photo, MediaDesc.QUARTER_THUMBNAIL_SIZE);
            _grid.setWidget(row, col+1, box, 1, "Cell");
            _grid.getFlexCellFormatter().addStyleName(row, col+1, "Photo");
            _grid.getFlexCellFormatter().setHorizontalAlignment(
                row, col+1, HasAlignment.ALIGN_CENTER);
            _grid.getFlexCellFormatter().setVerticalAlignment(
                row, col+1, HasAlignment.ALIGN_MIDDLE);

            _grid.setWidget(row, col+2, Link.memberView(rating.name), 1, "Cell");

            _grid.setText(row, col+3, ""+rating.rating, 1, "Cell");
            _grid.getFlexCellFormatter().setHorizontalAlignment(
                row, col+3, HasAlignment.ALIGN_RIGHT);

            _grid.setHTML(row, col+4, "&nbsp;", 1, "Cell");
            _grid.getFlexCellFormatter().addStyleName(row, col+4, "Gap");

            if (rating.name.getMemberId() == CShell.getMemberId()) {
                for (int cc = 0; cc < COLUMNS; cc++) {
                    _grid.getFlexCellFormatter().addStyleName(row, col+cc, "Self");
                }
            }
        }
    }

    protected void addNote (String text)
    {
        add(MsoyUI.createLabel(text, "Note"));
    }

    protected int _gameId;
    protected boolean _onlyMyFriends;
    protected SmartTable _grid;

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = (GameServiceAsync)
        ServiceUtil.bind(GWT.create(GameService.class), GameService.ENTRY_POINT);

    protected static final int COLUMNS = 5;
}
