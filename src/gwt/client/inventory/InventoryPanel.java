//
// $Id$

package client.inventory;

import java.util.HashMap;

import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.SourcesTabEvents;
import com.google.gwt.user.client.ui.TabListener;

import client.item.ItemTypePanel;

/**
 * Displays a tabbed panel containing a player's inventory.
 */
public class InventoryPanel extends FlexTable
    implements TabListener
{
    public InventoryPanel ()
    {
        setStyleName("inventoryPanel");
        setCellPadding(0);
        setCellSpacing(0);
        setWidth("100%");

        setWidget(0, 0, new Label(CInventory.msgs.inventoryTitle()));
        getFlexCellFormatter().setStyleName(0, 0, "Title");
        setWidget(0, 1, _itemTabs = new ItemTypePanel(this));
        getFlexCellFormatter().setStyleName(0, 1, "Tabs");
        setWidget(1, 0, _itemPaneContainer = new SimplePanel());
        getFlexCellFormatter().setColSpan(1, 0, 2);
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
        _itemPaneContainer.setWidget(panel);
    }

    // from TabListener
    public boolean onBeforeTabSelected (SourcesTabEvents sender, int tabIndex)
    {
        // always allow any item type selection 
        return true;
    }

    protected ItemTypePanel _itemTabs;
    protected HashMap _itemPanes = new HashMap();
    protected SimplePanel _itemPaneContainer;
}
