//
// $Id$

package com.threerings.msoy.item.data {

import com.threerings.io.TypedArray;
import com.threerings.msoy.item.client.ItemService;
import com.threerings.msoy.item.data.all.ItemFlag_Kind;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

/**
 * Provides the implementation of the <code>ItemService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class ItemMarshaller extends InvocationMarshaller
    implements ItemService
{
    /** The method id used to dispatch <code>addFlag</code> requests. */
    public static const ADD_FLAG :int = 1;

    // from interface ItemService
    public function addFlag (arg1 :ItemIdent, arg2 :ItemFlag_Kind, arg3 :String, arg4 :InvocationService_ConfirmListener) :void
    {
        var listener4 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener4.listener = arg4;
        sendRequest(ADD_FLAG, [
            arg1, arg2, arg3, listener4
        ]);
    }

    /** The method id used to dispatch <code>deleteItem</code> requests. */
    public static const DELETE_ITEM :int = 2;

    // from interface ItemService
    public function deleteItem (arg1 :ItemIdent, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(DELETE_ITEM, [
            arg1, listener2
        ]);
    }

    /** The method id used to dispatch <code>getCatalogId</code> requests. */
    public static const GET_CATALOG_ID :int = 3;

    // from interface ItemService
    public function getCatalogId (arg1 :ItemIdent, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_CATALOG_ID, [
            arg1, listener2
        ]);
    }

    /** The method id used to dispatch <code>getItemNames</code> requests. */
    public static const GET_ITEM_NAMES :int = 4;

    // from interface ItemService
    public function getItemNames (arg1 :TypedArray /* of class com.threerings.msoy.item.data.all.ItemIdent */, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(GET_ITEM_NAMES, [
            arg1, listener2
        ]);
    }

    /** The method id used to dispatch <code>peepItem</code> requests. */
    public static const PEEP_ITEM :int = 5;

    // from interface ItemService
    public function peepItem (arg1 :ItemIdent, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(PEEP_ITEM, [
            arg1, listener2
        ]);
    }

    /** The method id used to dispatch <code>reclaimItem</code> requests. */
    public static const RECLAIM_ITEM :int = 6;

    // from interface ItemService
    public function reclaimItem (arg1 :ItemIdent, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(RECLAIM_ITEM, [
            arg1, listener2
        ]);
    }
}
}
