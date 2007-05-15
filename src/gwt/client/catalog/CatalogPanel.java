//
// $Id$

package client.catalog;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.History;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.gwt.CatalogListing;
import com.threerings.msoy.item.data.gwt.ItemDetail;

import client.item.ItemSearchSortPanel;
import client.item.ItemTypePanel;
import client.item.TagCloud.TagCloudListener;
import client.item.TagCloud;
import client.util.InfoPopup;
import client.shell.Application;
import client.shell.Page;

/**
 * Displays a tabbed panel containing the catalog.
 */
public class CatalogPanel extends VerticalPanel
    implements TabListener, ItemSearchSortPanel.Listener
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 4;

    /** The number of rows of items to display. */
    public static final int ROWS = 3;

    public CatalogPanel ()
    {
        setStyleName("catalogPanel");
        setWidth("100%");

        _typeTabs = new ItemTypePanel("catalog", this);

        int row = 0;
        _items = new PagedGrid(ROWS, COLUMNS) {
            protected void displayPageFromClick (int page) {
                // route our page navigation through the URL
                String args = Page.composeArgs(new int[] { _type, page });
                History.newItem(Application.createLinkToken("catalog", args));
            }
            protected Widget createWidget (Object item) {
                return new ItemContainer((CatalogListing)item, CatalogPanel.this);
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
        _items.setStyleName("catalogContents");

        _searchSortPanel = new ItemSearchSortPanel(
            this,
            new String[] {
                CCatalog.msgs.sortByRating(),
                CCatalog.msgs.sortByListDate(),
                CCatalog.msgs.sortByPriceAsc(),
                CCatalog.msgs.sortByPriceDesc() },
            new byte[] {
                CatalogListing.SORT_BY_RATING,
                CatalogListing.SORT_BY_LIST_DATE,
                CatalogListing.SORT_BY_PRICE_ASC,
                CatalogListing.SORT_BY_PRICE_DESC },
            0);
        _sortBy = CatalogListing.SORT_BY_RATING;

        _items.addToHeader(WidgetUtil.makeShim(15, 1));
        _items.addToHeader(_searchSortPanel);

        _tagCloudContainer = new SimplePanel();
    }

    public Widget getTabs() 
    {
        return _typeTabs;
    }

    public void display (byte itemType, int pageNo, int itemId)
    {
// TODO: sort out displaying items via the URL
//         if (itemId == 0) {
            _page = pageNo;
            if (!_typeTabs.selectTab(itemType)) {
                // we're already on this tab, so refresh our items in order to trigger the
                // appropriate page selection
                refreshItems(false);
            }
            showCatalog();

//         } else {
//             _typeTabs.selectTab(itemType);
//             showListing(new ItemIdent(itemType, itemId));
//         }
    }

    public void showListing (final CatalogListing listing)
    {
        // load up the item details
        CCatalog.itemsvc.loadItemDetail(
            CCatalog.creds, listing.item.getIdent(), new AsyncCallback() {
            public void onSuccess (Object result) {
                clear();
                add(new ListingDetailPanel((ItemDetail)result, listing, CatalogPanel.this));
            }
            public void onFailure (Throwable caught) {
                new InfoPopup(CCatalog.serverError(caught)).show();
            }
        });
    }

    public void showCatalog ()
    {
        if (!_items.isAttached()) {
            clear();
            add(_items);
            add(_tagCloudContainer);
        }
    }

    // from TabListener
    public void onTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        _type = (byte) tabIndex;
        refreshItems(false);
        getTagCloud(false);
    }

    // from TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // always allow any item type selection
        return true;
    }

    // from ItemSearchSortPanel.Listener
    public void search (ItemSearchSortPanel panel)
    {
        _search = panel.search;
        _tag = null;
        _creator = -1;
        getTagCloud(false).setCurrentTag(null);
        refreshItems(true);
    }

    // from ItemSearchSortPanel.Listener
    public void sort (ItemSearchSortPanel panel)
    {
        _sortBy = panel.sortBy;
        refreshItems(true);
    }

    /**
     * Called by the {@link ListingDetailPanel} if the there is a request to browse this creator's
     * items.
     */
    public void browseByCreator (int creatorId, String creatorName) 
    {
        _search = "";
        _searchSortPanel.clearSearchBox();
        _tag = null;
        _creator = creatorId;

        FlowPanel creatorDisplay = new FlowPanel();
        creatorDisplay.add(new InlineLabel(CCatalog.msgs.creatorDisplay() + creatorName + " "));
        InlineLabel clearCreator = new InlineLabel(CCatalog.msgs.clearCurrentCreator());
        clearCreator.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                _creator = -1;
                refreshItems(true);
                // reloads the tag cloud into _tagCloudContainer
                getTagCloud(false);
            }
        });
        creatorDisplay.add(clearCreator);
        creatorDisplay.setStyleName("creatorContents");
        _tagCloudContainer.setWidget(creatorDisplay);

        if (_page == 0) {
            refreshItems(true);
        } else {
            // force ourselves back to page 0
            String args = Page.composeArgs(new int[] { _type, 0 });
            History.newItem(Application.createLinkToken("catalog", args));
        }
    }

    /**
     * Called by the {@link ListingDetailPanel} if the owner requests to delist an item.
     */
    public void itemDelisted (CatalogListing listing)
    {
        _items.removeItem(listing);
    }

    protected void refreshItems (boolean ignoreCache)
    {
        Byte tabKey = new Byte(_type);
        DataModel model = ignoreCache ? null : (DataModel) _models.get(tabKey);
        if (model == null) {
            model = new DataModel() {
                public void doFetchRows (int start, int count, final AsyncCallback callback) {
                    CCatalog.catalogsvc.loadCatalog(CCatalog.getMemberId(), _type, _sortBy, _search,
                                                    _tag, _creator, start, count, 
                                                    new AsyncCallback() {
                        public void onSuccess (Object result) {
                            callback.onSuccess(result);
                        }
                        public void onFailure (Throwable caught) {
                            CCatalog.log("loadCatalog failed", caught);
                            new InfoPopup(CCatalog.serverError(caught)).show();
                        }
                    });
                }
                public void removeItem (Object item) {
                    // currently we do no internal caching, no problem!
                }
            };
        }
        _items.setModel(model, _page);
    }

    protected TagCloud getTagCloud (boolean ignoreCache)
    {
        Byte tabKey = new Byte(_type);
        TagCloud cloud = ignoreCache ? null : (TagCloud) _clouds.get(tabKey);
        if (cloud == null) {
            final TagCloud newCloud = new TagCloud(_type);
            TagCloudListener tagListener = new TagCloudListener() {
                public void tagClicked (String tag) {
                    _search = "";
                    _searchSortPanel.clearSearchBox();
                    _creator = -1;
                    _tag = tag;
                    newCloud.setCurrentTag(tag);
                    refreshItems(true);
                }
            };
            newCloud.setListener(tagListener);
            _clouds.put(tabKey, newCloud);
            cloud = newCloud;
        }
        // if _creator != -1, we are currently displaying a creator in the _tagCloudContainer.
        if (_creator == -1) {
            _tagCloudContainer.setWidget(cloud);
        }
        return cloud;
    }

    protected byte _sortBy,  _type;
    protected String _search, _tag;
    protected int _creator = -1;
    protected int _page;
    protected Map _models = new HashMap();
    protected Map _clouds = new HashMap();

    protected ItemTypePanel _typeTabs;
    protected ItemSearchSortPanel _searchSortPanel;
    protected SimplePanel _tagCloudContainer;
    protected PagedGrid _items;
}
