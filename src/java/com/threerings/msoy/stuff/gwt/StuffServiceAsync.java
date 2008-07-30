//
// $Id$

package com.threerings.msoy.stuff.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.web.data.WebIdent;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * The asynchronous (client-side) version of {@link StuffService}.
 */
public interface StuffServiceAsync
{
    /**
     * The asynchronous version of {@link StuffService#createItem}.
     */
    void createItem (WebIdent ident, Item item, ItemIdent parent, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#updateItem}.
     */
    void updateItem (WebIdent ident, Item item, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link StuffService#remixItem}.
     */
    void remixItem (WebIdent ident, Item item, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#revertRemixClone}.
     */
    void revertRemixedClone (WebIdent ident, ItemIdent itemIdent, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#renameClone}.
     */
    void renameClone (WebIdent ident, ItemIdent itemIdent, String name,
                      AsyncCallback<String> callback);

    /**
     * The asynchronous version of {@link StuffService#loadInventory}.
     */
    void loadInventory (WebIdent ident, byte type, int suiteId,
                        AsyncCallback<List<Item>> callback);

    /**
     * Loads the details of a particular item.
     */
    void loadItem (WebIdent ident, ItemIdent item, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#loadItemDetail}.
     */
    void loadItemDetail (WebIdent ident, ItemIdent item,
                         AsyncCallback<StuffService.DetailOrIdent> callback);

    /**
     * The asynchronous version of {@link ItemService#deleteItem}.
     */
    void deleteItem (WebIdent ident, ItemIdent item, AsyncCallback<Void> callback);
}
