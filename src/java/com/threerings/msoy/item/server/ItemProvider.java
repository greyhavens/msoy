//
// $Id$

package com.threerings.msoy.item.server;

import com.threerings.msoy.item.client.ItemService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link ItemService}.
 */
public interface ItemProvider extends InvocationProvider
{
    /**
     * Handles a {@link ItemService#getInventory} request.
     */
    public void getInventory (ClientObject caller, String arg1, InvocationService.ResultListener arg2)
        throws InvocationException;
}
