//
// $Id$

package client.people;

import client.item.FavoritesGrid;
import client.item.ItemListDataModel;
import client.shell.Args;
import client.shell.Pages;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
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
    @Override
    // from Blurb
    public void init (ProfileService.ProfileResult pdata)
    {
        super.init(pdata);
        setHeader(CPeople.msgs.favoritesTitle());

        // load the first few items of the favorites list
        _itemsvc.getFavoriteListInfo(pdata.name.getMemberId(), new MsoyCallback<ItemListInfo>() {
            public void onSuccess (ItemListInfo favoriteListInfo)
            {
                // create the favorites model
                ItemListDataModel favoriteModel = new ItemListDataModel(Item.NOT_A_TYPE);
                favoriteModel.setListId(favoriteListInfo.listId);
                // order favorites starting with the most recently favorited items
                favoriteModel.setDescending(true);

                FavoritesGrid favorites = new FavoritesGrid(Pages.PEOPLE, 1, 5);
                favorites.setDisplayNavigation(false);
                favorites.setModel(favoriteModel, 0);

                setContent(favorites);
            }
        });

        setFooterLink(CPeople.msgs.seeMoreFavorites(pdata.name.toString()), Pages.SHOP,
            Args.compose("f", pdata.name.getMemberId()));
    }

    protected static final ItemServiceAsync _itemsvc =
        (ItemServiceAsync) ServiceUtil.bind(GWT.create(ItemService.class), ItemService.ENTRY_POINT);
}
