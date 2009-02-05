//
// $Id$

package client.shop;

import java.util.List;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.gwt.ShopData;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.msoy.data.all.MediaDesc;

import client.shell.DynamicLookup;
import client.ui.HeaderBox;
import client.ui.MsoyUI;
import client.ui.NowLoadingWidget;
import client.ui.PriceLabel;
import client.ui.SearchBox;
import client.ui.Stars;
import client.ui.ThumbBox;

import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;


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

        _nowLoading = new NowLoadingWidget();
        _nowLoading.center();

        // prepare the search box
        HorizontalPanel search = new HorizontalPanel();
        search.setStyleName("Search");
        search.setSpacing(5);
        search.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
        search.add(MsoyUI.createLabel(_msgs.shopSearch(), "SearchTitle"));
        final ListBox searchTypes = new ListBox();
        for (byte searchType : Item.STUFF_TYPES) {
            searchTypes.addItem(_dmsgs.xlate("pItemType" + searchType), searchType + "");
        }
        search.add(searchTypes);
        SearchBox searchBox = new SearchBox(new SearchBox.Listener() {
            public void search (String query) {
                String type = searchTypes.getValue(searchTypes.getSelectedIndex());
                Link.go(Pages.SHOP, Args.compose(type, CatalogQuery.SORT_BY_NEW_AND_HOT, "s"
                    + query));
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

        // now load up our shop data
        _catalogsvc.loadShopData(new MsoyCallback<ShopData>() {
            public void onSuccess (ShopData data) {
                init(data);
            }
        });
    }

    protected void init (final ShopData data)
    {
        _nowLoading.finishing(new Timer() {
            public void run () {
                SmartTable boxes = new SmartTable(0, 0);
                boxes.setWidget(0, 0, createTop("avatar", _msgs.shopTopAvatars(), data.topAvatars));
                boxes.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
                boxes.getFlexCellFormatter().setRowSpan(0, 0, 3);
                boxes.setWidget(0, 1, WidgetUtil.makeShim(10, 10));
                boxes.getFlexCellFormatter().setRowSpan(0, 1, 3);
                if (data.featuredPet != null) {
                    boxes.setWidget(0, 2,
                        createFeatured("pet", _msgs.shopFeatPet(), data.featuredPet));
                    boxes.getFlexCellFormatter().setVerticalAlignment(0, 2, HasAlignment.ALIGN_TOP);
                }
                boxes.setWidget(1, 0, WidgetUtil.makeShim(10, 10));
                if (data.featuredToy != null) {
                    boxes.setWidget(2, 0,
                        createFeatured("toy", _msgs.shopFeatToy(), data.featuredToy));
                    boxes.getFlexCellFormatter().setVerticalAlignment(2, 2, HasAlignment.ALIGN_TOP);
                }
                boxes.setWidget(0, 3, WidgetUtil.makeShim(10, 10));
                boxes.getFlexCellFormatter().setRowSpan(0, 3, 3);
                boxes.setWidget(0, 4,
                    createTop("furni", _msgs.shopTopFurniture(), data.topFurniture));
                boxes.getFlexCellFormatter().setVerticalAlignment(0, 4, HasAlignment.ALIGN_TOP);
                boxes.getFlexCellFormatter().setRowSpan(0, 4, 3);

                _contents.add(boxes);
                _nowLoading.hide();
            }
        });
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
        left.add(MsoyUI.createLabel(_imsgs.itemBy(card.creator.toString()), "Creator"));
        left.add(WidgetUtil.makeShim(5, 5));
        left.add(MsoyUI.createLabel(card.descrip, "Descrip"));

        FlowPanel right = new FlowPanel();
        right.add(new ThumbBox(card.thumbMedia, Pages.SHOP, makeShopArgs(card)));
        right.add(WidgetUtil.makeShim(10, 10));
        right.add(MsoyUI.createButton(MsoyUI.SHORT_THIN, _msgs.shopBuy(),
                                      Link.createListener(Pages.SHOP, makeShopArgs(card))));

        SmartTable contents = new SmartTable("FeatListingBox", 0, 0);
        contents.setWidget(0, 0, left);
        contents.getFlexCellFormatter().setVerticalAlignment(0, 0, HasAlignment.ALIGN_TOP);
        contents.setWidget(0, 1, right);
        contents.getFlexCellFormatter().setVerticalAlignment(0, 1, HasAlignment.ALIGN_TOP);
        contents.setWidget(1, 0, WidgetUtil.makeShim(5, 5), 2, null);
        contents.setWidget(2, 0, new PriceLabel(card.currency, card.cost));
        contents.setWidget(2, 1, new Stars(card.rating, true, false, null));

        String ipath = "/images/shop/icon_" + icon + ".png";
        return new HeaderBox(ipath, title, contents).makeRoundBottom();
    }

    protected static String makeShopArgs (ListingCard card)
    {
        return Args.compose("l", "" + card.itemType, "" + card.catalogId);
    }

    protected static class TopListingBox extends SmartTable
    {
        public TopListingBox (int rank, ListingCard card) {
            super("TopListingBox", 0, 0);
            setWidget(0, 1, new ThumbBox(card.thumbMedia, MediaDesc.HALF_THUMBNAIL_SIZE,
                                         Pages.SHOP, makeShopArgs(card)), 1, "Thumb");
            setText(1, 0, _msgs.shopRank(""+rank), 1, "Ranking");
            setWidget(1, 1, MsoyUI.createLabel(card.name, "Name")); // requires overflow: hidden
            setText(2, 1, _imsgs.itemBy(card.creator.toString()), 1, "Creator");
        }
    }

    protected FlowPanel _contents;
    protected NowLoadingWidget _nowLoading;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final CatalogServiceAsync _catalogsvc = (CatalogServiceAsync)
        ServiceUtil.bind(GWT.create(CatalogService.class), CatalogService.ENTRY_POINT);
}
