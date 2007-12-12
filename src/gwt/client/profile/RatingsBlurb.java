//
// $Id$

package client.profile;

import java.util.Iterator;
import java.util.List;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.GameRating;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.SimpleDataModel;

import client.game.GameDetailPanel;
import client.game.RatingLabel;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Displays a person's game ratings.
 */
public class RatingsBlurb extends Blurb
{
    // @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.ratings != null && pdata.ratings.size() > 0);
    }

    // @Override // from Blurb
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        setHeader(CProfile.msgs.ratingsTitle());
        setContent(new RatingGrid(pdata.ratings));
    }

    protected class RatingGrid extends PagedGrid
    {
        public RatingGrid (List ratings)
        {
            super(RATING_ROWS, 1, NAV_ON_BOTTOM);
            addStyleName("dottedGrid");
            addStyleName("ratingsBlurb");
            setModel(new SimpleDataModel(ratings), 0);
        }

        // @Override // from PagedGrid
        protected String getEmptyMessage ()
        {
            return ""; // not used
        }

        // @Override // from PagedGrid
        protected boolean displayNavi (int items)
        {
            return true;
        }

        // @Override // from PagedGrid
        protected Widget createWidget (Object item)
        {
            return new RatingWidget((GameRating)item);
        }

        // @Override // from PagedGrid
        protected boolean padToFullPage ()
        {
            return true;
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

            if (entry == null) {
                return; // we're a padding entry
            }

            ClickListener gameClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.GAME, Args.compose("d", ""+entry.gameId,
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
                setText(1, 0, CProfile.msgs.ratingsSingle());
                getFlexCellFormatter().setStyleName(1, 0, "Note");
                setText(1, 1, "" + entry.singleRating);
                getFlexCellFormatter().setStyleName(1, 1, "Rating");
            }
        }
    }

    protected static final int RATING_ROWS = 4;
}
