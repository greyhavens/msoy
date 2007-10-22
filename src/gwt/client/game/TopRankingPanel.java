//
// $Id$

package client.game;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
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
            add(new Label("Log in to see your rankings."));
            return;
        }

        CGame.gamesvc.loadTopRanked(CGame.ident, _gameId, _onlyMyFriends, new AsyncCallback() {
            public void onSuccess (Object result) {
                gotRankings((PlayerRating[][])result);
            }
            public void onFailure (Throwable caught) {
                CGame.log("getTopRanked failed", caught);
                add(new Label(CGame.serverError(caught)));
            }
        });
        _gameId = 0; // note that we've asked for our data
    }

    protected void gotRankings (PlayerRating[][] results)
    {
        int totalRows = Math.max(results[0].length, results[1].length);
        if (totalRows == 0) {
            add(new Label(_onlyMyFriends ? "You and your friends have no rankings in this game." :
                          "No one is ranked in this game."));
            return;
        }

        add(_grid = new FlexTable());
        _grid.setStyleName("Grid");
        _grid.setCellSpacing(0);
        _grid.setCellPadding(3);
        _grid.setText(0, 0, "Loading ratings...");

        int col = 0;
        if (results[0].length > 0) {
            displayRankings("Single Player", col, results[0], totalRows);
            col += COLUMNS;
        }
        if (results[1].length > 0) {
            displayRankings("Multiplayer", col, results[1], totalRows);
            col += COLUMNS;
        }

        // add the footer
        String text = _onlyMyFriends ? "Top ranked players among you and your friends." :
            "Top ranked players in all the Whirled.";
        add(MsoyUI.createLabel(text, "Footer"));
    }

    protected void displayRankings (String header, int col, PlayerRating[] results, int totalRows)
    {
        int hcol = (col > 0 ? 3 : 0);
        _grid.setText(0, hcol, header);
        _grid.getFlexCellFormatter().setStyleName(0, hcol, "Header");
        _grid.getFlexCellFormatter().setColSpan(0, hcol, COLUMNS-2);
        _grid.setText(0, hcol+1, "Rating");
        _grid.getFlexCellFormatter().setStyleName(0, hcol+1, "HeaderTip");
        _grid.setHTML(0, hcol+2, "&nbsp;");
        _grid.getFlexCellFormatter().setStyleName(0, hcol+2, "Header");

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
        }
    }

    protected int _gameId;
    protected boolean _onlyMyFriends;
    protected FlexTable _grid;

    protected static final int COLUMNS = 5;
}
