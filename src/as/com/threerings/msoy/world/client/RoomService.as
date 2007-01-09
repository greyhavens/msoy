//
// $Id$

package com.threerings.msoy.world.client {

import flash.utils.ByteArray;
import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.world.client.RoomService;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.whirled.data.SceneUpdate;

/**
 * An ActionScript version of the Java RoomService interface.
 */
public interface RoomService extends InvocationService
{
    // from Java interface RoomService
    function editRoom (arg1 :Client, arg2 :InvocationService_ResultListener) :void;

    // from Java interface RoomService
    function triggerEvent (arg1 :Client, arg2 :ItemIdent, arg3 :String) :void;

    // from Java interface RoomService
    function updateMemory (arg1 :Client, arg2 :MemoryEntry) :void;

    // from Java interface RoomService
    function updateRoom (arg1 :Client, arg2 :Array, arg3 :InvocationService_InvocationListener) :void;
}
}
