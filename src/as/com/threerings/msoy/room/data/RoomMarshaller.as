//
// $Id$

package com.threerings.msoy.room.data {

import flash.utils.ByteArray;

import com.threerings.io.TypedArray;

import com.threerings.util.Byte;
import com.threerings.util.Integer;
import com.threerings.util.langBoolean;

import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ConfirmMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ListenerMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.room.client.RoomService;

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
    /** The method id used to dispatch <code>addOrRemoveSong</code> requests. */
    public static const ADD_OR_REMOVE_SONG :int = 1;

    // from interface RoomService
    public function addOrRemoveSong (arg1 :int, arg2 :Boolean, arg3 :InvocationService_ConfirmListener) :void
    {
        var listener3 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(ADD_OR_REMOVE_SONG, [
            Integer.valueOf(arg1), langBoolean.valueOf(arg2), listener3
        ]);
    }

    /** The method id used to dispatch <code>bootDj</code> requests. */
    public static const BOOT_DJ :int = 2;

    // from interface RoomService
    public function bootDj (arg1 :int, arg2 :InvocationService_InvocationListener) :void
    {
        var listener2 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(BOOT_DJ, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>changeLocation</code> requests. */
    public static const CHANGE_LOCATION :int = 3;

    // from interface RoomService
    public function changeLocation (arg1 :ItemIdent, arg2 :Location) :void
    {
        sendRequest(CHANGE_LOCATION, [
            arg1, arg2
        ]);
    }

    /** The method id used to dispatch <code>despawnMob</code> requests. */
    public static const DESPAWN_MOB :int = 4;

    // from interface RoomService
    public function despawnMob (arg1 :int, arg2 :String, arg3 :InvocationService_InvocationListener) :void
    {
        var listener3 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(DESPAWN_MOB, [
            Integer.valueOf(arg1), arg2, listener3
        ]);
    }

    /** The method id used to dispatch <code>editRoom</code> requests. */
    public static const EDIT_ROOM :int = 5;

    // from interface RoomService
    public function editRoom (arg1 :InvocationService_ResultListener) :void
    {
        var listener1 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener1.listener = arg1;
        sendRequest(EDIT_ROOM, [
            listener1
        ]);
    }

    /** The method id used to dispatch <code>jumpToSong</code> requests. */
    public static const JUMP_TO_SONG :int = 6;

    // from interface RoomService
    public function jumpToSong (arg1 :int, arg2 :InvocationService_ConfirmListener) :void
    {
        var listener2 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(JUMP_TO_SONG, [
            Integer.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>moveMob</code> requests. */
    public static const MOVE_MOB :int = 7;

    // from interface RoomService
    public function moveMob (arg1 :int, arg2 :String, arg3 :Location, arg4 :InvocationService_InvocationListener) :void
    {
        var listener4 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(MOVE_MOB, [
            Integer.valueOf(arg1), arg2, arg3, listener4
        ]);
    }

    /** The method id used to dispatch <code>publishRoom</code> requests. */
    public static const PUBLISH_ROOM :int = 8;

    // from interface RoomService
    public function publishRoom (arg1 :InvocationService_InvocationListener) :void
    {
        var listener1 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener1.listener = arg1;
        sendRequest(PUBLISH_ROOM, [
            listener1
        ]);
    }

    /** The method id used to dispatch <code>quitDjing</code> requests. */
    public static const QUIT_DJING :int = 9;

    // from interface RoomService
    public function quitDjing () :void
    {
        sendRequest(QUIT_DJING, [
        ]);
    }

    /** The method id used to dispatch <code>rateRoom</code> requests. */
    public static const RATE_ROOM :int = 10;

    // from interface RoomService
    public function rateRoom (arg1 :int, arg2 :InvocationService_InvocationListener) :void
    {
        var listener2 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(RATE_ROOM, [
            Byte.valueOf(arg1), listener2
        ]);
    }

    /** The method id used to dispatch <code>requestControl</code> requests. */
    public static const REQUEST_CONTROL :int = 11;

    // from interface RoomService
    public function requestControl (arg1 :ItemIdent) :void
    {
        sendRequest(REQUEST_CONTROL, [
            arg1
        ]);
    }

    /** The method id used to dispatch <code>sendPostcard</code> requests. */
    public static const SEND_POSTCARD :int = 12;

    // from interface RoomService
    public function sendPostcard (arg1 :TypedArray /* of class java.lang.String */, arg2 :String, arg3 :String, arg4 :String, arg5 :InvocationService_ConfirmListener) :void
    {
        var listener5 :InvocationMarshaller_ConfirmMarshaller = new InvocationMarshaller_ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(SEND_POSTCARD, [
            arg1, arg2, arg3, arg4, listener5
        ]);
    }

    /** The method id used to dispatch <code>sendSpriteMessage</code> requests. */
    public static const SEND_SPRITE_MESSAGE :int = 13;

    // from interface RoomService
    public function sendSpriteMessage (arg1 :ItemIdent, arg2 :String, arg3 :ByteArray, arg4 :Boolean) :void
    {
        sendRequest(SEND_SPRITE_MESSAGE, [
            arg1, arg2, arg3, langBoolean.valueOf(arg4)
        ]);
    }

    /** The method id used to dispatch <code>sendSpriteSignal</code> requests. */
    public static const SEND_SPRITE_SIGNAL :int = 14;

    // from interface RoomService
    public function sendSpriteSignal (arg1 :String, arg2 :ByteArray) :void
    {
        sendRequest(SEND_SPRITE_SIGNAL, [
            arg1, arg2
        ]);
    }

    /** The method id used to dispatch <code>setActorState</code> requests. */
    public static const SET_ACTOR_STATE :int = 15;

    // from interface RoomService
    public function setActorState (arg1 :ItemIdent, arg2 :int, arg3 :String) :void
    {
        sendRequest(SET_ACTOR_STATE, [
            arg1, Integer.valueOf(arg2), arg3
        ]);
    }

    /** The method id used to dispatch <code>setTrackIndex</code> requests. */
    public static const SET_TRACK_INDEX :int = 16;

    // from interface RoomService
    public function setTrackIndex (arg1 :int, arg2 :int) :void
    {
        sendRequest(SET_TRACK_INDEX, [
            Integer.valueOf(arg1), Integer.valueOf(arg2)
        ]);
    }

    /** The method id used to dispatch <code>songEnded</code> requests. */
    public static const SONG_ENDED :int = 17;

    // from interface RoomService
    public function songEnded (arg1 :int) :void
    {
        sendRequest(SONG_ENDED, [
            Integer.valueOf(arg1)
        ]);
    }

    /** The method id used to dispatch <code>spawnMob</code> requests. */
    public static const SPAWN_MOB :int = 18;

    // from interface RoomService
    public function spawnMob (arg1 :int, arg2 :String, arg3 :String, arg4 :Location, arg5 :InvocationService_InvocationListener) :void
    {
        var listener5 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(SPAWN_MOB, [
            Integer.valueOf(arg1), arg2, arg3, arg4, listener5
        ]);
    }

    /** The method id used to dispatch <code>updateMemory</code> requests. */
    public static const UPDATE_MEMORY :int = 19;

    // from interface RoomService
    public function updateMemory (arg1 :ItemIdent, arg2 :String, arg3 :ByteArray, arg4 :InvocationService_ResultListener) :void
    {
        var listener4 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(UPDATE_MEMORY, [
            arg1, arg2, arg3, listener4
        ]);
    }

    /** The method id used to dispatch <code>updateRoom</code> requests. */
    public static const UPDATE_ROOM :int = 20;

    // from interface RoomService
    public function updateRoom (arg1 :SceneUpdate, arg2 :InvocationService_InvocationListener) :void
    {
        var listener2 :InvocationMarshaller_ListenerMarshaller = new InvocationMarshaller_ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(UPDATE_ROOM, [
            arg1, listener2
        ]);
    }
}
}
