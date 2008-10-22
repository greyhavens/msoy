//
// $Id

package com.threerings.msoy.world.tour.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

/**
 * Service for the "Whirled Tour".
 */
public interface TourService extends InvocationService
{
    /**
     * If the caller is not yet touring, start them touring.
     * Then, proceed to the next room on the tour.
     */
    void nextRoom (Client client, ResultListener listener);

    /**
     * End any tour the client is involved in. There is no listener because this cannot fail.
     */
    void endTour (Client client);
}
