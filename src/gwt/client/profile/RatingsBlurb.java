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

            // scale the rating into the [0.5, 5.0] range
            final float rating = (float) (0.5 + entry.rating * 4.5);

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
