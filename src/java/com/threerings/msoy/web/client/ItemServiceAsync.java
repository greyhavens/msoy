//
// $Id$

package com.threerings.msoy.web.client;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.item.data.Item;
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
     * The asynchronous version of {@link ItemService#loadInventory}.
     */
    public void loadInventory (WebCreds creds, String type,
                               AsyncCallback callback);
}
