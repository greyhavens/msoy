//
// $Id$

package client.profile;

import java.util.Iterator;

import client.util.Stars;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Panel;
import com.threerings.msoy.data.all.GameRating;
import com.threerings.msoy.web.client.ProfileService;

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
    protected void didInit (ProfileService.ProfileResult pdata)
    {
        setHeader(CProfile.msgs.ratingsTitle());

        Iterator i = pdata.ratings.iterator();

        while (i.hasNext() && _content.getRowCount() <= MAX_RATINGS) {
            int row = _content.getRowCount();
            
            GameRating entry = (GameRating) i.next();
            _content.setText(row, 0, entry.gameName);
            _content.getCellFormatter().setStyleName(row, 0, "Game");

            /**
             * FIDE/Elo ratings lie between 1000 and 3000, with new players starting out at
             * 1200 and the average active player having, on average, approximately 1500. The
             * ceiling is very high; anything about 2750 is quite extraordinary.
             * 
             * When we get the rating, it's been mapped into the [0, 1] range, but it is still
             * a highly non-linear distribution. We straighten the value out with a square root,
             * and then we want to map to a 5-star visualization, i.e. 10 half-stars, like so:
             * 
             * 1000 -> 0.00 -> 0.00 -> 0.5 stars
             * 1200 -> 0.10 -> 0.31 -> 2.0 stars
             * 1500 -> 0.25 -> 0.50 -> 3.0 stars
             * 2800 -> 0.90 -> 0.95 -> 5.0 stars
             */
            final float rating = (float) Math.min(5.0, 0.5 + Math.sqrt(entry.rating) * 5.0);

            _content.setWidget(row, 1, new Stars(Stars.MODE_READ, false) {
                protected void starsClicked (byte newRating) {
                }
                protected void update () {
                    updateStarImages(rating, false);
                }
                protected void update (double newRating) {
                    updateStarImages(rating, false);
                }
            });
            _content.getCellFormatter().setStyleName(row, 1, "Rating");
        }
    }

    protected FlexTable _content;
    
    protected static final int MAX_RATINGS = 10;
}
