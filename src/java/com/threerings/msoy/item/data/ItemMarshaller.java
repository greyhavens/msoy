//
// $Id$

package com.threerings.msoy.item.data;

import com.threerings.msoy.item.client.ItemService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link ItemService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class ItemMarshaller extends InvocationMarshaller
    implements ItemService
{
    /** The method id used to dispatch {@link #getInventory} requests. */
    public static final int GET_INVENTORY = 1;

    // from interface ItemService
    public void getInventory (Client arg1, byte arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, GET_INVENTORY, new Object[] {
            Byte.valueOf(arg2), listener3
        });
    }
}
