//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.web.data.ItemGIdent;
import com.threerings.msoy.web.data.WebCreds;

/**
 * The asynchronous (client-side) version of {@link ItemService}.
 */
public interface ItemServiceAsync
{
    /**
     * The asynchronous version of {@link ItemService#createItem}.
     */
    public void createItem (
        WebCreds creds, Item item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#loadInventory}.
     */
    public void loadInventory (
        WebCreds creds, String type, AsyncCallback callback);

    /**
     * Loads the details of a particular item.
     */
    public Item loadItem (
        WebCreds creds, ItemGIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#remixItem}.
     */
    public void remixItem (
        WebCreds creds, ItemGIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#getRating}.
     */
    public void getRating (
        WebCreds creds, ItemGIdent item, int memberId,
        AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#rateItem}.
     */
    public void rateItem (
        WebCreds creds, ItemGIdent item, byte rating, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#getTagHistory}.
     */
    public void getTagHistory (
        WebCreds creds, ItemGIdent item, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#tagItem}.
     */
    public void tagItem (
        WebCreds creds, ItemGIdent item, String tag, AsyncCallback callback);

    /**
     * The asynchronous version of {@link ItemService#untagItem}.
     */
    public void untagItem (
        WebCreds creds, ItemGIdent item, String tag, AsyncCallback callback);
}
