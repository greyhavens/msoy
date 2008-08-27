//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

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
    public static final String TRANSACTIONS = "t";

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
            // if no member is specified, we use the current member
            int memberId = args.get(1, CShop.getMemberId());
            byte itemType = (byte)args.get(2, Item.NOT_A_TYPE);
            int page = args.get(3, 0);
            setContent(new FavoritesPanel(_models, memberId, itemType, page));


        } else if (action.equals(TRANSACTIONS)) {
//             _moneysvc.getTransactionHistory(CShop.getMemberId(), new MsoyCallback<List<Integer>>() {
//                 public void onSuccess (List<Integer> history) {
//                     setContent(CShop.msgs.transactionsTitle(), new ShopPanel());
//                     // TODO
//                 }
//             });

        } else {
            byte type = (byte)args.get(0, Item.NOT_A_TYPE);
            if (type == Item.NOT_A_TYPE) {
                setContent(_msgs.catalogTitle(), new ShopPanel());
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

    protected CatalogModels _models = new CatalogModels();
    protected CatalogPanel _catalog = new CatalogPanel(_models);

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);

    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
