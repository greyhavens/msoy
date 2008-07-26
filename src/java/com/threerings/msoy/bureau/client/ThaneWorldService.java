package com.threerings.msoy.bureau.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Service for thane clients to access world information.
 */
public interface ThaneWorldService extends InvocationService
{
    /**
     * Lookup the room object id for a given scene. If successful, the listener will be called 
     * with an Integer containing the id of the room object.
     */
    void locateRoom (Client caller, int sceneId, ResultListener listener);
}
