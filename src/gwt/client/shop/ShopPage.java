//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;

import client.item.ShopUtil;
import client.shell.Args;
import client.shell.Page;
import client.shell.Pages;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

/**
 * Handles the MetaSOY inventory application.
 */
public class ShopPage extends Page
{
    public static final String LOAD_LISTING = "l";

    public static final String FAVORITES = "f";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals(LOAD_LISTING)) {
            byte type = (byte)args.get(1, Item.NOT_A_TYPE);
            int catalogId = args.get(2, 0);
            _catalogsvc.loadListing(type, catalogId, new MsoyCallback<CatalogListing>() {
                public void onSuccess (CatalogListing listing) {
                    setContent(new ListingDetailPanel(_models, listing));
                }
            });
        } else if (action.equals(FAVORITES)) {
            // get the member id from args, if no member id, use the current user
            int memberId = args.get(1, CShop.getMemberId());
            byte itemType = (byte) args.get(2, (int) Item.NOT_A_TYPE);
            String[] prefixArgs = new String[] { FAVORITES, String.valueOf(memberId) };
            // TODO get member's name (passed from the profile page? loaded from service?) from id
            // and display it in the page title
            String title = (memberId == CShop.getMemberId()) ? CShop.msgs.myFavoritesTitle()
                : CShop.msgs.favoritesTitle();
            setContent(title, _favorites);
            _favorites.update(memberId, itemType, prefixArgs, args);
        } else {
            byte type = (byte)args.get(0, Item.NOT_A_TYPE);
            if (type == Item.NOT_A_TYPE) {
                setContent(CShop.msgs.catalogTitle(), new ShopPanel());
            } else {
                if (!_catalog.isAttached()) {
                    setContent(_catalog);
                }
                _catalog.display(ShopUtil.parseArgs(args), args.get(3, 0));
            }
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.SHOP;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // load up our translation dictionaries
        CShop.msgs = (ShopMessages)GWT.create(ShopMessages.class);
    }

    @Override
    public void onPageLoad ()
    {
        super.onPageLoad();

        _favorites = new FavesPanel();
    }

    protected CatalogModels _models = new CatalogModels();
    protected CatalogPanel _catalog = new CatalogPanel(_models);
    protected FavesPanel _favorites;

    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
}
