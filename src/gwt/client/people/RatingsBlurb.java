//
// $Id$

package client.people;

import java.util.List;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.GameRating;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import client.games.GameDetailPanel;
import client.shell.Args;
import client.shell.Page;
import client.util.Link;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Displays a person's game ratings.
 */
public class RatingsBlurb extends Blurb
{
    @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.ratings != null && pdata.ratings.size() > 0);
    }

    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(CPeople.msgs.ratingsTitle());
        setContent(new RatingGrid(pdata.ratings));
    }

    protected class RatingGrid extends PagedGrid<GameRating>
    {
        public RatingGrid (List<GameRating> ratings)
        {
            super(RATING_ROWS, 2, NAV_ON_BOTTOM);
            setModel(new SimpleDataModel<GameRating>(ratings), 0);
        }

        @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return ""; // not used
        }

        @Override // from PagedGrid
        protected boolean displayNavi (int items)
        {
            return (items > _rows * _cols);
        }

        @Override // from PagedGrid
        protected Widget createWidget (GameRating rating)
        {
            return new RatingWidget(rating);
        }
    }

    protected class RatingWidget extends FlexTable
    {
        public RatingWidget (final GameRating entry)
        {
            setCellPadding(0);
            setCellSpacing(0);
            setStyleName("ratingWidget");
            getFlexCellFormatter().setStyleName(0, 0, "GameThumb");
            getFlexCellFormatter().setStyleName(0, 1, "GameName");

            ClickListener gameClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Link.go(Page.GAMES, Args.compose("d", ""+entry.gameId,
                                                            GameDetailPanel.MYRANKINGS_TAB));
                }
            };
            setWidget(0, 0, MediaUtil.createMediaView(
                          entry.gameThumb, MediaDesc.HALF_THUMBNAIL_SIZE, gameClick));
            if (entry.singleRating > 0) {
                getFlexCellFormatter().setRowSpan(0, 0, 2);
            }

            setWidget(0, 1, MsoyUI.createActionLabel(entry.gameName, gameClick));

            if (entry.multiRating > 0) {
                setText(0, 2, "" + entry.multiRating);
                getFlexCellFormatter().setStyleName(0, 2, "Rating");
            }

            if (entry.singleRating > 0) {
                setText(1, 0, CPeople.msgs.ratingsSingle());
                getFlexCellFormatter().setStyleName(1, 0, "Note");
                setText(1, 1, "" + entry.singleRating);
                getFlexCellFormatter().setStyleName(1, 1, "Rating");
            }
        }
    }

    protected static final int RATING_ROWS = 2;
}
