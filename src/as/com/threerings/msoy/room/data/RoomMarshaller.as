//
// $Id$

package com.threerings.msoy.room.data {

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.room.client.RoomService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;
import com.threerings.util.Integer;
import com.threerings.util.langBoolean;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;
import flash.utils.ByteArray;

/**
 * Provides the implementation of the <code>RoomService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class RoomMarshaller extends InvocationMarshaller
    implements RoomService
{
    /** The method id used to dispatch <code>changeLocation</code> requests. */
    public static const CHANGE_LOCATION :int = 1;

    // from interface RoomService
    public function changeLocation (arg1 :Client, arg2 :ItemIdent, arg3 :Location) :void
    {
        sendRequest(arg1, CHANGE_LOCATION, [
            arg2, arg3
        ]);
    }

    /** The method id used to dispatch <code>despawnMob</code> requests. */
    public static const DESPAWN_MOB :int = 2;

    // from interface RoomService
    public function despawnMob (arg1 :Client, arg2 :int, arg3 :String, arg4 :InvocationService_InvocationListener) :void
    {
        var listener4 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(arg1, DESPAWN_MOB, [
            Integer.valueOf(arg2), arg3, listener4
        ]);
    }

    /** The method id used to dispatch <code>editRoom</code> requests. */
    public static const EDIT_ROOM :int = 3;

    // from interface RoomService
    public function editRoom (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, EDIT_ROOM, [
            listener2
        ]);
    }

    /** The method id used to dispatch <code>moveMob</code> requests. */
    public static const MOVE_MOB :int = 4;

    // from interface RoomService
    public function moveMob (arg1 :Client, arg2 :int, arg3 :String, arg4 :Location, arg5 :InvocationService_InvocationListener) :void
    {
        var listener5 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(arg1, MOVE_MOB, [
            Integer.valueOf(arg2), arg3, arg4, listener5
        ]);
    }

    /** The method id used to dispatch <code>publishRoom</code> requests. */
    public static const PUBLISH_ROOM :int = 5;

    // from interface RoomService
    public function publishRoom (arg1 :Client, arg2 :InvocationService_InvocationListener) :void
    {
        var listener2 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, PUBLISH_ROOM, [
            listener2
        ]);
    }

    /** The method id used to dispatch <code>purchaseRoom</code> requests. */
    public static const PURCHASE_ROOM :int = 6;

    // from interface RoomService
    public function purchaseRoom (arg1 :Client, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(arg1, PURCHASE_ROOM, [
            listener2
        ]);
    }

    /** The method id used to dispatch <code>requestControl</code> requests. */
    public static const REQUEST_CONTROL :int = 7;

    // from interface RoomService
    public function requestControl (arg1 :Client, arg2 :ItemIdent) :void
    {
        sendRequest(arg1, REQUEST_CONTROL, [
            arg2
        ]);
    }

    /** The method id used to dispatch <code>sendSpriteMessage</code> requests. */
    public static const SEND_SPRITE_MESSAGE :int = 8;

    // from interface RoomService
    public function sendSpriteMessage (arg1 :Client, arg2 :ItemIdent, arg3 :String, arg4 :ByteArray, arg5 :Boolean) :void
    {
        sendRequest(arg1, SEND_SPRITE_MESSAGE, [
            arg2, arg3, arg4, langBoolean.valueOf(arg5)
        ]);
    }

    /** The method id used to dispatch <code>sendSpriteSignal</code> requests. */
    public static const SEND_SPRITE_SIGNAL :int = 9;

    // from interface RoomService
    public function sendSpriteSignal (arg1 :Client, arg2 :String, arg3 :ByteArray) :void
    {
        sendRequest(arg1, SEND_SPRITE_SIGNAL, [
            arg2, arg3
        ]);
    }

    /** The method id used to dispatch <code>setActorState</code> requests. */
    public static const SET_ACTOR_STATE :int = 10;

    // from interface RoomService
    public function setActorState (arg1 :Client, arg2 :ItemIdent, arg3 :int, arg4 :String) :void
    {
        sendRequest(arg1, SET_ACTOR_STATE, [
            arg2, Integer.valueOf(arg3), arg4
        ]);
    }

    /** The method id used to dispatch <code>spawnMob</code> requests. */
    public static const SPAWN_MOB :int = 11;

    // from interface RoomService
    public function spawnMob (arg1 :Client, arg2 :int, arg3 :String, arg4 :String, arg5 :Location, arg6 :InvocationService_InvocationListener) :void
    {
        var listener6 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener6.listener = arg6;
        sendRequest(arg1, SPAWN_MOB, [
            Integer.valueOf(arg2), arg3, arg4, arg5, listener6
        ]);
    }

    /** The method id used to dispatch <code>updateMemory</code> requests. */
    public static const UPDATE_MEMORY :int = 12;

    // from interface RoomService
    public function updateMemory (arg1 :Client, arg2 :EntityMemoryEntry, arg3 :InvocationService_ResultListener) :void
    {
        var listener3 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_MEMORY, [
            arg2, listener3
        ]);
    }

    /** The method id used to dispatch <code>updateRoom</code> requests. */
    public static const UPDATE_ROOM :int = 13;

    // from interface RoomService
    public function updateRoom (arg1 :Client, arg2 :SceneUpdate, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, UPDATE_ROOM, [
            arg2, listener3
        ]);
    }
}
}
