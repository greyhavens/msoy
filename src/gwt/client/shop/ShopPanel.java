//
// $Id$

package client.shop;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.ServiceUtil;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.data.all.MediaDesc;

import client.shell.DynamicLookup;
import client.ui.HeaderBox;
import client.ui.MoneyLabel;
import client.ui.MsoyUI;
import client.ui.SearchBox;
import client.ui.Stars;
import client.ui.ThumbBox;

import client.util.Link;
import client.item.ItemMessages;
import client.item.SideBar;

/**
 * Displays the main catalog landing page.
 */
public class ShopPanel extends FlowPanel
{
    public ShopPanel ()
    {
        setStyleName("shopPanel");

        // prepare the search box
        HorizontalPanel search = new HorizontalPanel();
        search.setStyleName("Search");
        search.setSpacing(5);
        search.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
        search.add(MsoyUI.createLabel(_msgs.shopSearch(), "SearchTitle"));
        final ListBox searchTypes = new ListBox();
        for (byte searchType : Item.STUFF_TYPES) {
            searchTypes.addItem(_dmsgs.xlate("pItemType" + searchType), searchType + "");
        }
        search.add(searchTypes);
        SearchBox searchBox = new SearchBox(new SearchBox.Listener() {
            public void search (String query) {
                String type = searchTypes.getValue(searchTypes.getSelectedIndex());
                Link.go(Pages.SHOP, type, CatalogQuery.SORT_BY_NEW_AND_HOT, "s" + query);
            }
            public void clearSearch () {
                String type = searchTypes.getValue(searchTypes.getSelectedIndex());
                Link.go(Pages.SHOP, type);
            }
        });
        search.add(searchBox);
        search.add(MsoyUI.createImageButton("GoButton", searchBox.makeSearchListener()));

        // display static header and search across the top
        add(MsoyUI.createLabel(_msgs.shopMarquee(), "ShopTitle"));
        add(search);

        // display categories on the left, content on the right
        HorizontalPanel row = new HorizontalPanel();
        row.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);
        SideBar sidebar = new SideBar(new CatalogQueryLinker(new CatalogQuery()), false, null);
        sidebar.add(MsoyUI.createImage("/images/shop/shop_bag.png", "Bag"));
        row.add(sidebar);
        row.add(_contents = MsoyUI.createFlowPanel("TopContents"));
        _contents.add(MsoyUI.createHTML(_msgs.shopIntro(), "Intro"));
        add(row);
        add(WidgetUtil.makeShim(15, 15));

        ListingGrid grid = new ListingGrid(HEADER_HEIGHT) {
            @Override protected String getEmptyMessage () {
                // TODO
                return "No Items";
            }
        };

        grid.setModel(new CatalogModels.Jumble(), 0);
        _contents.add(grid);
    }


    protected Widget createTop (String icon, String title, List<ListingCard> listings)
    {
        HeaderBox box = new HeaderBox("/images/shop/icon_" + icon + ".png", title);
        for (int ii = 0; ii < listings.size(); ii++) {
            box.add(new TopListingBox(ii + 1, listings.get(ii)));
        }
        return box.makeRoundBottom();
    }

    protected Widget createFeatured (String icon, String title, ListingCard card)
    {
        FlowPanel left = new FlowPanel();
        left.add(MsoyUI.createLabel(card.name, "Name"));
        left.add(MsoyUI.createLabel(_imsgs.itemBy(card.getListedBy().toString()), "Creator"));
        left.add(WidgetUtil.makeShim(5, 5));
        left.add(MsoyUI.createLabel(card.descrip, "Descrip"));

        FlowPanel right = new FlowPanel();
        right.add(new ThumbBox(card.thumbMedia, Pages.SHOP, makeShopArgs(card)));
        right.add(WidgetUtil.makeShim(10, 10));
        right.add(MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.shopBuy(),
                                      Link.createHandler(Pages.SHOP, makeShopArgs(card))));

        SmartTable contents = new SmartTable("FeatListingBox", 0, 0);
        contents.setWidget(0, 0, left);
        contents.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        contents.setWidget(0, 1, right);
        contents.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        contents.setWidget(1, 0, WidgetUtil.makeShim(5, 5), 2);
        MoneyLabel price = new MoneyLabel(card.currency, card.cost);
        price.insert(new InlineLabel(_msgs.shopPrice(), false, false, true), 0);
        contents.setWidget(2, 0, price);
        contents.setWidget(2, 1, new Stars(card.rating, true, false, null));

        String ipath = "/images/shop/icon_" + icon + ".png";
        return new HeaderBox(ipath, title, contents).makeRoundBottom();
    }

    protected static Args makeShopArgs (ListingCard card)
    {
        return Args.compose("l", card.itemType, card.catalogId);
    }

    protected static class TopListingBox extends SmartTable
    {
        public TopListingBox (int rank, ListingCard card) {
            super("TopListingBox", 0, 0);
            setWidget(0, 1, new ThumbBox(card.thumbMedia, MediaDesc.HALF_THUMBNAIL_SIZE,
                                         Pages.SHOP, makeShopArgs(card)), 1, "Thumb");
            setText(1, 0, _msgs.shopRank(""+rank), 1, "Ranking");
            setWidget(1, 1, MsoyUI.createLabel(card.name, "Name")); // requires overflow: hidden
            setText(2, 1, _imsgs.itemBy(card.getListedBy().toString()), 1, "Creator");
        }
    }

    // TODO: this looks out of date
    protected static final int HEADER_HEIGHT = 15 /* gap */ + 59 /* top tags, etc. */;

    protected FlowPanel _contents;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
}
