//
// $Id$

package com.threerings.msoy.item.client {

import com.threerings.io.TypedArray;
import com.threerings.msoy.item.data.all.ItemFlag_Kind;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java ItemService interface.
 */
public interface ItemService extends InvocationService
{
    // from Java interface ItemService
    function addFlag (arg1 :ItemIdent, arg2 :ItemFlag_Kind, arg3 :String, arg4 :InvocationService_ConfirmListener) :void;

    // from Java interface ItemService
    function deleteItem (arg1 :ItemIdent, arg2 :InvocationService_ConfirmListener) :void;

    // from Java interface ItemService
    function getCatalogId (arg1 :ItemIdent, arg2 :InvocationService_ResultListener) :void;

    // from Java interface ItemService
    function getItemNames (arg1 :TypedArray /* of class com.threerings.msoy.item.data.all.ItemIdent */, arg2 :InvocationService_ResultListener) :void;

    // from Java interface ItemService
    function peepItem (arg1 :ItemIdent, arg2 :InvocationService_ResultListener) :void;

    // from Java interface ItemService
    function reclaimItem (arg1 :ItemIdent, arg2 :InvocationService_ConfirmListener) :void;
}
}
