//
// $Id$

package client.catalog;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.item.data.gwt.ItemDetail;
import com.threerings.msoy.web.client.CatalogService;
import com.threerings.msoy.web.data.CatalogQuery;

import client.item.ItemSearchSortPanel;
import client.item.ItemTypePanel;
import client.item.TagCloud;
import client.shell.Application;
import client.shell.Args;
import client.shell.Page;
import client.util.MsoyCallback;
import client.util.MsoyUI;
import client.util.RowPanel;

/**
 * Displays a tabbed panel containing the catalog.
 */
public class CatalogPanel extends VerticalPanel
    implements ItemSearchSortPanel.Listener, TagCloud.TagListener
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 3;

    /** An action constant passed to this page. */
    public static final String LISTING_PAGE = "l";

    /** An action constant passed to this page. */
    public static final String ONE_LISTING = "i";

    public CatalogPanel (ItemTypePanel typeTabs)
    {
        setStyleName("catalogPanel");
        setWidth("100%");

        _typeTabs = typeTabs;

        int rows = (Window.getClientHeight() - Application.HEADER_HEIGHT -
                    HEADER_HEIGHT - NAV_BAR_ETC) / BOX_HEIGHT;
        _items = new PagedGrid(rows, COLUMNS) {
            protected void displayPageFromClick (int page) {
                Application.go(Page.CATALOG, composeArgs(_query, page));
            }
            protected Widget createWidget (Object item) {
                return new ListingContainer((CatalogListing)item);
            }
            protected String getEmptyMessage () {
                String name = CCatalog.dmsgs.getString("itemType" + _query.itemType);
                if (_query.tag != null) {
                    return CCatalog.msgs.catalogNoTag(name, _query.tag);
                } else if (_query.search != null) {
                    return CCatalog.msgs.catalogNoMatch(name, _query.search);
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
                CatalogListing.SORT_BY_PURCHASES, });
        _header.setWidget(0, 1, _searchSortPanel);
        _header.setText(1, 0, CCatalog.msgs.catalogNoFilter());
    }

    public void display (Args args)
    {
        CatalogQuery argQuery = parseArgs(args);
        _typeTabs.selectTab(argQuery.itemType);

        String mode = args.get(1, LISTING_PAGE);
        if (mode.equals(ONE_LISTING)) {
            int catalogId = args.get(2, 0);

            // search for the listing in our resolved data models
            CatalogListing listing = null;
            for (Iterator iter = _models.keySet().iterator(); iter.hasNext(); ) {
                CatalogQuery query = (CatalogQuery)iter.next();
                if (query.itemType != argQuery.itemType) {
                    continue;
                }
                CatalogDataModel model = (CatalogDataModel)_models.get(query);
                if ((listing = model.getListing(catalogId)) != null) {
                    break;
                }
            }

            // if we found the listing, display it, otherwise load it from the server
            MsoyCallback gotListing = new MsoyCallback() {
                public void onSuccess (Object result) {
                    showListing((CatalogListing)result);
                }
            };
            if (listing == null) {
                CCatalog.catalogsvc.loadListing(
                    CCatalog.ident, argQuery.itemType, catalogId, gotListing);
            } else {
                gotListing.onSuccess(listing);
            }

        } else /* mode.equals(LISTING_PAGE) */ {
            _query = argQuery;

            // configure our filter interface
            _searchSortPanel.setSearch(_query.search == null ? "" : _query.search);
            _searchSortPanel.setSelectedSort(_query.sortBy);
            if (_query.search != null) {
                setFilteredBy(CCatalog.msgs.catalogSearchFilter(_query.search));
            } else if (_query.tag != null) {
                setFilteredBy(CCatalog.msgs.catalogTagFilter(_query.tag));
            } else if (_query.creatorId != 0) {
                setFilteredBy(CCatalog.msgs.catalogCreatorFilter());
            } else {
                setFilteredBy(null);
            }

            // grab our data model and display it
            DataModel model = (DataModel) _models.get(_query);
            if (model == null) {
                _models.put(_query, model = new CatalogDataModel(_query));
            }
            _items.setModel(model, args.get(4, 0)); // args 4 is page
            if (!_items.isAttached()) {
                clear();
                add(_header);
                add(_items);
            }

            // configure the appropriate tab cloud
            Byte tabKey = new Byte(_query.itemType);
            TagCloud cloud = (TagCloud) _clouds.get(tabKey);
            if (cloud == null) {
                _clouds.put(tabKey, cloud = new TagCloud(_query.itemType, this));
            }
            _header.setWidget(0, 0, cloud);
        }
    }

    /**
     * Called by the {@link ListingDetailPanel} if the there is a request to browse this creator's
     * items.
     */
    public void browseByCreator (int creatorId, String creatorName)
    {
        Application.go(Page.CATALOG, composeArgs(_query, null, null, creatorId));
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
    public void clearFilters ()
    {
        CatalogQuery query = new CatalogQuery();
        query.itemType = _query.itemType;
        Application.go(Page.CATALOG, composeArgs(query, 0));
    }

    // from ItemSearchSortPanel.Listener
    public void search (String query)
    {
        Application.go(Page.CATALOG, composeArgs(_query, null, query, 0));
    }

    // from ItemSearchSortPanel.Listener
    public void sort (byte sortBy)
    {
        _query.sortBy = sortBy;
        Application.go(Page.CATALOG, composeArgs(_query, 0));
    }

    // from interface TagCloud.TagListener
    public void tagClicked (String tag)
    {
        Application.go(Page.CATALOG, composeArgs(_query, tag, null, 0));
    }

    protected void showListing (final CatalogListing listing)
    {
        // load up the item details
        CCatalog.itemsvc.loadItemDetail(CCatalog.ident, listing.item.getIdent(), new MsoyCallback() {
            public void onSuccess (Object result) {
                clear();
                add(new ListingDetailPanel((ItemDetail)result, listing, CatalogPanel.this));
            }
        });
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
                clearFilters();
            }
        }));
        _header.setWidget(1, 0, filter);
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

    protected String composeArgs (CatalogQuery query, String tag, String search, int creatorId)
    {
        query.tag = tag;
        query.search = search;
        query.creatorId = creatorId;
        return composeArgs(query, 0);
    }

    protected String composeArgs (CatalogQuery query, int page)
    {
        ArrayList args = new ArrayList();
        args.add(new Byte(query.itemType));
        args.add(LISTING_PAGE);
        args.add(new Byte(query.sortBy));
        if (query.tag != null) {
            args.add("t" + query.tag);
        } else if (query.search != null) {
            args.add("s" + query.search);
        } else if (query.creatorId != 0) {
            args.add("c" + query.creatorId);
        } else {
            args.add("");
        }
        if (page > 0) {
            args.add(new Integer(page));
        }
        return Args.compose(args);
    }

    protected static class CatalogDataModel implements DataModel
    {
        public CatalogDataModel (CatalogQuery query) {
            _query = query;
        }

        public int getItemCount () {
            return _listingCount;
        }

        public CatalogListing getListing (int catalogId) {
            int count = (_result == null) ? 0 : _result.listings.size();
            for (int ii = 0; ii < count; ii++) {
                CatalogListing listing = (CatalogListing)_result.listings.get(ii);
                if (listing.catalogId == catalogId) {
                    return listing;
                }
            }
            return null;
        }

        public void doFetchRows (int start, int count, final AsyncCallback callback) {
            CCatalog.catalogsvc.loadCatalog(
                CCatalog.ident, _query, start, count, _listingCount == -1, new MsoyCallback() {
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

    protected FlexTable _header;
    protected ItemTypePanel _typeTabs;
    protected ItemSearchSortPanel _searchSortPanel;
    protected PagedGrid _items;

    protected static final int HEADER_HEIGHT = 15 /* gap */ + 59 /* top tags, etc. */;
    protected static final int NAV_BAR_ETC = 15 /* gap */ + 20 /* bar height */ + 10 /* gap */;
    protected static final int BOX_HEIGHT = MediaDesc.THUMBNAIL_HEIGHT + 15 /* gap */;
}
