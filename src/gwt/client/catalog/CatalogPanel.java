//
// $Id$

package client.catalog;

import java.util.HashMap;
import java.util.Map;

import client.item.ItemSearchSortPanel;
import client.item.ItemTypePanel;
import client.item.TagCloud;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.threerings.msoy.item.web.CatalogListing;

/**
 * Displays a tabbed panel containing the catalog.
 */
public class CatalogPanel extends VerticalPanel
    implements TabListener, ItemSearchSortPanel.Listener
{
    public CatalogPanel ()
    {
        setStyleName("catalog");

        HorizontalPanel topRow = new HorizontalPanel();
        _tagCloudContainer = new SimplePanel();
        topRow.add(_tagCloudContainer);
        
        VerticalPanel uiBits = new VerticalPanel();
        _typeTabs = new ItemTypePanel(this);
        uiBits.add(_typeTabs);
        uiBits.add(new ItemSearchSortPanel(this,
            new String[] { CCatalog.msgs.sortByRating(), CCatalog.msgs.sortByListDate() },
            new byte[] { CatalogListing.SORT_BY_RATING, CatalogListing.SORT_BY_LIST_DATE },
            0));
        _sortBy = CatalogListing.SORT_BY_RATING;

        topRow.add(uiBits);
        add(topRow);

        add(_itemPaneContainer = new SimplePanel());
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
            _itemPanes.put(tabKey, panel = new ItemPanel(_tabIndex, _sortBy, _search));
        }
        _itemPaneContainer.setWidget(panel);
    }

    protected void getTagCloud (boolean ignoreCache)
    {
        Byte tabKey = new Byte(_tabIndex);
        TagCloud cloud = ignoreCache ? null : (TagCloud) _tagClouds.get(tabKey);
        if (cloud == null) {
            _tagClouds.put(tabKey, cloud = new TagCloud(_tabIndex));
        }
        _tagCloudContainer.setWidget(cloud);
    }

    protected byte _sortBy;
    protected String _search;
    protected byte _tabIndex;
    protected ItemTypePanel _typeTabs;
    protected Map _itemPanes = new HashMap();
    protected Map _tagClouds = new HashMap();
    protected SimplePanel _itemPaneContainer;
    protected SimplePanel _tagCloudContainer;
}
