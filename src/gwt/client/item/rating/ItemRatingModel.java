//
// $Id$

package client.item.rating;

import com.threerings.msoy.item.data.all.Item;

/**
 * Provides the information needed by the item rating display.
 */
public interface ItemRatingModel
{
    /** Provides the item being rated. */
    Item getItem ();

    /** Provides the rating assigned to the item by this member or 0 if the member has not rated
     * it. */
    byte getMemberRating ();

    /** Called when the user updates their rating of this item. TODO: are we responsible for
     * telling the server that they updated their rating? */
    void setMemberRating (byte rating);

    /** Returns true if the item in question has been marked as a favorite by the user. */
    boolean isFavorite ();

    /** Called if the user changes the favorite status of this item. TODO: are we responsible for
     * telling the server that they updated the favorite status? */
    void setFavorite (boolean favorite);
}
