//
// $Id$

package com.threerings.msoy.item.server;

import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.ItemFlag;
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
     * Handles a {@link ItemService#addFlag} request.
     */
    void addFlag (ClientObject caller, ItemIdent arg1, ItemFlag.Kind arg2, String arg3, InvocationService.ConfirmListener arg4)
        throws InvocationException;

    /**
     * Handles a {@link ItemService#deleteItem} request.
     */
    void deleteItem (ClientObject caller, ItemIdent arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ItemService#getCatalogId} request.
     */
    void getCatalogId (ClientObject caller, ItemIdent arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ItemService#getItemNames} request.
     */
    void getItemNames (ClientObject caller, ItemIdent[] arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ItemService#peepItem} request.
     */
    void peepItem (ClientObject caller, ItemIdent arg1, InvocationService.ResultListener arg2)
        throws InvocationException;

    /**
     * Handles a {@link ItemService#reclaimItem} request.
     */
    void reclaimItem (ClientObject caller, ItemIdent arg1, InvocationService.ConfirmListener arg2)
        throws InvocationException;
}
