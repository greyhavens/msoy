//
// $Id$

package com.threerings.msoy.bureau.server;

import com.threerings.msoy.room.data.RoomObject;
import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.server.PresentsSession;

/**
 * Represents a bureau window connection.
 */
public class WindowSession extends PresentsSession
{
    @Override // from PresentsSession
    protected void subscribedToObject (DObject object)
    {
        super.subscribedToObject(object);
        if (object instanceof RoomObject) {
            ++_activeRooms;
            setThrottleFromRoomCount();
        }
    }

    @Override // from PresentsSession
    protected void unsubscribedFromObject (DObject object)
    {
        super.unsubscribedFromObject(object);
        if (object instanceof RoomObject) {
            --_activeRooms;
            setThrottleFromRoomCount();
        }
    }

    @Override
    protected void clearSubscrips (boolean verbose)
    {
        boolean wasClearing = _clearing;
        _clearing = true;
        try {
            super.clearSubscrips(verbose);
        } finally {
            _clearing = wasClearing;
        }

        setThrottleFromRoomCount();
    }

    protected void setThrottleFromRoomCount ()
    {
        if (!_clearing) {
            if (_omgr.isDispatchThread()) {
                setIncomingMessageThrottle(
                    Client.DEFAULT_MSGS_PER_SECOND * Math.max(_activeRooms, 1));

            } else {
                final int roomCount = _activeRooms;
                _omgr.postRunnable(new Runnable () {
                    public void run () {
                        if (roomCount == _activeRooms) {
                            setIncomingMessageThrottle(
                                Client.DEFAULT_MSGS_PER_SECOND * Math.max(_activeRooms, 1));
                        }
                    }
                });
            }
        }
    }
    
    protected int _activeRooms;
    protected boolean _clearing;
}
