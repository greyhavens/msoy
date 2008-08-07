//
// $Id$

package com.threerings.msoy.stuff.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.ItemListInfo;
import com.threerings.msoy.item.data.all.ItemListQuery;

import com.threerings.msoy.stuff.gwt.StuffService.ItemListResult;

/**
 * The asynchronous (client-side) version of {@link StuffService}.
 */
public interface StuffServiceAsync
{
    /**
     * The asynchronous version of {@link StuffService#createItem}.
     */
    void createItem (Item item, ItemIdent parent, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#updateItem}.
     */
    void updateItem (Item item, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link StuffService#remixItem}.
     */
    void remixItem (Item item, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#revertRemixClone}.
     */
    void revertRemixedClone (ItemIdent itemIdent, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#renameClone}.
     */
    void renameClone (ItemIdent itemIdent, String name, AsyncCallback<String> callback);

    /**
     * The asynchronous version of {@link StuffService#loadInventory}.
     */
    void loadInventory (byte type, int suiteId, AsyncCallback<List<Item>> callback);

    /**
     * Loads the details of a particular item.
     */
    void loadItem (ItemIdent item, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link StuffService#loadItemDetail}.
     */
    void loadItemDetail (ItemIdent item, AsyncCallback<StuffService.DetailOrIdent> callback);

    /**
     * The asynchronous version of {@link ItemService#deleteItem}.
     */
    void deleteItem (ItemIdent item, AsyncCallback<Void> callback);

    /**
     * Loads items from a list that match the given criteria.
     */
    void loadItemList (ItemListQuery query, AsyncCallback<ItemListResult> callback);

    /**
     * Gets the number of list items that match the given query.
     */
    void getSize (ItemListQuery query, AsyncCallback<Integer> callback);

    /**
     * The asynchronous version of {@link ItemService#getFavoriteListInfo}.
     */
    void getFavoriteListInfo (AsyncCallback<ItemListInfo> callback);
}
