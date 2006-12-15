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
        uiBits.add(new ItemTypePanel(this));
        uiBits.add(new ItemSearchSortPanel( this,
            new String[] { "Rating", "List Date" },
            new byte[] { CatalogListing.SORT_BY_RATING, CatalogListing.SORT_BY_LIST_DATE }));

        topRow.add(uiBits);
        add(topRow);

        add(_itemPaneContainer = new SimplePanel());

        new ItemTypePanel(this).selectTab(Item.AVATAR);
    }

    // from TabListener
    public void onTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        ItemPanel panel = (ItemPanel) _itemPanes.get(new Integer(tabIndex));
        if (panel == null) {
            panel = new ItemPanel(_ctx, (byte) tabIndex);
            _itemPanes.put(new Integer(tabIndex), panel);
        }
        _itemPaneContainer.setWidget(panel);

        TagCloud cloud = (TagCloud) _tagClouds.get(new Integer(tabIndex));
        if (cloud == null) {
            cloud = new TagCloud(_ctx, (byte) tabIndex);
            _tagClouds.put(new Integer(tabIndex), cloud);
        }
        _tagCloudContainer.setWidget(cloud);
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
        Window.alert("I would search for: " + panel.search);
    }

    // from ItemSearchSortPanel.Listener
    public void sort (ItemSearchSortPanel panel)
    {
        Window.alert("I would search by criterium number: " + panel.sortBy);
    }

    protected WebContext _ctx;
    protected Map _itemPanes = new HashMap();
    protected Map _tagClouds = new HashMap();
    protected SimplePanel _itemPaneContainer;
    protected SimplePanel _tagCloudContainer;
}
