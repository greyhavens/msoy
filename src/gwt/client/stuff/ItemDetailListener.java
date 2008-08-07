package client.stuff;

import com.threerings.msoy.item.data.all.Item;

/**
 * Listens for changes made to an item detail by the user.
 *
 * @author mjensen
 */
public interface ItemDetailListener
{
    /**
     * The item was renamed, etc.
     */
    public void itemUpdated (Item item);

    /**
     * The item was deleted.
     */
    public void itemDeleted (Item item);
}
