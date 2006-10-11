//
// $Id$

package com.threerings.msoy.item.client {

import com.threerings.msoy.item.client.ItemService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * An ActionScript version of the Java ItemService interface.
 */
public interface ItemService extends InvocationService
{
    // from Java interface ItemService
    function getInventory (arg1 :Client, arg2 :int, arg3 :InvocationService_ResultListener) :void;
}
}
