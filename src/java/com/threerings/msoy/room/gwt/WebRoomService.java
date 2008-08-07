//
// $Id$

package com.threerings.msoy.room.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.web.data.WebIdent;

/**
 * Provides information related to the world.
 */
public interface WebRoomService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/roomsvc";

    /**
     * Fetch the n most Popular Places data in JSON-serialized form.
     */
    String serializePopularPlaces (WebIdent ident, int n)
        throws ServiceException;

    /**
     * Loads information on a particular room.
     */
    RoomInfo loadRoomInfo (int sceneId)
        throws ServiceException;
}
