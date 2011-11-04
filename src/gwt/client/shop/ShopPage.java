//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.samskivert.util.ByteEnumUtil;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.Theme;
import com.threerings.msoy.group.gwt.GroupService;
import com.threerings.msoy.group.gwt.GroupServiceAsync;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.CatalogListing;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.stuff.gwt.StuffService;
import com.threerings.msoy.stuff.gwt.StuffServiceAsync;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ShopUtil;
import client.remix.ItemRemixer;
import client.remix.RemixerHost;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.shell.Page;
import client.util.InfoCallback;
import client.util.Link;

/**
 * Handles the MetaSOY inventory application.
 */
public class ShopPage extends Page
{
    public static final String LOAD_LISTING = "l";
    public static final String FAVORITES = "f";
    public static final String SUITE = "s";
    public static final String GAME = "g";
    public static final String REMIX = "r";
    public static final String JUMBLE = "j";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        Integer themeId = null;
        if (action.equals(LOAD_LISTING)) {
            MsoyItemType type = getItemType(args, 1, MsoyItemType.NOT_A_TYPE);
            int catalogId = args.get(2, 0);
            setContent(new ListingDetailPanel(_models, type, catalogId));

        } else if (action.equals(FAVORITES)) {
            // if no member is specified, we use the current member
            int memberId = args.get(1, CShell.getMemberId());
            MsoyItemType type = getItemType(args, 2, MsoyItemType.NOT_A_TYPE);
            int page = args.get(3, 0);
            setContent(new FavoritesPanel(_models, memberId, type, page));

        } else if (action.equals(SUITE)) {
            final MsoyItemType type = getItemType(args, 1, MsoyItemType.NOT_A_TYPE);
            final int catalogId = args.get(2, 0);
            setContent(new SuiteCatalogPanel(_models, type, catalogId));

        } else if (action.equals(GAME)) {
            final int gameId = args.get(1, 0);
            setContent(new SuiteCatalogPanel(_models, gameId));

        } else if (action.equals(REMIX)) {
            final MsoyItemType type = getItemType(args, 1, MsoyItemType.AVATAR);
            final int itemId = args.get(2, 0);
            final int catalogId = args.get(3, 0);
            final ItemRemixer remixer = new ItemRemixer();
            _stuffsvc.loadItem(new ItemIdent(type, itemId), new InfoCallback<Item>() {
                public void onSuccess (Item result) {
                    remixer.init(createRemixerHost(remixer, type, catalogId), result, catalogId);
                }
            });
            setContent(remixer);

        } else if (action.equals(JUMBLE)) {
            themeId = args.get(1, 0);

        } else if (getItemType(args, 0, MsoyItemType.NOT_A_TYPE) == MsoyItemType.NOT_A_TYPE) {
            themeId = args.get(0, 0);

        } else {
            if (!_catalog.isAttached()) {
                setContent(_catalog);
            }
            CatalogQuery query = new CatalogQuery();
            int page = ShopUtil.parseArgs(args, query);
            _catalog.display(query, page);
        }

        if (themeId != null) {
            if (themeId != 0) {
                _groupsvc.getTheme(themeId, new InfoCallback<Theme>() {
                    public void onSuccess (Theme result) {
                        loadShopPage(result != null ? result.group : null);
                    }
                });
                return;
            }
            loadShopPage(null);
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.SHOP;
    }

    protected void loadShopPage (GroupName theme)
    {
        setContent(null, new ShopPanel(theme));
    }

    protected RemixerHost createRemixerHost (
        final ItemRemixer remixer, final MsoyItemType type, final int catalogId)
    {
        return new RemixerHost() {
            public void buyItem () {
                // Request the listing, re-reserving a new price for us
                _catalogsvc.loadListing(type, catalogId, false,
                    new InfoCallback<CatalogListing>() {
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
            public void remixComplete (Item item) {
                if (item != null) {
                    Link.go(Pages.STUFF, "d", item.getType().toByte(), item.itemId);
                } else {
                    History.back();
                }
            }
        };
    }

    /**
     * Extracts the item type from the arguments, sanitizing it if necessary.
     */
    protected MsoyItemType getItemType (Args args, int index, MsoyItemType deftype)
    {
        byte code = args.get(index, (byte) -1);
        if (code >= 0) {
            MsoyItemType type = ByteEnumUtil.fromByte(MsoyItemType.class, code);
            if (type != null) {
                return type;
            }
        }
        return deftype;
    }

    protected CatalogModels _models = new CatalogModels();
    protected CatalogPanel _catalog = new CatalogPanel(_models);

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final CatalogServiceAsync _catalogsvc = GWT.create(CatalogService.class);
    protected static final StuffServiceAsync _stuffsvc = GWT.create(StuffService.class);
    protected static final GroupServiceAsync _groupsvc = GWT.create(GroupService.class);
}
