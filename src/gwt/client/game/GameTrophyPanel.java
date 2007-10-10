//
// $Id$

package client.game;

import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.MediaUtil;

/**
 * Displays the trophies 
 */
public class GameTrophyPanel extends FlexTable
{
    public GameTrophyPanel (int gameId)
    {
        _gameId = gameId;
        setCellPadding(0);
        setCellSpacing(5);
        setText(0, 0, CGame.msgs.gameTrophyLoading());
    }

    // @Override // from UIObject
    public void setVisible (boolean visible)
    {
        super.setVisible(visible);
        if (!visible || _gameId == 0) {
            return;
        }

        CGame.gamesvc.loadGameTrophies(CGame.ident, _gameId, new AsyncCallback() {
            public void onSuccess (Object result) {
                gotTrophies((Trophy[])result);
            }
            public void onFailure (Throwable caught) {
                CGame.log("loadGameTrophies failed", caught);
                setText(0, 0, CGame.serverError(caught));
            }
        });
        _gameId = 0; // note that we've asked for our data
    }

    protected void gotTrophies (Trophy[] trophies)
    {
        if (trophies == null || trophies.length == 0) {
            setText(0, 0, CGame.msgs.gameTrophyNoTrophies());
        } else {
            for (int ii = 0; ii < trophies.length; ii++) {
                setWidget(ii/COLUMNS, ii%COLUMNS, new TrophyDetail(trophies[ii]));
            }
        }
    }

    protected class TrophyDetail extends FlexTable
    {
        public TrophyDetail (Trophy trophy) {
            setCellSpacing(0);
            setCellPadding(0);
            setStyleName("trophyDetail");
            setWidget(0, 0, MediaUtil.createMediaView(
                          trophy.trophyMedia, MediaDesc.HALF_THUMBNAIL_SIZE));
            getFlexCellFormatter().setStyleName(0, 0, "Image");

            setText(0, 1, trophy.name);
            getFlexCellFormatter().setStyleName(0, 1, "Name");

            if (trophy.description == null) {
                setText(1, 0, CGame.msgs.gameTrophySecret());
                getFlexCellFormatter().setStyleName(1, 0, "Italic");
            } else {
                setText(1, 0, trophy.description);
            }
            getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);

            if (CGame.getMemberId() != 0 && trophy.whenEarned != null) {
                setText(2, 0, CGame.msgs.gameTrophyEarnedOn(
                            _pfmt.format(new Date(trophy.whenEarned.longValue()))));
                getFlexCellFormatter().setStyleName(2, 0, "Italic");
            }
            getFlexCellFormatter().setRowSpan(0, 0, getRowCount());
        }
    }

    protected int _gameId;

    protected static SimpleDateFormat _pfmt = new SimpleDateFormat("MMM dd, yyyy");

    protected static final int COLUMNS = 2;
}
