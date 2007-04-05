//
// $Id$

package client.inventory;

import java.util.HashMap;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;

import client.item.ItemTypePanel;

/**
 * Displays a tabbed panel containing a player's inventory.
 */
public class InventoryPanel extends SimplePanel
    implements TabListener
{
    public InventoryPanel ()
    {
        setStyleName("inventoryPanel");

        _itemTabs = new ItemTypePanel("inventory", this);
    }

    public Widget getTabs ()
    {
        return _itemTabs;
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
            panel = new ItemPanel((byte) tabIndex);
            _itemPanes.put(new Integer(tabIndex), panel);
        }
        setWidget(panel);
    }

    // from TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // always allow any item type selection 
        return true;
    }

    protected ItemTypePanel _itemTabs;
    protected HashMap _itemPanes = new HashMap();
}
