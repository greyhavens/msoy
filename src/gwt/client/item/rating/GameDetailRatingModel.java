package client.item.rating;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.game.gwt.GameDetail;

/**
 * Wraps ItemDetails so to a common type that can be passed to the ItemDetailPanel.
 * 
 * @author mjensen
 */
public class GameDetailRatingModel implements ItemRatingModel
{
    public GameDetailRatingModel(GameDetail detail)
    {
        this._gameDetail = detail;
    }

    public Item getItem ()
    {
        return _gameDetail.listedItem;
    }

    public byte getMemberRating ()
    {
        return _gameDetail.memberRating;
    }
    
    public void setMemberRating (byte rating)
    {
        _gameDetail.memberRating = rating;
    }  

    public boolean isFavorite ()
    {
        return _gameDetail.favorite;
    }

    public void setFavorite (boolean favorite)
    {
        _gameDetail.favorite = favorite;
    }
    
    protected GameDetail _gameDetail;   
}
