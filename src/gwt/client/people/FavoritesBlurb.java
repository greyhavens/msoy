//
// $Id$

package client.people;

import client.item.ItemBox;
import client.item.ItemGrid;
import client.item.ItemListDataModel;
import client.item.ItemMessages;
import client.shell.Args;
import client.shell.Pages;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.item.gwt.ItemService;
import com.threerings.msoy.item.gwt.ItemServiceAsync;
import com.threerings.msoy.profile.gwt.ProfileService;

/**
 * A blurb containing a few of the current member's most recent favorites.
 *
 * @author mjensen
 */
public class FavoritesBlurb extends Blurb
{
    @Override // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(CPeople.msgs.favoritesTitle());

        // load the first few items of the favorites list
        _itemsvc.getFavoriteListInfo(pdata.name.getMemberId(), new MsoyCallback<ItemListInfo>() {
            public void onSuccess (ItemListInfo favoriteListInfo) {
                setListId(favoriteListInfo.listId);
            }
        });

        setFooterLink(CPeople.msgs.seeMoreFavorites(pdata.name.toString()),
            Pages.SHOP, Args.compose("f", pdata.name.getMemberId()));
    }

    protected void setListId (int listId)
    {
        // create the favorites model
        ItemListDataModel _favoriteModel = new ItemListDataModel(Item.NOT_A_TYPE);
        _favoriteModel.setListId(listId);
        // order favorites starting with the most recently favorited items
        _favoriteModel.setDescending(true);

        ItemGrid _favorites = new ItemGrid(Pages.PEOPLE, 1, 5) {
            @Override
            protected String getEmptyMessage (){
                return _imsgs.noFavorites();
            }

            @Override
            public String getTitle () {
                return _imsgs.favorites();
            }

            @Override
            protected Widget createWidget (Item item)
            {
                // When this box is clicked, show the item listing in the shop
                String args = Args.compose("l", String.valueOf(item.getType()),
                    String.valueOf(item.catalogId));
                return new ItemBox(item.getThumbnailMedia(), item.name, Pages.SHOP, args, false);
            }
        };
        _favorites.setDisplayNavigation(false);
        _favorites.setModel(_favoriteModel, 0);

        setContent(_favorites);
    }

    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);

    protected static final ItemServiceAsync _itemsvc = (ItemServiceAsync)
        ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);
}
