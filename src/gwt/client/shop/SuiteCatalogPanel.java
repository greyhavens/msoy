//
// $Id$

package client.shop;

import client.item.ShopUtil;
import client.shell.Args;
import client.shell.DynamicMessages;
import client.shell.Pages;
import client.ui.Marquee;
import client.util.Link;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasAlignment;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.CatalogService;

/**
 * Displays catalog items belonging to a game suite.
 *
 * @author mjensen
 */
public class SuiteCatalogPanel extends SmartTable
{
    public SuiteCatalogPanel (CatalogModels models)
    {
        // mimic the style of the catalog panel
        super("catalogPanel", 0, 0);
        _models = models;

        // create our listings interface
        _listings = new SmartTable("Listings", 0, 0);
        setWidget(0, 0, WidgetUtil.makeShim(10, 10));
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        setWidget(0, 1, WidgetUtil.makeShim(10, 10));
        setWidget(0, 2, _listings, 1, "ListingsCell");
        getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        setWidget(0, 3, WidgetUtil.makeShim(10, 10));

        _items = new ListingGrid(HEADER_HEIGHT) {
            @Override protected void displayPageFromClick (int page) {
                Link.go(Pages.SHOP, ShopUtil.composeArgs(_query, page));
            }
            @Override protected String getEmptyMessage () {
                String name = _dmsgs.getString("itemType" + _query.itemType);
                return CShop.msgs.catalogNoList(name);
            }
        };
        // TODO add a snappy blurb for level and item packs
        _listings.setWidget(0, 0, WidgetUtil.makeShim(10, 10));
        _listings.setWidget(1, 0, _items, 2, null);
        _listings.getFlexCellFormatter().setHeight(1, 0, "100%");
        _listings.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);
    }

    public void display (int gameId, CatalogService.SuiteInfo suiteInfo, byte itemType, int page)
    {
        _gameId = gameId;
        _query.suiteId = suiteInfo.suiteId;
        _suiteName = suiteInfo.name;
        display(itemType, page);
    }

    public void display (byte itemType, int page)
    {
        _query.itemType = itemType;

        // update the marquee with the plural name of the item type
        String tname = _dmsgs.getString("pItemType" + _query.itemType);
        _listings.setWidget(0, 1, new Marquee(null, tname));
        _listings.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);

        // grab our data model and display it
        CatalogModels.Listings model = _models.getListingsModel(_query);
        CatalogModels.Listings current = (CatalogModels.Listings)_items.getModel();
        if (current != null && current.getType() != model.getType()) {
            // clear the display when we switching item types so that we don't see items of the
            // old type while items of the new type are loading
            _items.clear();
        }
        _items.setModel(model, page);

        // update the side bar
        setWidget(0, 0, new SideBar(new SideBar.Linker() {
            public boolean isSelected (byte type) {
                return type == _query.itemType;
            }
            public String composeArgs (byte type) {
                return Args.compose(new String[] { ShopPage.SUITE, ""+_gameId, ""+type });
            }
        }, ITEM_TYPES, null));

    }

    public String getTitle ()
    {
        return CShop.msgs.suiteShopTitle(_suiteName);
    }

    public int getGameId ()
    {
        return _gameId;
    }

    protected SmartTable _listings = new SmartTable();

    protected ListingGrid _items;

    protected CatalogQuery _query = new CatalogQuery();

    protected CatalogModels _models;

    protected String _suiteName;

    protected int _gameId;

    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);

    protected static final byte[] ITEM_TYPES = new byte[]{ Item.ITEM_PACK, Item.LEVEL_PACK };

    protected static final int HEADER_HEIGHT = 75;
}
