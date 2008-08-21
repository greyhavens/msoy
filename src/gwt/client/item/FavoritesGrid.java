//
// $Id$

package client.item;

import client.shell.Args;
import client.shell.DynamicMessages;
import client.shell.Pages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.item.data.all.Item;

/**
 * An item grid for displaying favorites.
 *
 * @author mjensen
 */
public class FavoritesGrid extends ItemGrid
{
    public FavoritesGrid (Pages parentPage, int rows, int columns)
    {
        super(parentPage, rows, columns);
    }

    @Override
    protected String getEmptyMessage (){
        return _imsgs.noFavorites();
    }

    @Override
    public String getTitle () {
        byte type = getItemType();
        if (type == Item.NOT_A_TYPE) {
            return _imsgs.allFavorites();
        }
        return _imsgs.favoriteTitle(_dmsgs.getString("pItemType" + type));
    }

    @Override
    protected Widget createWidget (Item item)
    {
        // When this box is clicked, show the item listing in the shop
        String args = Args.compose("l", String.valueOf(item.getType()),
            String.valueOf(item.catalogId));
        return new ItemBox(item.getThumbnailMedia(), item.name, Pages.SHOP, args, false);
    }

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);
}
