//
// $Id$

package com.threerings.msoy.stuff.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.orth.data.MediaDesc;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.data.all.MsoyItemType;

/**
 * Provides the asynchronous version of {@link StuffService}.
 */
public interface StuffServiceAsync
{
    /**
     * The async version of {@link StuffService#createItem}.
     */
    void createItem (Item item, AsyncCallback<Item> callback);

    /**
     * The async version of {@link StuffService#publishExternalMedia}.
     */
    void publishExternalMedia (String data, byte mimeType, AsyncCallback<MediaDesc> callback);

    /**
     * The async version of {@link StuffService#updateItem}.
     */
    void updateItem (Item item, AsyncCallback<Void> callback);

    /**
     * The async version of {@link StuffService#remixItem}.
     */
    void remixItem (Item item, AsyncCallback<Item> callback);

    /**
     * The async version of {@link StuffService#revertRemixedClone}.
     */
    void revertRemixedClone (ItemIdent itemIdent, AsyncCallback<Item> callback);

    /**
     * The async version of {@link StuffService#renameClone}.
     */
    void renameClone (ItemIdent itemIdent, String name, AsyncCallback<String> callback);

    /**
     * The async version of {@link StuffService#loadInventory}.
     */
    void loadInventory (int memberId, MsoyItemType type, String query, AsyncCallback<StuffService.InventoryResult<Item>> callback);

    /**
     * The async version of {@link StuffService#loadItem}.
     */
    void loadItem (ItemIdent item, AsyncCallback<Item> callback);

    /**
     * The async version of {@link StuffService#loadItemDetail}.
     */
    void loadItemDetail (ItemIdent item, AsyncCallback<StuffService.DetailOrIdent> callback);

    /**
     * The async version of {@link StuffService#deleteItem}.
     */
    void deleteItem (ItemIdent item, AsyncCallback<Void> callback);

    /**
     * The async version of {@link StuffService#loadThemeLineup}.
     */
    void loadThemeLineup (int groupId, AsyncCallback<StuffService.InventoryResult<Avatar>> callback);
}
