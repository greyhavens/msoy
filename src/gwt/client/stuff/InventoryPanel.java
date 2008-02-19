//
// $Id$

package client.stuff;

import java.util.HashMap;

import com.google.gwt.user.client.ui.Widget;
import com.google.gwt.user.client.ui.SimplePanel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Displays a tabbed panel containing a player's inventory.
 */
public class InventoryPanel extends SimplePanel
{
    public InventoryPanel (InventoryModels models)
    {
        setStyleName("inventoryPanel");
        _models = models;
    }

    public void display (byte itemType, int pageNo, int itemId)
    {
        if (itemId == 0) {
            getItemPanel(itemType).setPage(pageNo);
        } else {
            Item item = _models.findItem(itemType, itemId);
            if (item == null) {
                getItemPanel(itemType).showDetail(new ItemIdent(itemType, itemId));
            } else {
                getItemPanel(itemType).showDetail(item);
            }
        }
        setWidget(getItemPanel(itemType));
    }

    protected ItemPanel getItemPanel (byte itemType)
    {
        Byte key = new Byte(itemType);
        ItemPanel panel = (ItemPanel) _itemPanes.get(key);
        if (panel == null) {
            panel = new ItemPanel(_models, itemType);
            _itemPanes.put(key, panel);
        }
        return panel;
    }

    protected InventoryModels _models;
    protected HashMap _itemPanes = new HashMap();
}
