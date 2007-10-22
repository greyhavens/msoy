//
// $Id$

package client.game;

import java.util.List;
import java.util.Date;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.MediaDesc;

import client.util.MediaUtil;

/**
 * Displays the trophies 
 */
public class GameTrophyPanel extends PagedGrid
{
    public GameTrophyPanel (int gameId)
    {
        super(5, 2, NAV_ON_BOTTOM);
        _gameId = gameId;
        addStyleName("gameTrophyPanel");
        addStyleName("dottedGrid");
        add(new Label(CGame.msgs.gameTrophyLoading()));
        setCellAlignment(ALIGN_LEFT, ALIGN_TOP);
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
                setModel(new SimpleDataModel((List)result), 0);
            }
            public void onFailure (Throwable caught) {
                CGame.log("loadGameTrophies failed", caught);
                add(new Label(CGame.serverError(caught)));
            }
        });
        _gameId = 0; // note that we've asked for our data
    }

    // @Override // from PagedGrid
    protected Widget createWidget (Object item)
    {
        return new TrophyDetail((Trophy)item);
    }

    // @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return CGame.msgs.gameTrophyNoTrophies();
    }

    // @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return (items > _rows * _cols);
    }

    protected class TrophyDetail extends FlexTable
    {
        public TrophyDetail (Trophy trophy) {
            setCellSpacing(0);
            setCellPadding(0);
            setStyleName("trophyDetail");
            setWidget(0, 0, MediaUtil.createMediaView(
                          trophy.trophyMedia, MediaDesc.THUMBNAIL_SIZE));
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
}
