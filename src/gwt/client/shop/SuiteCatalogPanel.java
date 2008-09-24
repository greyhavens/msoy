//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasAlignment;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.SubItem;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.CatalogService;

import client.item.ShopUtil;
import client.shell.Args;
import client.shell.DynamicMessages;
import client.shell.Pages;
import client.ui.Marquee;
import client.util.Link;

/**
 * Displays catalog items belonging to a game suite.
 *
 * @author mjensen
 */
public class SuiteCatalogPanel extends SmartTable
{
    public SuiteCatalogPanel (CatalogModels models)
    {
        super("catalogPanel", 0, 0); // mimic the style of the catalog panel
        _models = models;

        // create our listings interface
        _listings = new SmartTable("Listings", 0, 0);
        _listings.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);
        setWidget(0, 0, WidgetUtil.makeShim(10, 10));
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        setWidget(0, 1, WidgetUtil.makeShim(10, 10));
        setWidget(0, 2, _listings, 1, "ListingsCell");
        getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        setWidget(0, 3, WidgetUtil.makeShim(10, 10));

        _items = new ListingGrid(HEADER_HEIGHT) {
            @Override protected void displayPageFromClick (int page) {
                Link.go(Pages.SHOP, ShopUtil.composeArgs(_curquery, page));
            }
            @Override protected String getEmptyMessage () {
                String name = _dmsgs.getString("pItemType" + _curquery.itemType);
                return _msgs.suiteNoItems(_suiteName, name);
            }
        };
        _listings.setWidget(0, 0, WidgetUtil.makeShim(10, 10));
        _listings.setWidget(1, 0, _items, 2, null);
        _listings.getFlexCellFormatter().setHeight(1, 0, "100%");
        _listings.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);
    }

    public void display (int gameId, CatalogService.SuiteInfo suite, byte itemType, int page)
    {
        // set up our queries for subitems
        _subquery.suiteId = suite.suiteId;
        // set up our queries for tagged items
        _tagquery.creatorId = suite.creatorId;
        _tagquery.tag = suite.suiteTag;
        // keep track of some other bits
        _gameId = gameId;
        _types = (suite.suiteTag != null) ? FULL_ITEM_TYPES : NOTAG_ITEM_TYPES;
        _suiteName = suite.name;
        // and finally display our content
        display(itemType, page);
    }

    public void display (byte itemType, int page)
    {
        boolean isSubItem = false;
        for (SubItem item : new Game().getSubTypes()) {
            if (item.getType() == itemType) {
                isSubItem = true;
                break;
            }
        }
        _curquery = isSubItem ? _subquery : _tagquery;
        _curquery.itemType = itemType;

        // display our blurb and the item type
        _listings.setText(0, 0, _dmsgs.getString("catIntro" + _curquery.itemType), 1, "Blurb");
        _listings.setWidget(0, 1, new Marquee(
                                null, _dmsgs.getString("pItemType" + _curquery.itemType)));

        // grab our data model and display it
        CatalogModels.Listings model = _models.getListingsModel(_curquery);
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
                return type == _curquery.itemType;
            }
            public String composeArgs (byte type) {
                return Args.compose(ShopPage.SUITE, _gameId, type);
            }
        }, _types, null));
    }

    public String getName ()
    {
        return _suiteName;
    }

    public String getTitle ()
    {
        return _msgs.suiteShopTitle(_suiteName);
    }

    public int getGameId ()
    {
        return _gameId;
    }

    protected CatalogModels _models;
    protected CatalogQuery _subquery = new CatalogQuery(), _tagquery = new CatalogQuery();
    protected CatalogQuery _curquery;

    protected SmartTable _listings;
    protected ListingGrid _items;

    protected String _suiteName;
    protected byte[] _types;
    protected int _gameId;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final DynamicMessages _dmsgs = GWT.create(DynamicMessages.class);

    protected static final byte[] NOTAG_ITEM_TYPES = new byte[] {
        Item.LEVEL_PACK, Item.ITEM_PACK };
    protected static final byte[] FULL_ITEM_TYPES = new byte[] {
        Item.LEVEL_PACK, Item.ITEM_PACK, Item.AVATAR, Item.FURNITURE, Item.DECOR, Item.TOY,
        Item.PET, Item.PHOTO, Item.AUDIO, Item.VIDEO };

    protected static final int HEADER_HEIGHT = 75;
}
