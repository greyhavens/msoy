//
// $Id$

package client.shop;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.EnterClickAdapter;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.item.data.gwt.ItemDetail;
import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.data.CatalogQuery;
import com.threerings.msoy.web.data.ListingCard;

import client.item.TagCloud;
import client.shell.Application;
import client.shell.Args;
import client.shell.Frame;
import client.shell.Page;
import client.util.FlashClients;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays a tabbed panel containing the catalog.
 */
public class CatalogPanel extends SimplePanel
    implements TagCloud.TagListener
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 4;

    /** An action constant passed to this page. */
    public static final String LISTING_PAGE = "l";

    /** An action constant passed to this page. */
    public static final String ONE_LISTING = "i";

    public CatalogPanel ()
    {
        setStyleName("catalogPanel");

        // create our listings interface
        _listings = new SmartTable("Listings", 0, 0);
        // the blurb and type will be set into (0, 0) and (0, 1) later
        _listings.getFlexCellFormatter().setRowSpan(0, 0, 2);

        _searchBox = new TextBox();
        _searchBox.setVisibleLength(20);
        _searchBox.addStyleName("itemSearchBox");
        ClickListener clickListener = new ClickListener() {
            public void onClick (Widget sender) {
                String query = _searchBox.getText().trim();
                Application.go(Page.SHOP, CShop.composeArgs(_query, null, query, 0));
            }
        };
        _searchBox.addKeyboardListener(new EnterClickAdapter(clickListener));

        Button searchGo = new Button(CShop.msgs.catalogSearch());
        searchGo.addClickListener(clickListener);

        HorizontalPanel search = new HorizontalPanel();
        search.add(_searchBox);
        search.add(WidgetUtil.makeShim(5, 5));
        search.add(searchGo);
        _listings.setWidget(1, 0, search);

        _sortBox = new ListBox();
        for (int ii = 0; ii < SORT_LABELS.length; ii ++) {
            _sortBox.addItem(SORT_LABELS[ii]);
        }
        _sortBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget widget) {
                _query.sortBy = SORT_VALUES[((ListBox)widget).getSelectedIndex()];
                Application.go(Page.SHOP, CShop.composeArgs(_query, 0));
            }
        });

        int rows = Math.max(1, (Window.getClientHeight() - Frame.HEADER_HEIGHT -
                                HEADER_HEIGHT - NAV_BAR_ETC) / BOX_HEIGHT);
        _items = new PagedGrid(rows, COLUMNS) {
            protected void displayPageFromClick (int page) {
                Application.go(Page.SHOP, CShop.composeArgs(_query, page));
            }
            protected Widget createWidget (Object item) {
                return new ListingBox((ListingCard)item);
            }
            protected String getEmptyMessage () {
                String name = CShop.dmsgs.getString("itemType" + _query.itemType);
                if (_query.tag != null) {
                    return CShop.msgs.catalogNoTag(name, _query.tag);
                } else if (_query.search != null) {
                    return CShop.msgs.catalogNoMatch(name, _query.search);
                } else {
                    return CShop.msgs.catalogNoList(name);
                }
            }
            protected void configureNavi (FlexTable controls, int row, int col,
                                          int start, int limit, int total) {
                super.configureNavi(controls, row, col, start, limit, total);
                controls.getFlexCellFormatter().setHorizontalAlignment(
                    row, col, HasAlignment.ALIGN_RIGHT);
            }
            protected void addCustomControls (FlexTable controls) {
                controls.setText(0, 0, CShop.msgs.catalogSortBy());
                controls.getFlexCellFormatter().setStyleName(0, 0, "SortBy");
                controls.setWidget(0, 1, _sortBox);
            }
            protected boolean displayNavi (int items) {
                return true;
            }
        };
        _items.addStyleName("ListingGrid");
        _listings.setWidget(2, 0, _items, 2, null);
        _listings.getFlexCellFormatter().setHeight(2, 0, "100%");
        _listings.getFlexCellFormatter().setVerticalAlignment(2, 0, HasAlignment.ALIGN_TOP);
    }

    public void display (Args args)
    {
        CatalogQuery argQuery = parseArgs(args);
        String mode = args.get(1, LISTING_PAGE);
        if (mode.equals(ONE_LISTING)) {
            int catalogId = args.get(2, 0);
            CShop.catalogsvc.loadListing(
                CShop.ident, argQuery.itemType, catalogId, new MsoyCallback() {
                public void onSuccess (Object result) {
                    setWidget(new ListingDetailPanel((CatalogListing)result, CatalogPanel.this));
                }
            });

        } else if (argQuery.itemType == Item.NOT_A_TYPE) {
            // display a grid of the selectable item types
            VerticalPanel intro = new VerticalPanel();
            intro.add(MsoyUI.createLabel(CShop.msgs.catalogIntro(), "Intro"));
            SmartTable types = new SmartTable("Types", 0, 10);
            for (int ii = 0; ii < Item.TYPES.length; ii++) {
                final byte type = Item.TYPES[ii];
                ClickListener onClick = new ClickListener() {
                    public void onClick (Widget sender) {
                        Application.go(Page.SHOP, ""+type);
                    }
                };
                SmartTable ttable = new SmartTable("Type", 0, 2);
                String tpath = Item.getDefaultThumbnailMediaFor(type).getMediaPath();
                ttable.setWidget(0, 0, MsoyUI.createActionImage(tpath, onClick), 1, "Icon");
                ttable.getFlexCellFormatter().setRowSpan(0, 0, 2);
                String tname = CShop.dmsgs.getString("pItemType" + type);
                ttable.setWidget(0, 1, MsoyUI.createActionLabel(tname, onClick), 1, "Name");
                String tblurb = CShop.dmsgs.getString("catIntro" + type);
                ttable.setText(1, 0, tblurb, 1, "Blurb");
                types.setWidget(ii / 2, ii % 2, ttable);
            }
            intro.add(types);

            setWidget(createCategorizedPage(Item.NOT_A_TYPE, intro, null, null));
            Frame.setTitle(CShop.msgs.catalogTitle());

        } else /* mode.equals(LISTING_PAGE) */ {
            _query = argQuery;

            // TODO: add logo image
            String tname = CShop.dmsgs.getString("pItemType" + _query.itemType);
            _listings.setText(0, 1, tname, 1, "Type");

            // configure our filter interface
            _searchBox.setText(_query.search == null ? "" : _query.search);
            for (int ii = 0; ii < SORT_VALUES.length; ii++) {
                if (SORT_VALUES[ii] == _query.sortBy) {
                    _sortBox.setSelectedIndex(ii);
                }
            }

            if (_query.search != null) {
                setFilteredBy(CShop.msgs.catalogSearchFilter(_query.search));
            } else if (_query.tag != null) {
                setFilteredBy(CShop.msgs.catalogTagFilter(_query.tag));
            } else if (_query.creatorId != 0) {
                setFilteredBy(CShop.msgs.catalogCreatorFilter());
            } else {
                setFilteredBy(null);
            }

            // grab our data model and display it
            CatalogDataModel model = (CatalogDataModel)_models.get(_query);
            if (model == null) {
                _models.put(_query, model = new CatalogDataModel(_query));
            }
            CatalogDataModel current = (CatalogDataModel)_items.getModel();
            if (current != null && current.getType() != model.getType()) {
                // clear the display when we switching item types so that we don't see items of the
                // old type while items of the new type are loading
                _items.clear();
            }
            _items.setModel(model, args.get(4, 0));

            // set up our page title
            Frame.setTitle(CShop.dmsgs.getString("pItemType" + _query.itemType));

            // configure the appropriate tab cloud
            Byte tabKey = new Byte(_query.itemType);
            TagCloud cloud = (TagCloud) _clouds.get(tabKey);
            if (cloud == null) {
                _clouds.put(tabKey, cloud = new TagCloud(_query.itemType, TAG_COUNT, this));
            }

            setWidget(createCategorizedPage(_query.itemType, _listings, cloud, "#FFFFFF"));
        }
    }

    /**
     * Called by the {@link ListingDetailPanel} if the owner requests to delist an item.
     */
    public void itemDelisted (CatalogListing listing)
    {
        _items.removeItem(listing);
    }

    // from interface TagCloud.TagListener
    public void tagClicked (String tag)
    {
        Application.go(Page.SHOP, CShop.composeArgs(_query, tag, null, 0));
    }

    protected Widget createCategorizedPage (
        byte type, Widget contents, Widget sideExtra, String bgcolor)
    {
        HorizontalPanel page = new HorizontalPanel();
        page.setStyleName("WithCatNav");
        page.setVerticalAlignment(HasAlignment.ALIGN_TOP);

        SmartTable sidebar = new SmartTable("SideBar", 0, 0);
        sidebar.addWidget(new Image("/images/shop/sidebar_top.png"), 1, null);
        sidebar.addText(CShop.msgs.catalogCats(), 1, "Title");
        sidebar.addWidget(_navibar = new NaviPanel(type), 1, "Middle");
        if (sideExtra != null) {
            sidebar.addWidget(sideExtra, 1, "Middle");
        }
        sidebar.addWidget(new Image("/images/shop/sidebar_bottom.png"), 1, null);
        page.add(sidebar);

        page.add(WidgetUtil.makeShim(10, 10));
        page.add(contents);
        page.setCellWidth(contents, "100%");
        if (bgcolor != null) {
            DOM.setStyleAttribute(DOM.getParent(contents.getElement()), "background", bgcolor);
        }
        page.add(WidgetUtil.makeShim(10, 10));

        return page;
    }

    protected void itemPurchased (Item item)
    {
        // report to the client that we generated a tutorial event
        if (item.getType() == Item.DECOR) {
            FlashClients.tutorialEvent("decorBought");
        } else if (item.getType() == Item.FURNITURE) {
            FlashClients.tutorialEvent("furniBought");
        } else if (item.getType() == Item.AVATAR) {
            FlashClients.tutorialEvent("avatarBought");
        }

        // display the "you bought an item" UI
        setWidget(new BoughtItemPanel(item));
    }

    protected void setFilteredBy (String text)
    {
        if (text == null) {
            String blurb = CShop.dmsgs.getString("catIntro" + _query.itemType);
            _listings.setText(0, 0, blurb, 1, "Blurb");

        } else {
            FlowPanel filter = new FlowPanel();
            filter.add(new InlineLabel(text, false, false, true));
            CatalogQuery query = new CatalogQuery();
            query.itemType = _query.itemType;
            filter.add(Application.createLink(CShop.msgs.catalogClearFilter(),
                                              Page.SHOP, CShop.composeArgs(query, 0)));
            _listings.setWidget(0, 0, filter, 1, "Blurb");
        }
    }

    protected CatalogQuery parseArgs (Args args)
    {
        CatalogQuery query = new CatalogQuery();
        query.itemType = (byte)args.get(0,  query.itemType);
        query.sortBy = (byte)args.get(2, query.sortBy);
        String action = args.get(3, "");
        if (action.startsWith("s")) {
            query.search = action.substring(1);
        } else if (action.startsWith("t")) {
            query.tag = action.substring(1);
        } else if (action.startsWith("c")) {
            try {
                query.creatorId = Integer.parseInt(action.substring(1));
            } catch (Exception e) {
                // oh well
            }
        }
        return query;
    }

    protected static class NaviPanel extends FlowPanel
    {
        public NaviPanel (byte seltype) {
            setStyleName("NaviPanel");
            for (int ii = 0; ii < Item.TYPES.length; ii++) {
                byte type = Item.TYPES[ii];
                String name = CShop.dmsgs.getString("pItemType" + type);
                if (seltype == type) {
                    add(MsoyUI.createLabel(name, "Selected"));
                } else {
                    Widget link = Application.createLink(name, Page.SHOP, ""+type);
                    link.removeStyleName("inline");
                    add(link);
                }
            }
        }
    }

    protected static class CatalogDataModel implements DataModel
    {
        public CatalogDataModel (CatalogQuery query) {
            _query = query;
        }

        public byte getType () {
            return _query.itemType;
        }

        public int getItemCount () {
            return _listingCount;
        }

        public void doFetchRows (int start, int count, final AsyncCallback callback) {
            CShop.catalogsvc.loadCatalog(
                CShop.ident, _query, start, count, _listingCount == -1, new MsoyCallback() {
                public void onSuccess (Object result) {
                    _result = (CatalogService.CatalogResult)result;
                    if (_listingCount == -1) {
                        _listingCount = _result.listingCount;
                    }
                    callback.onSuccess(_result.listings);
                }
            });
        }

        public void removeItem (Object item) {
            // currently we do no internal caching, no problem!
        }

        protected CatalogQuery _query;
        protected CatalogService.CatalogResult _result;
        protected int _listingCount = -1;
    }

    protected CatalogQuery _query;
    protected Map _models = new HashMap(); /* Filter, CatalogDataModel */
    protected Map _clouds = new HashMap(); /* Byte, TagCloud */

    protected NaviPanel _navibar;
    protected SmartTable _listings;
    protected TextBox _searchBox;
    protected ListBox _sortBox;
    protected PagedGrid _items;

    protected static final int TAG_COUNT = 10;

    protected static final int HEADER_HEIGHT = 15 /* gap */ + 59 /* top tags, etc. */;
    protected static final int NAV_BAR_ETC = 15 /* gap */ + 20 /* bar height */ + 10 /* gap */;
    protected static final int BOX_HEIGHT = MediaDesc.THUMBNAIL_HEIGHT + 20 /* border */ +
        15 /* name */ + 20 /* creator */ + 20 /* rating/price */;

    protected static final String[] SORT_LABELS = new String[] {
        CShop.msgs.sortByRating(),
        CShop.msgs.sortByListDate(),
        CShop.msgs.sortByPriceAsc(),
        CShop.msgs.sortByPriceDesc(),
        CShop.msgs.sortByPurchases(),
    };
    protected static final byte[] SORT_VALUES = new byte[] {
        CatalogQuery.SORT_BY_RATING,
        CatalogQuery.SORT_BY_LIST_DATE,
        CatalogQuery.SORT_BY_PRICE_ASC,
        CatalogQuery.SORT_BY_PRICE_DESC,
        CatalogQuery.SORT_BY_PURCHASES,
    };
}
