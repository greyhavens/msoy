//
// $Id$

package com.threerings.msoy.item.gwt;

import java.util.Collection;
import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;

import com.threerings.msoy.web.data.TagHistory;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link ItemService}.
 */
public interface ItemServiceAsync
{
    /**
     * The asynchronous version of {@link ItemService#createItem}.
     */
    void createItem (WebIdent ident, Item item, ItemIdent parent, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link ItemService#updateItem}.
     */
    void updateItem (WebIdent ident, Item item, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService#remixItem}.
     */
    void remixItem (WebIdent ident, Item item, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link ItemService#revertRemixClone}.
     */
    void revertRemixedClone (WebIdent ident, ItemIdent itemIdent, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link ItemService#renameClone}.
     */
    void renameClone (WebIdent ident, ItemIdent itemIdent, String name,
                      AsyncCallback<String> callback);

    /**
     * The asynchronous version of {@link ItemService#loadInventory}.
     */
    void loadInventory (WebIdent ident, byte type, int suiteId,
                        AsyncCallback<List<Item>> callback);

    /**
     * Loads the details of a particular item.
     */
    void loadItem (WebIdent ident, ItemIdent item, AsyncCallback<Item> callback);

    /**
     * The asynchronous version of {@link ItemService#loadItemDetail}.
     */
    void loadItemDetail (WebIdent ident, ItemIdent item,
                         AsyncCallback<ItemService.DetailOrIdent> callback);

    /**
     * The asynchronous version of {@link ItemService#scaleAvatar}.
     */
    void scaleAvatar (WebIdent ident, int avatarId, float newScale,
                      AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService#deleteItem}.
     */
    void deleteItem (WebIdent ident, ItemIdent item, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService#rateItem}.
     */
    void rateItem (WebIdent ident, ItemIdent item, byte rating, AsyncCallback<Float> callback);

    /**
     * The asynchronous version of {@link ItemService#getTags}.
     */
    void getTags (WebIdent ident, ItemIdent item, AsyncCallback<Collection<String>> callback);

    /**
     * The asynchronous versions of {@link ItemService#getTagHistory}.
     */
    void getTagHistory (WebIdent ident, ItemIdent item,
                        AsyncCallback<Collection<TagHistory>> callback);

    /**
     * The asynchronous versions of {@link ItemService#getRecentTags}.
     */
    void getRecentTags (WebIdent ident, AsyncCallback<Collection<TagHistory>> callback);

    /**
     * The asynchronous version of {@link ItemService#tagItem}.
     */
    void tagItem (WebIdent ident, ItemIdent item, String tag, boolean set,
                  AsyncCallback<TagHistory> callback);

    /**
     * The asynchronous version of {@link ItemService.wrapItem}.
     */
    void wrapItem (WebIdent ident, ItemIdent item, boolean wrap, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService.setFlags}.
     */
    void setFlags (WebIdent ident, ItemIdent item, byte mask, byte values,
                   AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService.setMature}.
     */
    void setMature (WebIdent ident, ItemIdent item, boolean value, AsyncCallback<Void> callback);

    /**
     * The asynchronous version of {@link ItemService.getFlaggedItems}.
     */
    void getFlaggedItems (WebIdent ident, int count, AsyncCallback<List<ItemDetail>> callback);

    /**
     * The asynchronous version of {@link ItemService.deleteItemAdmin}.
     */
    void deleteItemAdmin (WebIdent ident, ItemIdent item, String subject, String body,
                          AsyncCallback<Integer> callback);
    
    /**
     * The asynchronous version of {@link ItemService.setFavorite}.
     */
    void setFavorite (WebIdent ident, ItemIdent item, boolean favorite, 
                      AsyncCallback<Void> callback);
}
