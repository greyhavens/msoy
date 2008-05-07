//
// $Id$

package com.threerings.msoy.item.client {

import flash.utils.ByteArray;
import com.threerings.io.TypedArray;
import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * An ActionScript version of the Java ItemService interface.
 */
public interface ItemService extends InvocationService
{
    // from Java interface ItemService
    function getCatalogId (arg1 :Client, arg2 :ItemIdent, arg3 :InvocationService_ResultListener) :void;

    // from Java interface ItemService
    function getItemNames (arg1 :Client, arg2 :TypedArray /* of class com.threerings.msoy.item.data.all.ItemIdent */, arg3 :InvocationService_ResultListener) :void;

    // from Java interface ItemService
    function peepItem (arg1 :Client, arg2 :ItemIdent, arg3 :InvocationService_ResultListener) :void;

    // from Java interface ItemService
    function reclaimItem (arg1 :Client, arg2 :ItemIdent, arg3 :InvocationService_ConfirmListener) :void;
}
}
