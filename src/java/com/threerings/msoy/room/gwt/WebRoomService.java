//
// $Id$

package com.threerings.msoy.room.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;

/**
 * Provides information related to the world.
 */
public interface WebRoomService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/roomsvc";

    /** Delivers the respose to {@link #getGroupRooms}. */
    public static class RoomsResult implements IsSerializable
    {
        /**
         * The rooms of this group.
         */
        public List<RoomInfo> groupRooms;

        /**
         * The rooms owned by the caller.
         */
        public List<RoomInfo> callerRooms;
    }

    /**
     * Loads information on a particular room.
     */
    RoomInfo loadRoomInfo (int sceneId)
        throws ServiceException;

    /**
     * Loads the list of rooms owned by the calling user.
     */
    List<RoomInfo> loadMyRooms ()
        throws ServiceException;

    /**
     * Returns a list of all the rooms owned by a specific group.
     */
    RoomsResult loadGroupRooms (int groupId)
        throws ServiceException;
}
