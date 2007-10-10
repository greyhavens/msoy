//
// $Id$

package client.profile;

import java.util.Iterator;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Panel;

import client.util.MediaUtil;
import client.util.Stars;

import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.web.client.ProfileService;
import com.threerings.msoy.web.data.GameRating;

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
            GameRating entry = (GameRating) iter.next();

            _content.setWidget(row, 0, MediaUtil.createMediaView(
                entry.gameThumb, MediaDesc.HALF_THUMBNAIL_SIZE));
            _content.getFlexCellFormatter().setStyleName(row, 0, "GameThumb");
            if (entry.singleRating > 0) {
                _content.getFlexCellFormatter().setRowSpan(row, 0, 2);
            }

            _content.setText(row, 1, entry.gameName);
            _content.getFlexCellFormatter().setStyleName(row, 1, "GameName");

            if (entry.multiRating > 0) {
                _content.setWidget(row, 2, createRatingLabel(entry.multiRating));
                _content.getFlexCellFormatter().setStyleName(row, 2, "Rating");
            }

            if (entry.singleRating > 0) {
                _content.setText(++row, 0, CProfile.msgs.ratingsSingle());
                _content.getFlexCellFormatter().setStyleName(row, 0, "Note");
                _content.setWidget(row, 1, createRatingLabel(entry.singleRating));
                _content.getFlexCellFormatter().setStyleName(row, 1, "Rating");
            }
        }
    }

    protected Stars createRatingLabel (float rating)
    {
        // Whirled ratings lie between 1000 and 3000, with new players starting out at 1200 and the
        // average active player having, on average, approximately 1500. The ceiling is very high;
        // anything about 2750 is quite extraordinary.
        //
        // When we get the rating, it's been mapped into the [0, 1] range, but it is still a highly
        // non-linear distribution. We straighten the value out with a square root, and then we
        // want to map to a 5-star visualization, i.e. 10 half-stars, like so:
        //
        // 1000 -> 0.00 -> 0.00 -> 0.5 stars
        // 1200 -> 0.10 -> 0.31 -> 2.0 stars
        // 1500 -> 0.25 -> 0.50 -> 3.0 stars
        // 2800 -> 0.90 -> 0.95 -> 5.0 stars
        final float stars = (float) Math.min(5.0, 0.5 + Math.sqrt(rating) * 5.0);

        return new Stars(Stars.MODE_READ, false) {
            protected void starsClicked (byte newRating) {
            }
            protected void update () {
                updateStarImages(stars, false);
            }
            protected void update (double newRating) {
                updateStarImages(stars, false);
            }
        };
    }

    protected FlexTable _content;
}
