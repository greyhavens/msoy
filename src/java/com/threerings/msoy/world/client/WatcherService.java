//
// $Id: MsoySceneService.java 8844 2008-04-15 17:05:43Z nathan $

package com.threerings.msoy.world.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

public interface WatcherService extends InvocationService
{
    /**
     * Request notification on {@link WatcherReceiver} when the given member moves from one
     * scene to another. This is a peer-savvy watcher.
     */
    void addWatch (Client client, int memberId);
    /**
     * Stop watching the given member's scene-to-scene movements.
     */
    void clearWatch (Client client, int memberId);
}
