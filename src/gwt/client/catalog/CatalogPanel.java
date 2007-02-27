//
// $Id$

package client.catalog;

import java.util.HashMap;
import java.util.Map;

import client.item.ItemSearchSortPanel;
import client.item.ItemTypePanel;
import client.item.TagCloud;
import client.item.TagCloud.TagCloudListener;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.threerings.msoy.item.web.CatalogListing;

/**
 * Displays a tabbed panel containing the catalog.
 */
public class CatalogPanel extends FlexTable
    implements TabListener, ItemSearchSortPanel.Listener
{
    public CatalogPanel ()
    {
        setStyleName("catalogPanel");
        setCellPadding(0);
        setCellSpacing(0);
        setWidth("100%");

        setWidget(0, 0, new Label(CCatalog.msgs.catalogTitle()));
        getFlexCellFormatter().setStyleName(0, 0, "Title");

        setWidget(0, 1, _typeTabs = new ItemTypePanel(this));
        getFlexCellFormatter().setStyleName(0, 1, "Tabs");

        HorizontalPanel search = new HorizontalPanel();
        search.add(_tagCloudContainer = new SimplePanel());
        _searchSortPanel = new ItemSearchSortPanel(
            this, new String[] { CCatalog.msgs.sortByRating(), CCatalog.msgs.sortByListDate() },
            new byte[] { CatalogListing.SORT_BY_RATING, CatalogListing.SORT_BY_LIST_DATE }, 0);
        search.add(_searchSortPanel);
        _sortBy = CatalogListing.SORT_BY_RATING;

        setWidget(1, 0, search);
        getFlexCellFormatter().setColSpan(1, 0, 2);

        setWidget(2, 0, _itemPaneContainer = new SimplePanel());
        getFlexCellFormatter().setColSpan(2, 0, 2);
    }

    public void selectType (byte itemType)
    {
        _typeTabs.selectTab(itemType);
    }

    // from TabListener
    public void onTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        _tabIndex = (byte) tabIndex;
        getItemPanel(false);
        getTagCloud(false);
    }

    // from TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // always allow any item type selection
        return true;
    }

    //from ItemSearchSortPanel.Listener
    public void search (ItemSearchSortPanel panel)
    {
        _search = panel.search;
        _tag = null;
        getTagCloud(false).setCurrentTag(null);
        getItemPanel(true);
    }

    // from ItemSearchSortPanel.Listener
    public void sort (ItemSearchSortPanel panel)
    {
        _sortBy = panel.sortBy;
        getItemPanel(true);
    }

    protected void getItemPanel (boolean ignoreCache)
    {
        Byte tabKey = new Byte(_tabIndex);
        ItemPanel panel = ignoreCache ? null : (ItemPanel) _itemPanes.get(tabKey);
        if (panel == null) {
            _itemPanes.put(tabKey, panel = new ItemPanel(_tabIndex, _sortBy, _search, _tag));
        }
        _itemPaneContainer.setWidget(panel);
    }

    protected TagCloud getTagCloud (boolean ignoreCache)
    {
        Byte tabKey = new Byte(_tabIndex);
        TagCloud cloud = ignoreCache ? null : (TagCloud) _tagClouds.get(tabKey);
        if (cloud == null) {
            final TagCloud newCloud = new TagCloud(_tabIndex);
            TagCloudListener tagListener = new TagCloudListener() {
                public void tagClicked (String tag) {
                    _search = "";
                    _searchSortPanel.clearSearchBox();
                    _tag = tag;
                    newCloud.setCurrentTag(tag);
                    getItemPanel(true);
                }
            };
            newCloud.setListener(tagListener);
            _tagClouds.put(tabKey, newCloud);
            cloud = newCloud;
        }
        _tagCloudContainer.setWidget(cloud);
        return cloud;
    }

    protected byte _sortBy;
    protected String _search;
    protected String _tag;
    protected byte _tabIndex;
    protected ItemTypePanel _typeTabs;
    protected ItemSearchSortPanel _searchSortPanel;
    protected Map _itemPanes = new HashMap();
    protected Map _tagClouds = new HashMap();
    protected SimplePanel _itemPaneContainer;
    protected SimplePanel _tagCloudContainer;
}
