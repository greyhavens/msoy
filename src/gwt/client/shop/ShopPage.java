//
// $Id$

package client.shop;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;

import client.item.ShopUtil;
import client.remix.ItemRemixer;
import client.remix.RemixerHost;
import client.shell.Args;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.Page;
import client.shell.Pages;
import client.util.Link;
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
    public static final String REMIX = "r";

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
            CShell.frame.addNavLink(_dmsgs.xlate("pItemType" + type), Pages.SHOP, ""+type, 1);

        } else if (action.equals(FAVORITES)) {
            // if no member is specified, we use the current member
            int memberId = args.get(1, CShell.getMemberId());
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
                        CShell.frame.addNavLink(
                            suite.name, Pages.GAMES, Args.compose("d", gameId), 1);
                        _suite.display(gameId, suite, itemType, page);
                        setContent(_suite.getTitle(), _suite);
                    }
                });
            } else {
                CShell.frame.addNavLink(_suite.getName(), Pages.GAMES, Args.compose("d", gameId), 1);
                _suite.display(itemType, page);
                setContent(_suite.getTitle(), _suite);
            }

        } else if (action.equals(REMIX)) {
            final byte type = getItemType(args, 1, Item.AVATAR);
            final int itemId = args.get(2, 0);
            final int catalogId = args.get(3, 0);
            final ItemRemixer remixer = new ItemRemixer();
            _stuffsvc.loadItem(new ItemIdent(type, itemId), new MsoyCallback<Item>() {
                public void onSuccess (Item result) {
                    remixer.init(createRemixerHost(remixer, type, catalogId), result, catalogId);
                }
            });
            setContent(remixer);

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

    protected RemixerHost createRemixerHost (
        final ItemRemixer remixer, final byte type, final int catalogId)
    {
        return new RemixerHost() {
            public void buyItem () {
                // Request the listing, re-reserving a new price for us
                _catalogsvc.loadListing(type, catalogId, new MsoyCallback<CatalogListing>() {
                    public void onSuccess (CatalogListing listing) {
                        // and display a mini buy dialog.
                        new BuyRemixDialog(listing, new AsyncCallback<Item>() {
                                public void onFailure (Throwable cause) { /* not used */ }

                                public void onSuccess (Item item) {
                                    remixer.itemPurchased(item);
                                }
                            });
                    }
                });
            }

            // called only when the remixer exits
            public void remixComplete (Item item)
            {
                if (item != null) {
                    Link.go(Pages.STUFF, Args.compose("d", item.getType(), item.itemId));

                } else {
                    History.back();
                }
            }
        };
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

   protected static final StuffServiceAsync _stuffsvc = (StuffServiceAsync)
       ServiceUtil.bind(GWT.create(StuffService.class), StuffService.ENTRY_POINT);

}
