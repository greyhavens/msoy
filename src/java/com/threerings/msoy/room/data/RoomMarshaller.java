//
// $Id$

package com.threerings.msoy.room.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Location;

import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.room.client.RoomService;

/**
 * Provides the implementation of the {@link RoomService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from RoomService.java.")
public class RoomMarshaller extends InvocationMarshaller<ClientObject>
    implements RoomService
{
    /** The method id used to dispatch {@link #addOrRemoveSong} requests. */
    public static final int ADD_OR_REMOVE_SONG = 1;

    // from interface RoomService
    public void addOrRemoveSong (int arg1, boolean arg2, InvocationService.ConfirmListener arg3)
    {
        InvocationMarshaller.ConfirmMarshaller listener3 = new InvocationMarshaller.ConfirmMarshaller();
        listener3.listener = arg3;
        sendRequest(ADD_OR_REMOVE_SONG, new Object[] {
            Integer.valueOf(arg1), Boolean.valueOf(arg2), listener3
        });
    }

    /** The method id used to dispatch {@link #bootDj} requests. */
    public static final int BOOT_DJ = 2;

    // from interface RoomService
    public void bootDj (int arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(BOOT_DJ, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #changeLocation} requests. */
    public static final int CHANGE_LOCATION = 3;

    // from interface RoomService
    public void changeLocation (ItemIdent arg1, Location arg2)
    {
        sendRequest(CHANGE_LOCATION, new Object[] {
            arg1, arg2
        });
    }

    /** The method id used to dispatch {@link #despawnMob} requests. */
    public static final int DESPAWN_MOB = 4;

    // from interface RoomService
    public void despawnMob (int arg1, String arg2, InvocationService.InvocationListener arg3)
    {
        ListenerMarshaller listener3 = new ListenerMarshaller();
        listener3.listener = arg3;
        sendRequest(DESPAWN_MOB, new Object[] {
            Integer.valueOf(arg1), arg2, listener3
        });
    }

    /** The method id used to dispatch {@link #editRoom} requests. */
    public static final int EDIT_ROOM = 5;

    // from interface RoomService
    public void editRoom (InvocationService.ResultListener arg1)
    {
        InvocationMarshaller.ResultMarshaller listener1 = new InvocationMarshaller.ResultMarshaller();
        listener1.listener = arg1;
        sendRequest(EDIT_ROOM, new Object[] {
            listener1
        });
    }

    /** The method id used to dispatch {@link #jumpToSong} requests. */
    public static final int JUMP_TO_SONG = 6;

    // from interface RoomService
    public void jumpToSong (int arg1, InvocationService.ConfirmListener arg2)
    {
        InvocationMarshaller.ConfirmMarshaller listener2 = new InvocationMarshaller.ConfirmMarshaller();
        listener2.listener = arg2;
        sendRequest(JUMP_TO_SONG, new Object[] {
            Integer.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #moveMob} requests. */
    public static final int MOVE_MOB = 7;

    // from interface RoomService
    public void moveMob (int arg1, String arg2, Location arg3, InvocationService.InvocationListener arg4)
    {
        ListenerMarshaller listener4 = new ListenerMarshaller();
        listener4.listener = arg4;
        sendRequest(MOVE_MOB, new Object[] {
            Integer.valueOf(arg1), arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #publishRoom} requests. */
    public static final int PUBLISH_ROOM = 8;

    // from interface RoomService
    public void publishRoom (InvocationService.InvocationListener arg1)
    {
        ListenerMarshaller listener1 = new ListenerMarshaller();
        listener1.listener = arg1;
        sendRequest(PUBLISH_ROOM, new Object[] {
            listener1
        });
    }

    /** The method id used to dispatch {@link #quitDjing} requests. */
    public static final int QUIT_DJING = 9;

    // from interface RoomService
    public void quitDjing ()
    {
        sendRequest(QUIT_DJING, new Object[] {
        });
    }

    /** The method id used to dispatch {@link #rateRoom} requests. */
    public static final int RATE_ROOM = 10;

    // from interface RoomService
    public void rateRoom (byte arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(RATE_ROOM, new Object[] {
            Byte.valueOf(arg1), listener2
        });
    }

    /** The method id used to dispatch {@link #rateTrack} requests. */
    public static final int RATE_TRACK = 11;

    // from interface RoomService
    public void rateTrack (int arg1, boolean arg2)
    {
        sendRequest(RATE_TRACK, new Object[] {
            Integer.valueOf(arg1), Boolean.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #requestControl} requests. */
    public static final int REQUEST_CONTROL = 12;

    // from interface RoomService
    public void requestControl (ItemIdent arg1)
    {
        sendRequest(REQUEST_CONTROL, new Object[] {
            arg1
        });
    }

    /** The method id used to dispatch {@link #sendPostcard} requests. */
    public static final int SEND_POSTCARD = 13;

    // from interface RoomService
    public void sendPostcard (String[] arg1, String arg2, String arg3, String arg4, InvocationService.ConfirmListener arg5)
    {
        InvocationMarshaller.ConfirmMarshaller listener5 = new InvocationMarshaller.ConfirmMarshaller();
        listener5.listener = arg5;
        sendRequest(SEND_POSTCARD, new Object[] {
            arg1, arg2, arg3, arg4, listener5
        });
    }

    /** The method id used to dispatch {@link #sendSpriteMessage} requests. */
    public static final int SEND_SPRITE_MESSAGE = 14;

    // from interface RoomService
    public void sendSpriteMessage (ItemIdent arg1, String arg2, byte[] arg3, boolean arg4)
    {
        sendRequest(SEND_SPRITE_MESSAGE, new Object[] {
            arg1, arg2, arg3, Boolean.valueOf(arg4)
        });
    }

    /** The method id used to dispatch {@link #sendSpriteSignal} requests. */
    public static final int SEND_SPRITE_SIGNAL = 15;

    // from interface RoomService
    public void sendSpriteSignal (String arg1, byte[] arg2)
    {
        sendRequest(SEND_SPRITE_SIGNAL, new Object[] {
            arg1, arg2
        });
    }

    /** The method id used to dispatch {@link #setActorState} requests. */
    public static final int SET_ACTOR_STATE = 16;

    // from interface RoomService
    public void setActorState (ItemIdent arg1, int arg2, String arg3)
    {
        sendRequest(SET_ACTOR_STATE, new Object[] {
            arg1, Integer.valueOf(arg2), arg3
        });
    }

    /** The method id used to dispatch {@link #setTrackIndex} requests. */
    public static final int SET_TRACK_INDEX = 17;

    // from interface RoomService
    public void setTrackIndex (int arg1, int arg2)
    {
        sendRequest(SET_TRACK_INDEX, new Object[] {
            Integer.valueOf(arg1), Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #songEnded} requests. */
    public static final int SONG_ENDED = 18;

    // from interface RoomService
    public void songEnded (int arg1)
    {
        sendRequest(SONG_ENDED, new Object[] {
            Integer.valueOf(arg1)
        });
    }

    /** The method id used to dispatch {@link #spawnMob} requests. */
    public static final int SPAWN_MOB = 19;

    // from interface RoomService
    public void spawnMob (int arg1, String arg2, String arg3, Location arg4, InvocationService.InvocationListener arg5)
    {
        ListenerMarshaller listener5 = new ListenerMarshaller();
        listener5.listener = arg5;
        sendRequest(SPAWN_MOB, new Object[] {
            Integer.valueOf(arg1), arg2, arg3, arg4, listener5
        });
    }

    /** The method id used to dispatch {@link #updateMemory} requests. */
    public static final int UPDATE_MEMORY = 20;

    // from interface RoomService
    public void updateMemory (ItemIdent arg1, String arg2, byte[] arg3, InvocationService.ResultListener arg4)
    {
        InvocationMarshaller.ResultMarshaller listener4 = new InvocationMarshaller.ResultMarshaller();
        listener4.listener = arg4;
        sendRequest(UPDATE_MEMORY, new Object[] {
            arg1, arg2, arg3, listener4
        });
    }

    /** The method id used to dispatch {@link #updateRoom} requests. */
    public static final int UPDATE_ROOM = 21;

    // from interface RoomService
    public void updateRoom (SceneUpdate arg1, InvocationService.InvocationListener arg2)
    {
        ListenerMarshaller listener2 = new ListenerMarshaller();
        listener2.listener = arg2;
        sendRequest(UPDATE_ROOM, new Object[] {
            arg1, listener2
        });
    }
}
