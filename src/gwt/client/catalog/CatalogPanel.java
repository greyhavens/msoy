//
// $Id$

package client.catalog;

import java.util.HashMap;
import java.util.Map;

import client.item.ItemSearchSortPanel;
import client.item.ItemTypePanel;
import client.item.TagCloud;
import client.util.WebContext;

import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.threerings.msoy.item.web.CatalogListing;
import com.threerings.msoy.item.web.Item;

/**
 * Displays a tabbed panel containing the catalog.
 */
public class CatalogPanel extends VerticalPanel
    implements TabListener, ItemSearchSortPanel.Listener
{
    public CatalogPanel (WebContext ctx)
    {
        _ctx = ctx;
        setStyleName("catalog");

        HorizontalPanel topRow = new HorizontalPanel();
        _tagCloudContainer = new SimplePanel();
        topRow.add(_tagCloudContainer);
        
        VerticalPanel uiBits = new VerticalPanel();
        ItemTypePanel itemTypePanel = new ItemTypePanel(this);
        uiBits.add(itemTypePanel);
        uiBits.add(new ItemSearchSortPanel( this,
            new String[] { "Rating", "List Date" },
            new byte[] { CatalogListing.SORT_BY_RATING, CatalogListing.SORT_BY_LIST_DATE },
            0));
        _sortBy = CatalogListing.SORT_BY_RATING;

        topRow.add(uiBits);
        add(topRow);

        add(_itemPaneContainer = new SimplePanel());

        // when everything is nicely set up, select a tab
        itemTypePanel.selectTab(Item.AVATAR);
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
            panel = new ItemPanel(_ctx, _tabIndex, _sortBy, _search);
            _itemPanes.put(tabKey, panel);
        }
        _itemPaneContainer.setWidget(panel);
    }

    protected void getTagCloud (boolean ignoreCache)
    {
        Byte tabKey = new Byte(_tabIndex);
        TagCloud cloud = ignoreCache ? null : (TagCloud) _tagClouds.get(tabKey);
        if (cloud == null) {
            cloud = new TagCloud(_ctx, _tabIndex);
            _tagClouds.put(tabKey, cloud);
        }
        _tagCloudContainer.setWidget(cloud);
    }

    protected WebContext _ctx;
    protected byte _sortBy;
    protected String _search;
    protected byte _tabIndex;
    protected Map _itemPanes = new HashMap();
    protected Map _tagClouds = new HashMap();
    protected SimplePanel _itemPaneContainer;
    protected SimplePanel _tagCloudContainer;
}
