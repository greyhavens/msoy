//
// $Id$

package client.catalog;

import java.util.HashMap;
import java.util.Map;

import client.item.ItemTypePanel;
import client.item.TagCloud;

import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;

import com.threerings.msoy.item.web.Item;

import client.util.WebContext;

/**
 * Displays a tabbed panel containing the catalog.
 */
public class CatalogPanel extends VerticalPanel
    implements TabListener
{
    public CatalogPanel (WebContext ctx)
    {
        _ctx = ctx;
        setStyleName("catalog");

        HorizontalPanel topRow = new HorizontalPanel();
        _tagCloudContainer = new SimplePanel();
        topRow.add(_tagCloudContainer);

        ItemTypePanel itemTabs = new ItemTypePanel(this);
        topRow.add(itemTabs);
        add(topRow);

        add(_itemPaneContainer = new SimplePanel());

        itemTabs.selectTab(Item.AVATAR);
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
    
    protected WebContext _ctx;
    protected Map _itemPanes = new HashMap();
    protected Map _tagClouds = new HashMap();
    protected SimplePanel _itemPaneContainer;
    protected SimplePanel _tagCloudContainer;
}
