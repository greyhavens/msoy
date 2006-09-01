//
// $Id$

package com.threerings.msoy.item.data {

import com.threerings.msoy.item.client.ItemService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.presents.dobj.InvocationResponseEvent;

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
    public static const GET_INVENTORY :int = 1;

    // documentation inherited from interface
    public function getInventory (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, GET_INVENTORY, [
            listener2
        ]);
    }

}
}
