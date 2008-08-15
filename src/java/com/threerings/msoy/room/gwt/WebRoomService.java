//
// $Id$

package com.threerings.msoy.room.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;

/**
 * Provides information related to the world.
 */
public interface WebRoomService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/roomsvc";

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
}
