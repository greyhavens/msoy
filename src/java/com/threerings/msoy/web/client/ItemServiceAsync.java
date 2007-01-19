//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link ItemService}.
 */
public interface ItemServiceAsync
{
    /**
     * The asynchronous version of {@link ItemService#createItem}.
     */
    public void createItem (WebCreds creds, Item item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#updateItem}.
     */
    public void updateItem (WebCreds creds, Item item, AsyncCallback callback);

    /**
     * Loads the details of a particular item.
     */
    public void loadItem (WebCreds creds, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#loadItemDetail}.
     */
    public void loadItemDetail (WebCreds creds, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#remixItem}.
     */
    public void remixItem (WebCreds creds, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#deleteItem}.
     */
    public void deleteItem (WebCreds creds, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#getRating}.
     */
    public void getRating (WebCreds creds, ItemIdent item, int memberId, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#rateItem}.
     */
    public void rateItem (WebCreds creds, ItemIdent item, byte rating, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#getTags}.
     */
    public void getTags (WebCreds creds, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous versions of {@link ItemService#getTagHistory}.
     */
    public void getTagHistory (WebCreds creds, ItemIdent item, AsyncCallback callback);

    /**
     * The asynchronous versions of {@link ItemService#getRecentTags}.
     */
    public void getRecentTags (WebCreds creds, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#tagItem}.
     */
    public void tagItem (
        WebCreds creds, ItemIdent item, String tag, boolean set, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService.setFlags}.
     */
    public void setFlags (WebCreds creds, ItemIdent ident, byte mask, byte values,
                          AsyncCallback callback);
}
