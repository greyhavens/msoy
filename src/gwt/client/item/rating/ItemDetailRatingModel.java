package client.item.rating;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.ItemDetail;

/**
 * Wraps ItemDetails so that it can be passed as a common type to the ItemDetailPanel.
 * 
 * @author mjensen
 */
public class ItemDetailRatingModel implements ItemRatingModel
{
    public ItemDetailRatingModel(ItemDetail _detail)
    {
        this._itemDetail = _detail;
    }

    public Item getItem ()
    {
        return _itemDetail.item;
    }

    public byte getMemberRating ()
    {
        return _itemDetail.memberRating;
    }

    public void setMemberRating (byte rating)
    {
        _itemDetail.memberRating = rating;
    }  
    
    public boolean isFavorite ()
    {
        return _itemDetail.favorite;
    }
    
    public void setFavorite (boolean favorite)
    {
        _itemDetail.favorite = favorite;        
    }
    
    protected ItemDetail _itemDetail;  
}
