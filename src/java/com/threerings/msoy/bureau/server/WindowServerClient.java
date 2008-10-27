//
// $Id$

package com.threerings.msoy.bureau.server;

import com.samskivert.util.Throttle;
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

    // HACK: override the throttle until client side issues can be resolved.
    @Override protected Throttle createIncomingMessageThrottle ()
    {
        int averageRoomCount = 4;
        return new Throttle(10*(Client.DEFAULT_MSGS_PER_SECOND+1)*averageRoomCount, 10*1000L);
    }

    protected void setThrottleFromRoomCount ()
    {
        if (false) { // bailing on dynamic throttle for the moment, client issues prevent this from working
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
        }}
    }
    
    protected int _activeRooms;
    protected boolean _clearing;
}
