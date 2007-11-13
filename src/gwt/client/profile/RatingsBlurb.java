//
// $Id$

package client.profile;

import java.util.Iterator;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.GameRating;

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
    protected Panel createContent ()
    {
        _content = new FlexTable();
        _content.setWidth("100%");
        _content.addStyleName("ratingsBlurb");
        return _content;
    }

    // @Override // from Blurb
    public boolean shouldDisplay (ProfileService.ProfileResult pdata)
    {
        return (pdata.ratings != null && pdata.ratings.size() > 0);
    }

    // @Override // from Blurb
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        setHeader(CProfile.msgs.ratingsTitle());

        for (Iterator iter = pdata.ratings.iterator(); iter.hasNext(); ) {
            int row = _content.getRowCount();
            final GameRating entry = (GameRating) iter.next();

            ClickListener gameClick = new ClickListener() {
                public void onClick (Widget sender) {
                    Application.go(Page.GAME, Args.compose("d", ""+entry.gameId,
                                                           GameDetailPanel.MYRANKINGS_TAB));
                }
            };
            Image image = (Image)MediaUtil.createMediaView(
                entry.gameThumb, MediaDesc.THUMBNAIL_SIZE);
            image.addClickListener(gameClick);
            image.setStyleName("actionLabel");
            _content.setWidget(row, 0, image);
            _content.getFlexCellFormatter().setStyleName(row, 0, "GameThumb");
            if (entry.singleRating > 0) {
                _content.getFlexCellFormatter().setRowSpan(row, 0, 2);
            }

            _content.setWidget(row, 1, MsoyUI.createActionLabel(entry.gameName, gameClick));
            _content.getFlexCellFormatter().setStyleName(row, 1, "GameName");

            if (entry.multiRating > 0) {
                _content.setText(row, 2, "" + entry.multiRating);
                _content.getFlexCellFormatter().setStyleName(row, 2, "Rating");
            }

            if (entry.singleRating > 0) {
                _content.setText(++row, 0, CProfile.msgs.ratingsSingle());
                _content.getFlexCellFormatter().setStyleName(row, 0, "Note");
                _content.setText(row, 1, "" + entry.singleRating);
                _content.getFlexCellFormatter().setStyleName(row, 1, "Rating");
            }
        }
    }

    protected FlexTable _content;
}
