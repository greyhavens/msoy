//
// $Id$

package com.threerings.msoy.item.client {

import flash.utils.ByteArray;
import com.threerings.msoy.item.client.ItemService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;

/**
 * An ActionScript version of the Java ItemService interface.
 */
public interface ItemService extends InvocationService
{
    // from Java interface ItemService
    function getInventory (arg1 :Client, arg2 :int, arg3 :InvocationService_InvocationListener) :void;
}
}
