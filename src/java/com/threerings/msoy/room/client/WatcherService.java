//
// $Id: MsoySceneService.java 8844 2008-04-15 17:05:43Z nathan $

package com.threerings.msoy.room.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

public interface WatcherService extends InvocationService
{
    /**
     * Request notification on {@link WatcherReceiver} when the given member moves from one
     * scene to another. This is a peer-savvy watcher. After establishing the watch, the service's
     * provider will also inform the <code>WatcherReceiver</code> of the member's current location 
     * by dispatching an initial location update.
     */
    void addWatch (Client client, int memberId);

    /**
     * Stop watching the given member's scene-to-scene movements.
     */
    void clearWatch (Client client, int memberId);
}
