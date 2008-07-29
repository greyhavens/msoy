package client.item.rating;

import com.threerings.msoy.item.data.all.Item;

public interface ItemRatingModel
{
    public Item getItem();
    
    public byte getMemberRating();
    
    public void setMemberRating(byte rating);
    
    public boolean isFavorite();
    
    public void setFavorite(boolean favorite);
}
