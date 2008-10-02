//
// $Id$

package client.remix;

import com.threerings.msoy.item.data.all.Item;

/**
 * An interface for having a page communicate with the remixer.
 * It's a bit of a twisty maze of passages.
 */
public interface RemixerHost
{
    /**
     * Called by the remixer when the user needs to purchase the item.
     * The page should handle that, and then call back into the remixer iwth
     * itemPurchased(), passing null if the user declined to purchase.
     */
    void buyItem ();

    /**
     * Called by the remixer when it is all done remixing. If the item
     * is non-null, that indicates that the remix was saved.
     */
    void remixComplete (Item item);
}
