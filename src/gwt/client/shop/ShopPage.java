//
// $Id$

package client.shop;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;

import client.item.ShopUtil;
import client.shell.Args;
import client.shell.CShell;
import client.shell.DynamicLookup;
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
    public static final String SUITE = "g";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        if (action.equals(LOAD_LISTING)) {
            byte type = getItemType(args, 1, Item.NOT_A_TYPE);
            int catalogId = args.get(2, 0);
            _catalogsvc.loadListing(type, catalogId, new MsoyCallback<CatalogListing>() {
                public void onSuccess (CatalogListing listing) {
                    setContent(new ListingDetailPanel(_models, listing));
                }
            });
            CShell.frame.addNavLink(_dmsgs.xlate("pItemType" + type), Pages.SHOP, ""+type);

        } else if (action.equals(FAVORITES)) {
            // if no member is specified, we use the current member
            int memberId = args.get(1, CShop.getMemberId());
            byte type = getItemType(args, 2, Item.NOT_A_TYPE);
            int page = args.get(3, 0);
            setContent(new FavoritesPanel(_models, memberId, type, page));

        } else if (action.equals(SUITE)) {
            final int gameId = args.get(1, 0);
            final byte itemType = getItemType(args, 2, Item.LEVEL_PACK);
            final int page = args.get(3, 0);
            if (_suite.getGameId() != gameId) {
                // only load the suite info in the case that the game id has changed
                _catalogsvc.loadGameSuiteInfo(gameId, new MsoyCallback<CatalogService.SuiteInfo>() {
                    public void onSuccess (CatalogService.SuiteInfo suite) {
                        CShell.frame.addNavLink(suite.name, Pages.GAMES, Args.compose("d", gameId));
                        _suite.display(gameId, suite, itemType, page);
                        setContent(_suite.getTitle(), _suite);
                    }
                });
            } else {
                CShell.frame.addNavLink(_suite.getName(), Pages.GAMES, Args.compose("d", gameId));
                _suite.display(itemType, page);
                setContent(_suite.getTitle(), _suite);
            }

        } else {
            byte type = getItemType(args, 0, Item.NOT_A_TYPE);
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

    /**
     * Extracts the item type from the arguments, sanitizing it if necessary.
     */
    protected byte getItemType (Args args, int index, byte deftype)
    {
        byte type = (byte)args.get(index, deftype);
        if (Item.getClassForType(type) == null) {
            CShell.log("Rejecting invalid item type", "type", type, "args", args);
            return deftype;
        }
        return type;
    }

    protected CatalogModels _models = new CatalogModels();
    protected CatalogPanel _catalog = new CatalogPanel(_models);
    protected SuiteCatalogPanel _suite = new SuiteCatalogPanel(_models);
    protected Map<Integer, Integer> suiteIdMap = new HashMap<Integer, Integer>();

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
}
