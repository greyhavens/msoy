//
// $Id$

package client.games;

import java.util.Date;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DateUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.orth.data.MediaDescSize;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.game.gwt.GameService;
import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.shell.CShell;
import client.ui.MiniNowLoadingWidget;
import client.util.MediaUtil;

/**
 * Displays the trophies.
 */
public class GameTrophyPanel extends PagedGrid<Trophy>
{
    public GameTrophyPanel (int gameId)
    {
        super(50, 2, NAV_ON_BOTTOM);
        addStyleName("gameTrophyPanel");
        addStyleName("dottedGrid");
        setCellAlignment(HasAlignment.ALIGN_LEFT, HasAlignment.ALIGN_TOP);

        _gamesvc.loadGameTrophies(gameId, new AsyncCallback<List<Trophy>>() {
            public void onSuccess (List<Trophy> result) {
                setModel(new SimpleDataModel<Trophy>(result), 0);
            }
            public void onFailure (Throwable caught) {
                CShell.log("loadGameTrophies failed", caught);
                clear();
                add(new Label(CShell.serverError(caught)));
            }
        });
    }

    @Override // from PagedGrid
    protected Widget createWidget (Trophy item)
    {
        return new TrophyDetail(item);
    }

    @Override // from PagedGrid
    protected String getEmptyMessage ()
    {
        return _msgs.gameTrophyNoTrophies();
    }

    @Override // from PagedGrid
    protected boolean displayNavi (int items)
    {
        return (items > _rows*_cols);
    }

    @Override// from PagedWidget
    protected Widget getNowLoadingWidget ()
    {
        return new MiniNowLoadingWidget();
    }

    protected class TrophyDetail extends FlexTable
    {
        public TrophyDetail (Trophy trophy) {
            setCellSpacing(0);
            setCellPadding(0);
            setStyleName("trophyDetail");

            if (trophy != null) {
                setWidget(0, 0, MediaUtil.createMediaView(
                              trophy.trophyMedia, MediaDescSize.THUMBNAIL_SIZE));
                setText(0, 1, trophy.name);
                if (trophy.description == null) {
                    setText(1, 0, _msgs.gameTrophySecret());
                    getFlexCellFormatter().setStyleName(1, 0, "Italic");
                } else {
                    setText(1, 0, trophy.description);
                }
                if (trophy.whenEarned != null) {
                    Date date = new Date(trophy.whenEarned.longValue());
                    setText(2, 0, _msgs.gameTrophyEarnedOn(DateUtil.formatDate(date, false)));
                    getFlexCellFormatter().setStyleName(2, 0, "Earned");
                }
            }

            getFlexCellFormatter().setStyleName(0, 0, "Image");
            getFlexCellFormatter().setStyleName(0, 1, "Name");
            getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);
            getFlexCellFormatter().setRowSpan(0, 0, getRowCount());
        }
    }

    protected static final GamesMessages _msgs = GWT.create(GamesMessages.class);
    protected static final GameServiceAsync _gamesvc = GWT.create(GameService.class);
}
