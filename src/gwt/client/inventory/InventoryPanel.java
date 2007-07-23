//
// $Id$

package client.inventory;

import java.util.HashMap;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;
import com.threerings.msoy.item.data.all.ItemIdent;

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

    public void display (byte itemType, int pageNo, int itemId)
    {
        if (itemId == 0) {
            getItemPanel(itemType).setPage(pageNo);
        } else {
            getItemPanel(itemType).showDetail(new ItemIdent(itemType, itemId));
        }
        _itemTabs.selectTab(itemType);
    }

    // from TabListener
    public void onTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        setWidget(getItemPanel((byte)tabIndex));
    }

    // from TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // always allow any item type selection 
        return true;
    }

    protected ItemPanel getItemPanel (byte itemType)
    {
        ItemPanel panel = (ItemPanel) _itemPanes.get(new Byte(itemType));
        if (panel == null) {
            panel = new ItemPanel(itemType);
            _itemPanes.put(new Byte(itemType), panel);
        }
        return panel;
    }

    protected ItemTypePanel _itemTabs;
    protected HashMap _itemPanes = new HashMap();
}
