//
// $Id$

package com.threerings.msoy.item.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.ItemFlag;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Provides the implementation of the {@link ItemService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from ItemService.java.")
public class ItemMarshaller extends InvocationMarshaller<ClientObject>
    implements ItemService
{
    /** The method id used to dispatch {@link #addFlag} requests. */
    public static final int ADD_FLAG = 1;

    // from interface ItemService
    public void addFlag (ItemIdent arg1, ItemFlag.Kind arg2, String arg3, InvocationService.ConfirmListener arg4)
    {
        InvocationMarshaller.ConfirmMarshaller listener4 = new InvocationMarshaller.ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(ADD_FLAG, new Object[] {
            arg1, arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #deleteItem} requests. */
    public static final int DELETE_ITEM = 2;

    // from interface ItemService
    public void deleteItem (ItemIdent arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(DELETE_ITEM, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #getCatalogId} requests. */
    public static final int GET_CATALOG_ID = 3;

    // from interface ItemService
    public void getCatalogId (ItemIdent arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_CATALOG_ID, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #getItemNames} requests. */
    public static final int GET_ITEM_NAMES = 4;

    // from interface ItemService
    public void getItemNames (ItemIdent[] arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_ITEM_NAMES, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #peepItem} requests. */
    public static final int PEEP_ITEM = 5;

    // from interface ItemService
    public void peepItem (ItemIdent arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(PEEP_ITEM, new Object[] {
            arg1, listener2
        });
    }

    /** The method id used to dispatch {@link #reclaimItem} requests. */
    public static final int RECLAIM_ITEM = 6;

    // from interface ItemService
    public void reclaimItem (ItemIdent arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(RECLAIM_ITEM, new Object[] {
            arg1, listener2
        });
    }
}
