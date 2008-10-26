//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.msoy.room.data.RoomObject;
import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.server.PresentsClient;

/**
 * Represents a bureau window connection.
 */
public class WindowServerClient extends PresentsClient
{
    @Override // from PresentsClient
    protected void subscribedToObject (DObject object)
    {
        super.subscribedToObject(object);
        if (object instanceof RoomObject) {
            ++_activeRooms;
            setThrottleFromRoomCount();
        }
    }

    @Override // from PresentsClient
    protected void unsubscribedFromObject (DObject object)
    {
        super.unsubscribedFromObject(object);
        if (object instanceof RoomObject) {
            --_activeRooms;
            setThrottleFromRoomCount();
        }
    }
    
    protected void setThrottleFromRoomCount ()
    {
        setIncomingMessageThrottle(Client.DEFAULT_MSGS_PER_SECOND * (_activeRooms + 1));
    }
    
    protected int _activeRooms;
}
