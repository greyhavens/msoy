//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.web.data.WebIdent;

/**
 * The asynchronous (client-side) version of {@link ItemService}.
 */
public interface ItemServiceAsync
{
    /**
     * The asynchronous version of {@link ItemService#createItem}.
     */
    public void createItem (WebIdent ident, Item item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#updateItem}.
     */
    public void updateItem (WebIdent ident, Item item, AsyncCallback callback);

    /**
     * Loads the details of a particular item.
     */
    public void loadItem (WebIdent ident, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#loadItemDetail}.
     */
    public void loadItemDetail (WebIdent ident, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#scaleAvatar}.
     */
    public void scaleAvatar (WebIdent ident, int avatarId, float newScale, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#remixItem}.
     */
    public void remixItem (WebIdent ident, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#deleteItem}.
     */
    public void deleteItem (WebIdent ident, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#rateItem}.
     */
    public void rateItem (WebIdent ident, ItemIdent item, byte rating, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#getTags}.
     */
    public void getTags (WebIdent ident, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous versions of {@link ItemService#getTagHistory}.
     */
    public void getTagHistory (WebIdent ident, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous versions of {@link ItemService#getRecentTags}.
     */
    public void getRecentTags (WebIdent ident, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#tagItem}.
     */
    public void tagItem (WebIdent ident, ItemIdent item, String tag, boolean set,
                         AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService.wrapItem}.
     */
    public void wrapItem (WebIdent ident, ItemIdent item, boolean wrap, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService.setFlags}.
     */
    public void setFlags (WebIdent ident, ItemIdent item, byte mask, byte values,
                          AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService.setMature}.
     */
    public void setMature (WebIdent ident, ItemIdent item, boolean value, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService.getFlaggedItems}.
     */
    public void getFlaggedItems (WebIdent ident, int count, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService.deleteItemAdmin}.
     */
    public void deleteItemAdmin (WebIdent ident, ItemIdent item, String subject, String body,
                                 AsyncCallback callback);

}
