//
// $Id$

package client.games;

// import client.util.Stars;

import com.threerings.gwt.ui.InlineLabel;

/**
 * Displays a player's rating as stars.
 */
public class RatingLabel extends InlineLabel // Stars
{
    public RatingLabel (int rating)
    {
        super("Rating: " + rating);
    }

//     public RatingLabel (float rating)
//     {
//         super(MODE_READ, false);

//         // Whirled ratings lie between 1000 and 3000, with new players starting out at 1200 and the
//         // average active player having, on average, approximately 1500. The ceiling is very high;
//         // anything about 2750 is quite extraordinary.
//         //
//         // When we get the rating, it's been mapped into the [0, 1] range, but it is still a highly
//         // non-linear distribution. We straighten the value out with a square root, and then we
//         // want to map to a 5-star visualization, i.e. 10 half-stars, like so:
//         //
//         // 1000 -> 0.00 -> 0.00 -> 0.5 stars
//         // 1200 -> 0.10 -> 0.31 -> 2.0 stars
//         // 1500 -> 0.25 -> 0.50 -> 3.0 stars
//         // 2800 -> 0.90 -> 0.95 -> 5.0 stars
//         _stars = (float) Math.min(5.0, 0.5 + Math.sqrt(rating) * 5.0);
//         update(); // the superclass call to update comes before _stars is initialized
//     }

//     // @Override // from Stars
//     protected void starsClicked (byte newRating)
//     {
//         // nada
//     }

//     // @Override // from Stars
//     protected void update ()
//     {
//         updateStarImages(_stars, false);
//     }

//     // @Override // from Stars
//     protected void update (double newRating)
//     {
//         updateStarImages(_stars, false);
//     }

//     protected float _stars;
}
