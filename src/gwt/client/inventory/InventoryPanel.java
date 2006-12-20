//
// $Id$

package client.inventory;

import java.util.HashMap;

import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.google.gwt.user.client.ui.VerticalPanel;

import client.item.ItemTypePanel;
import client.util.WebContext;

/**
 * Displays a tabbed panel containing a player's inventory.
 */
public class InventoryPanel extends VerticalPanel
    implements TabListener
{
    public InventoryPanel (WebContext ctx)
    {
        setStyleName("inventory");
        _ctx = ctx;

        add(_itemTabs = new ItemTypePanel(this));
        add(_itemPaneContainer = new SimplePanel());
    }

    public void selectType (byte itemType)
    {
        _itemTabs.selectTab(itemType);
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
    }

    // from TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // always allow any item type selection 
        return true;
    }

    protected WebContext _ctx;
    protected ItemTypePanel _itemTabs;
    protected HashMap _itemPanes = new HashMap();
    protected SimplePanel _itemPaneContainer;
}
