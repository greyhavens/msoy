//
// $Id$

package client.game;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.data.PlayerRating;

import client.shell.Application;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyUI;

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

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _gameId == 0) {
            return;
        }

        // it's possible to have this tab shown and be a guest; so we avoid freakoutage
        if (_onlyMyFriends && (CGame.getMemberId() == 0)) {
            addNote(CGame.msgs.trpLogin());
            return;
        }

        addNote(CGame.msgs.trpLoading());
        CGame.gamesvc.loadTopRanked(CGame.ident, _gameId, _onlyMyFriends, new AsyncCallback() {
            public void onSuccess (Object result) {
                gotRankings((PlayerRating[][])result);
            }
            public void onFailure (Throwable caught) {
                CGame.log("getTopRanked failed", caught);
                addNote(CGame.serverError(caught));
            }
        });
        _gameId = 0; // note that we've asked for our data
    }

    protected void gotRankings (PlayerRating[][] results)
    {
        clear();

        int totalRows = Math.max(results[0].length, results[1].length);
        if (totalRows == 0) {
            addNote(_onlyMyFriends ? CGame.msgs.trpMyNoRankings() :
                      CGame.msgs.trpTopNoRankings());
            return;
        }

        add(_grid = new FlexTable());
        _grid.setStyleName("Grid");
        _grid.setCellSpacing(0);
        _grid.setCellPadding(3);

        int col = 0;
        if (results[0].length > 0) {
            displayRankings(CGame.msgs.trpSingleHeader(), col, results[0], totalRows);
            col += COLUMNS;
        }
        if (results[1].length > 0) {
            displayRankings(CGame.msgs.trpMultiHeader(), col, results[1], totalRows);
            col += COLUMNS;
        }

        String text = _onlyMyFriends ? CGame.msgs.trpSingleTip() : CGame.msgs.trpMultiTip();
        add(MsoyUI.createLabel(text, "Footer"));
    }

    protected void displayRankings (String header, int col, PlayerRating[] results, int totalRows)
    {
        for (int cc = 0; cc < COLUMNS; cc++) {
            int hcol = (cc + col);
            switch (cc) {
            case 2: _grid.setText(0, hcol, header); break;
            case 3: _grid.setText(0, hcol, CGame.msgs.trpRatingHeader()); break;
            default: _grid.setHTML(0, hcol, "&nbsp;"); break;
            }
            _grid.getFlexCellFormatter().setStyleName(0, hcol, (cc == 3) ? "HeaderTip" : "Header");
        }

        for (int ii = 0; ii < totalRows; ii++) {
            int row = 1 + ii*2;
            if (ii >= results.length) {
                for (int cc = 0; cc < COLUMNS; cc++) {
                    _grid.setHTML(row, cc+col, "&nbsp;");
                    _grid.getFlexCellFormatter().setStyleName(row, cc+col, "Cell");
                }
                continue;
            }

            final PlayerRating rating = results[ii];
            _grid.setText(row, col, CGame.msgs.gameRank("" + (ii+1)));
            _grid.getFlexCellFormatter().setStyleName(row, col, "Cell");
            _grid.getFlexCellFormatter().setHorizontalAlignment(row, col, HasAlignment.ALIGN_RIGHT);

            ClickListener onClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.PROFILE, "" + rating.name.getMemberId());
                }
            };
            Widget photo = MediaUtil.createMediaView(
                rating.photo, MediaDesc.QUARTER_THUMBNAIL_SIZE);
            if (photo instanceof Image) {
                ((Image) photo).addClickListener(onClick);
                photo.setStyleName("actionLabel");
            }
            _grid.setWidget(row, col+1, photo);
            _grid.getFlexCellFormatter().setStyleName(row, col+1, "Cell");
            _grid.getFlexCellFormatter().addStyleName(row, col+1, "Photo");
            _grid.getFlexCellFormatter().setHorizontalAlignment(
                row, col+1, HasAlignment.ALIGN_CENTER);
            _grid.getFlexCellFormatter().setVerticalAlignment(
                row, col+1, HasAlignment.ALIGN_MIDDLE);

            _grid.setWidget(row, col+2, MsoyUI.createActionLabel(rating.name.toString(), onClick));
            _grid.getFlexCellFormatter().setStyleName(row, col+2, "Cell");

            _grid.setText(row, col+3, ""+rating.rating);
            _grid.getFlexCellFormatter().setStyleName(row, col+3, "Cell");
            _grid.getFlexCellFormatter().setHorizontalAlignment(
                row, col+3, HasAlignment.ALIGN_RIGHT);

            _grid.setHTML(row, col+4, "&nbsp;");
            _grid.getFlexCellFormatter().setStyleName(row, col+4, "Cell");
            _grid.getFlexCellFormatter().addStyleName(row, col+4, "Gap");

            if (rating.name.getMemberId() == CGame.getMemberId()) {
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
    protected FlexTable _grid;

    protected static final int COLUMNS = 5;
}
