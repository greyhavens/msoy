//
// $Id$

package client.shop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.SimpleDataModel;

import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.ListingCard;

import client.item.ListingBox;
import client.item.SideBar;
import client.shell.CShell;
import client.ui.Marquee;
import client.util.InfoCallback;

/**
 * Displays catalog items belonging to a suite.
 */
public class SuiteCatalogPanel extends SmartTable
{
    public SuiteCatalogPanel (CatalogModels models, MsoyItemType itemType, int catalogId)
    {
        this(itemType);
        models.getSuite(itemType, catalogId, new InfoCallback<CatalogService.SuiteResult>() {
            public void onSuccess (CatalogService.SuiteResult result) {
                init(result);
            }
        });
    }

    public SuiteCatalogPanel (CatalogModels models, int gameId)
    {
        this(MsoyItemType.NOT_A_TYPE);
        models.getSuite(gameId, new InfoCallback<CatalogService.SuiteResult>() {
            public void onSuccess (CatalogService.SuiteResult result) {
                init(result);
            }
        });
    }

    protected SuiteCatalogPanel (MsoyItemType itemType)
    {
        super("catalogPanel", 0, 0); // mimic the style of the catalog panel

        // create our listings interface
        _listings = new SmartTable("Listings", 0, 0);
        _listings.getFlexCellFormatter().setHorizontalAlignment(0, 1, HasAlignment.ALIGN_RIGHT);
        _listings.setText(0, 0, _msgs.suiteBlurb(), 1, "Blurb");
        _listings.setWidget(0, 1, new Marquee(null, _msgs.suiteDefTitle()));

// TODO: add loading indicator when we have one that does not require crack smoking
//         _listings.setWidget(1, 0, _items, 2, null);

        // set up our sidebar and main page structure
        CatalogQuery query = new CatalogQuery();
        query.itemType = itemType;
        MsoyItemType[] items = CShell.getClientMode().isMinimal() ?
            MsoyItemType.DJ_ITEMS : MsoyItemType.SHOP_ITEMS;
        setWidget(0, 0, new SideBar(new CatalogQueryLinker(query), items, null));
        getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        setWidget(0, 1, WidgetUtil.makeShim(10, 10));
        setWidget(0, 2, _listings, 1, "ListingsCell");
        getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
        setWidget(0, 3, WidgetUtil.makeShim(10, 10));
    }

    protected void init (CatalogService.SuiteResult suite)
    {
        // display the game title
        _listings.setWidget(0, 1, new Marquee(null, suite.name));
        _listings.getFlexCellFormatter().setHeight(1, 0, "100%");
        _listings.getFlexCellFormatter().setVerticalAlignment(1, 0, HasAlignment.ALIGN_TOP);

        // display the items in one big grid; TODO: make a custom display that separates them out
        // by subtype with little headers
        ListingGrid items = new ListingGrid(100, 4) {
            @Override protected String getEmptyMessage () {
                return "error"; // can't happen, we always have the suite master item
            }
            @Override protected Widget createWidget (ListingCard card) {
                return ListingBox.newSubBox(card);
            }
        };
        items.setModel(SimpleDataModel.newModel(suite.listings), 0);
        _listings.setWidget(1, 0, items, 2);
    }

    protected SmartTable _listings;

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
}
