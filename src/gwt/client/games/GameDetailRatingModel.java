//
// $Id$

package client.games;

import client.item.rating.ItemRatingModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.game.gwt.GameDetail;

/**
 * Wraps ItemDetails so to a common type that can be passed to the ItemDetailPanel.
 *
 * @author mjensen
 */
public class GameDetailRatingModel implements ItemRatingModel
{
    public GameDetailRatingModel (GameDetail detail)
    {
        this._gameDetail = detail;
    }

    // from interface ItemRatingModel
    public Item getItem ()
    {
        return _gameDetail.listedItem;
    }

    // from interface ItemRatingModel
    public byte getMemberRating ()
    {
        return _gameDetail.memberRating;
    }

    // from interface ItemRatingModel
    public void setMemberRating (byte rating)
    {
        _gameDetail.memberRating = rating;
    }

    // from interface ItemRatingModel
    public boolean isFavorite ()
    {
        return _gameDetail.favorite;
    }

    // from interface ItemRatingModel
    public void setFavorite (boolean favorite)
    {
        _gameDetail.favorite = favorite;
    }

    protected GameDetail _gameDetail;
}
