//
// $Id$

package client.shop;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;

import com.threerings.orth.data.MediaDescSize;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.item.ItemMessages;
import client.item.SideBar;
import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.SearchBox;
import client.ui.ThumbBox;
import client.util.Link;

/**
 * Displays the main catalog landing page.
 */
public class ShopPanel extends FlowPanel
{
    public ShopPanel (GroupName theme)
    {
        setStyleName("shopPanel");
        _theme = theme;
        int themeId = (theme != null) ? theme.getGroupId() : 0;

        // prepare the search box
        HorizontalPanel search = new HorizontalPanel();
        search.setStyleName("Search");
        search.setSpacing(5);
        search.setVerticalAlignment(HasAlignment.ALIGN_MIDDLE);
        search.add(MsoyUI.createLabel(_msgs.shopSearch(), "SearchTitle"));
        final ListBox searchTypes = new ListBox();
        MsoyItemType[] items = CShell.getClientMode().isMinimal() ?
            MsoyItemType.DJ_ITEMS : MsoyItemType.SHOP_ITEMS;
        for (MsoyItemType searchType : items) {
            searchTypes.addItem(_dmsgs.xlateItemsType(searchType), searchType.toByte() + "");
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
        String title = (theme != null) ? _msgs.themeShopMarquee() : _msgs.shopMarquee();
        add(MsoyUI.createLabel(title, "ShopTitle"));
        add(search);

        // display categories on the left, content on the right
        HorizontalPanel row = new HorizontalPanel();
        row.setVerticalAlignment(HorizontalPanel.ALIGN_TOP);

        // display a sidebar with a linker that knows which theme we're in
        CatalogQuery linkerQuery = new CatalogQuery();
        linkerQuery.themeGroupId = themeId;
        SideBar sidebar = new SideBar(new CatalogQueryLinker(linkerQuery), items, null);
        sidebar.add(MsoyUI.createImage("/images/shop/shop_bag.png", "Bag"));
        row.add(sidebar);

        row.add(_contents = MsoyUI.createFlowPanel("TopContents"));
        _contents.add(MsoyUI.createHTML(_msgs.shopIntro(), "Intro"));
        add(row);
        add(WidgetUtil.makeShim(15, 15));

        ListingGrid grid = new ListingGrid(HEADER_HEIGHT) {
            @Override protected String getEmptyMessage () {
                GroupName theme = ((CatalogModels.Jumble) _model).theme;
                if (theme != null) {
                    return _msgs.themeShopNoItems();
                }
                return _msgs.shopNoFavorites();
            }
            @Override protected void addCustomControls (FlexTable controls) {
                _controls.setWidget(0, 0, _controlContainer = new FlowPanel());
            }
            @Override protected void displayResults (int start, int count, List<ListingCard> list) {
                _controlContainer.clear();
                GroupName theme = ((CatalogModels.Jumble) _model).theme;
                if (theme != null) {
                    _controlContainer.add(MsoyUI.createLabel(
                        _msgs.themeShopHeader(theme.toString()), null));
                } else {
                    _controlContainer.add(
                        Link.create(_msgs.shopClubPicks(), Pages.BILLING, "subscribe"));
                }

                super.displayResults(start, count, list);
            }
            protected FlowPanel _controlContainer;
        };

        grid.setModel(new CatalogModels.Jumble(theme), 0);
        _contents.add(grid);
    }

    protected static Args makeShopArgs (ListingCard card)
    {
        return Args.compose("l", card.itemType, card.catalogId);
    }

    protected static class TopListingBox extends SmartTable
    {
        public TopListingBox (int rank, ListingCard card) {
            super("TopListingBox", 0, 0);
            setWidget(0, 1, new ThumbBox(card.thumbMedia, MediaDescSize.HALF_THUMBNAIL_SIZE,
                                         Pages.SHOP, makeShopArgs(card)), 1, "Thumb");
            setText(1, 0, _msgs.shopRank(""+rank), 1, "Ranking");
            setWidget(1, 1, MsoyUI.createLabel(card.name, "Name")); // requires overflow: hidden
            setText(2, 1, _imsgs.itemBy(card.getListedBy().toString()), 1, "Creator");
        }
    }

    // TODO: this looks out of date
    protected static final int HEADER_HEIGHT = 15 /* gap */ + 59 /* top tags, etc. */;

    protected GroupName _theme;
    protected FlowPanel _contents;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
    protected static final ItemMessages _imsgs = GWT.create(ItemMessages.class);
    protected static final CatalogServiceAsync _catalogsvc = GWT.create(CatalogService.class);
}
