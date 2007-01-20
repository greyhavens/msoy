package client.item;

import client.inventory.ItemDetailPopup;
import client.inventory.ItemEditor;

import com.threerings.msoy.item.web.Item;

public interface ItemDetailHost
{
    /**
     * Called by an active {@link ItemDetailPopup} to let us know that an item has been deleted
     * from our inventory.
     */
    void itemDeleted (Item _item);

    /**
     * Creates an item editor interface for items of the specified type.  Returns null if the type
     * is unknown.
     */
    ItemEditor createItemEditor (byte type);
}
