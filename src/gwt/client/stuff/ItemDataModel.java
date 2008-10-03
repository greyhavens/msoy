//
// $Id$

package client.stuff;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.threerings.gwt.util.DataModel;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.stuff.gwt.StuffService;

/**
 * This defines methods used by the GUI to get items from the model and to make callbacks to the
 * model when the user has changed or deleted an item.
 *
 * @author mjensen
 */
public interface ItemDataModel
{
    /**
     * Returns the Item type to use for this model.
     */
    byte getDefaultItemType ();

    /**
     * Used to try to find an item in the local data model/cache. If this returns null, then the
     * item will get loaded from the server.
     */
    Item findItem (byte itemType, int itemId);

    /**
     * A callback to let the model know that the item was renamed, etc.
     */
    void itemUpdated (Item item);

    /**
     * Let the model know that the item was deleted.
     */
    void itemDeleted (Item item);

    /**
     * Loads a data model for the specified item type and suite.
     */
    void loadModel (byte itemType, int suiteId, String query,
        AsyncCallback<DataModel<Item>> resultCallback);

    /**
     * Loads item details for a particular item.
     */
    void loadItemDetail(ItemIdent ident, AsyncCallback<StuffService.DetailOrIdent> resultCallback);
}
