//
// $Id$

package com.threerings.msoy.item.server;

import com.threerings.msoy.item.data.all.ItemIdent;
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
     * Handles a {@link ItemService#deleteItem} request.
     */
    public void deleteItem (ClientObject caller, ItemIdent arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ItemService#getCatalogId} request.
     */
    public void getCatalogId (ClientObject caller, ItemIdent arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ItemService#getItemNames} request.
     */
    public void getItemNames (ClientObject caller, ItemIdent[] arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ItemService#peepItem} request.
     */
    public void peepItem (ClientObject caller, ItemIdent arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ItemService#reclaimItem} request.
     */
    public void reclaimItem (ClientObject caller, ItemIdent arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;
}
