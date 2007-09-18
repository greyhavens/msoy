//
// $Id$

package client.catalog;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.item.data.gwt.ItemDetail;
import com.threerings.msoy.web.client.CatalogService;

import client.item.ItemSearchSortPanel;
import client.item.ItemTypePanel;
import client.item.TagCloud;
import client.shell.Application;
import client.shell.Page;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays a tabbed panel containing the catalog.
 */
public class CatalogPanel extends VerticalPanel
    implements TabListener, ItemSearchSortPanel.Listener, TagCloud.TagListener
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 3;

    public CatalogPanel ()
    {
        setStyleName("catalogPanel");
        setWidth("100%");

        _typeTabs = new ItemTypePanel("catalog", this);

        int rows = (Window.getClientHeight() - Application.HEADER_HEIGHT -
                    HEADER_HEIGHT - NAV_BAR_ETC) / BOX_HEIGHT;
        _items = new PagedGrid(rows, COLUMNS) {
            protected void displayPageFromClick (int page) {
// TODO: route our page navigation through the URL
//                 String args = Page.composeArgs(new int[] { _type, page });
//                 History.newItem(Application.createLinkToken("catalog", args));
                CatalogPanel.this._page = page;
                refreshItems(false);
            }
            protected Widget createWidget (Object item) {
                return new ListingContainer((CatalogListing)item, CatalogPanel.this);
            }
            protected String getEmptyMessage () {
                String name = Item.getTypeName(_type);
                if (_tag != null) {
                    return CCatalog.msgs.catalogNoTag(name, _tag);
                } else if (_search != null) {
                    return CCatalog.msgs.catalogNoMatch(name, _search);
                } else {
                    return CCatalog.msgs.catalogNoList(name);
                }
            }
        };
        _items.addStyleName("catalogContents");

        _header = new FlexTable();
        _header.setCellPadding(0);
        _header.setCellSpacing(10);
        _header.getFlexCellFormatter().setRowSpan(0, 0, 2);

        _searchSortPanel = new ItemSearchSortPanel(
            this,
            new String[] {
                CCatalog.msgs.sortByRating(),
                CCatalog.msgs.sortByListDate(),
                CCatalog.msgs.sortByPriceAsc(),
                CCatalog.msgs.sortByPriceDesc(),
                CCatalog.msgs.sortByPurchases(), },
            new byte[] {
                CatalogListing.SORT_BY_RATING,
                CatalogListing.SORT_BY_LIST_DATE,
                CatalogListing.SORT_BY_PRICE_ASC,
                CatalogListing.SORT_BY_PRICE_DESC,
                CatalogListing.SORT_BY_PURCHASES, },
            0); // index of CatalogListing.SORT_BY_RATING
        _sortBy = CatalogListing.SORT_BY_RATING;
        _header.setWidget(0, 1, _searchSortPanel);
        _header.setText(1, 0, CCatalog.msgs.catalogNoFilter());
    }

    public Widget getTabs() 
    {
        return _typeTabs;
    }

    public void display (String args)
    {
        // TODO: route everything through the args (search, tag, creator, sortBy)
        int[] avals = Page.splitArgs(args);
        byte type = (avals[0] == 0) ? Item.AVATAR : (byte)avals[0];
        if (!_typeTabs.selectTab(type)) {
            // we're already on this tab, so refresh our items in order to trigger the
            // appropriate page selection
            refreshItems(false);
        }
        showCatalog();
    }

    public void showListing (final CatalogListing listing)
    {
        // load up the item details
        CCatalog.itemsvc.loadItemDetail(
            CCatalog.ident, listing.item.getIdent(), new AsyncCallback() {
            public void onSuccess (Object result) {
                clear();
                add(new ListingDetailPanel((ItemDetail)result, listing, CatalogPanel.this));
            }
            public void onFailure (Throwable caught) {
                MsoyUI.error(CCatalog.serverError(caught));
            }
        });
    }

    public void showCatalog ()
    {
        if (!_items.isAttached()) {
            clear();
            add(_header);
            add(_items);
        }
    }

    // from TabListener
    public void onTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        _type = (byte) tabIndex;
        _page = 0;
        refreshItems(true);

        Byte tabKey = new Byte(_type);
        TagCloud cloud = (TagCloud) _clouds.get(tabKey);
        if (cloud == null) {
            _clouds.put(tabKey, cloud = new TagCloud(_type, this));
        }
        _header.setWidget(0, 0, cloud);
    }

    // from TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // always allow any item type selection
        return true;
    }

    // from ItemSearchSortPanel.Listener
    public void search (String query)
    {
        clearFilters(false);
        _search = query;
        setFilteredBy(CCatalog.msgs.catalogSearchFilter(_search));
        refreshItems(true);
    }

    // from ItemSearchSortPanel.Listener
    public void sort (byte sortBy)
    {
        _sortBy = sortBy;
        refreshItems(true);
    }

    /**
     * Called by the {@link ListingDetailPanel} if the there is a request to browse this creator's
     * items.
     */
    public void browseByCreator (int creatorId, String creatorName) 
    {
        clearFilters(false);
        _creator = creatorId;
        refreshItems(true);
        setFilteredBy(CCatalog.msgs.catalogCreatorFilter(creatorName));
    }

    /**
     * Called by the {@link ListingDetailPanel} if the owner requests to delist an item.
     */
    public void itemDelisted (CatalogListing listing)
    {
        _items.removeItem(listing);
    }

    /**
     * Clears any tag, creator or search filters currently in effect.
     */
    public void clearFilters (boolean reload)
    {
        _page = 0;
        _search = "";
        _creator = -1;
        _tag = null;
        if (reload) {
            refreshItems(true);
            setFilteredBy(null);
        }
    }

    // from interface TagCloud.TagListener
    public void tagClicked (String tag)
    {
        clearFilters(false);
        _tag = tag;
        refreshItems(true);
        setFilteredBy(CCatalog.msgs.catalogTagFilter(tag));
    }

    protected void refreshItems (boolean ignoreCache)
    {
        Byte tabKey = new Byte(_type);
        DataModel model = ignoreCache ? null : (DataModel) _models.get(tabKey);
        if (model == null) {
            model = new DataModel() {
                public int getItemCount () {
                    return _listingCount;
                }
                public void doFetchRows (int start, int count, final AsyncCallback callback) {
                    CCatalog.catalogsvc.loadCatalog(
                        CCatalog.getMemberId(), _type, _sortBy, _search,
                        _tag, _creator, start, count, _listingCount == -1, new AsyncCallback() {
                        public void onSuccess (Object result) {
                            CatalogService.CatalogResult cr = (CatalogService.CatalogResult)result;
                            if (_listingCount == -1) {
                                _listingCount = cr.listingCount;
                            }
                            callback.onSuccess(cr.listings);
                        }
                        public void onFailure (Throwable caught) {
                            CCatalog.log("loadCatalog failed", caught);
                            MsoyUI.error(CCatalog.serverError(caught));
                        }
                    });
                }
                public void removeItem (Object item) {
                    // currently we do no internal caching, no problem!
                }
                protected int _listingCount = -1;
            };
        }
        _items.setModel(model, _page);
    }

    protected void setFilteredBy (String text)
    {
        if (text == null) {
            _header.setText(1, 0, CCatalog.msgs.catalogNoFilter());
            return;
        }

        RowPanel filter = new RowPanel();
        filter.add(new Label(text));
        String clear = CCatalog.msgs.catalogClearFilter();
        filter.add(MsoyUI.createActionLabel(clear, new ClickListener() {
            public void onClick (Widget widget) {
                clearFilters(true);
            }
        }));
        _header.setWidget(1, 0, filter);
    }

    protected byte _sortBy,  _type;
    protected String _search, _tag;
    protected int _creator = -1;
    protected int _page;
    protected Map _models = new HashMap();
    protected Map _clouds = new HashMap();

    protected FlexTable _header;
    protected ItemTypePanel _typeTabs;
    protected ItemSearchSortPanel _searchSortPanel;
    protected PagedGrid _items;

    protected static final int HEADER_HEIGHT = 15 /* gap */ + 59 /* top tags, etc. */;
    protected static final int NAV_BAR_ETC = 15 /* gap */ + 20 /* bar height */ + 10 /* gap */;
    protected static final int BOX_HEIGHT = MediaDesc.THUMBNAIL_HEIGHT/2 + 15 /* gap */;
}
