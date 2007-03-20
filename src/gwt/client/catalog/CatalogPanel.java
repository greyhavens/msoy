//
// $Id$

package client.catalog;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;

import client.item.ItemSearchSortPanel;
import client.item.ItemTypePanel;
import client.item.TagCloud;
import client.item.TagCloud.TagCloudListener;

/**
 * Displays a tabbed panel containing the catalog.
 */
public class CatalogPanel extends FlexTable
    implements TabListener, ItemSearchSortPanel.Listener
{
    /** The number of columns of items to display. */
    public static final int COLUMNS = 4;

    /** The number of rows of items to display. */
    public static final int ROWS = 3;

    public CatalogPanel ()
    {
        setStyleName("catalogPanel");
        setCellPadding(0);
        setCellSpacing(0);
        setWidth("100%");

        int row = 0;
        setWidget(row, 0, new Label(CCatalog.msgs.catalogTitle()));
        getFlexCellFormatter().setStyleName(row, 0, "Title");

        setWidget(row, 1, _typeTabs = new ItemTypePanel("catalog", this));
        getFlexCellFormatter().setStyleName(row++, 1, "Tabs");

        _items = new PagedGrid(ROWS, COLUMNS) {
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
        setWidget(row, 0, _items);
        getFlexCellFormatter().setColSpan(row++, 0, 2);

        _searchSortPanel = new ItemSearchSortPanel(
            this,
            new String[] {
                CCatalog.msgs.sortByRating(),
                CCatalog.msgs.sortByListDate(),
                CCatalog.msgs.sortByPrice() },
            new byte[] {
                CatalogListing.SORT_BY_RATING,
                CatalogListing.SORT_BY_LIST_DATE,
                CatalogListing.SORT_BY_PRICE },
            0);
        _sortBy = CatalogListing.SORT_BY_RATING;

        _items.addToHeader(WidgetUtil.makeShim(15, 1));
        _items.addToHeader(_searchSortPanel);

        setWidget(row, 0, _tagCloudContainer = new SimplePanel());
        getFlexCellFormatter().setColSpan(row++, 0, 2);

        setWidget(row, 0, _status = new Label(""));
        getFlexCellFormatter().setColSpan(row++, 0, 2);
    }

    public void selectType (byte itemType)
    {
        _typeTabs.selectTab(itemType);
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
     * Called by the {@link ListingDetailPopup} if the owner requests to delist an item.
     */
    public void itemDelisted (CatalogListing listing)
    {
        _items.removeItem(listing);
    }

    protected void setStatus (String status)
    {
        _status.setText(status);
    }

    protected void refreshItems (boolean ignoreCache)
    {
        Byte tabKey = new Byte(_type);
        DataModel model = ignoreCache ? null : (DataModel) _models.get(tabKey);
        if (model == null) {
            model = new DataModel() {
                public void doFetchRows (int start, int count, final AsyncCallback callback) {
                    setStatus("Loading...");
                    CCatalog.catalogsvc.loadCatalog(CCatalog.getMemberId(), _type, _sortBy, _search, _tag, start, count, new AsyncCallback() {
                        public void onSuccess (Object result) {
                            setStatus("");
                            callback.onSuccess(result);
                        }
                        public void onFailure (Throwable caught) {
                            CCatalog.log("loadCatalog failed", caught);
                            setStatus(CCatalog.serverError(caught));
                        }
                    });
                }
                public void removeItem (Object item) {
                    // currently we do no internal caching, no problem!
                }
            };
        }
        _items.setModel(model);
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
                    _tag = tag;
                    newCloud.setCurrentTag(tag);
                    refreshItems(true);
                }
            };
            newCloud.setListener(tagListener);
            _clouds.put(tabKey, newCloud);
            cloud = newCloud;
        }
        _tagCloudContainer.setWidget(cloud);
        return cloud;
    }

    protected byte _sortBy,  _type;
    protected String _search, _tag;
    protected Map _models = new HashMap();
    protected Map _clouds = new HashMap();

    protected ItemTypePanel _typeTabs;
    protected ItemSearchSortPanel _searchSortPanel;
    protected SimplePanel _tagCloudContainer;
    protected PagedGrid _items;
    protected Label _status;
}
