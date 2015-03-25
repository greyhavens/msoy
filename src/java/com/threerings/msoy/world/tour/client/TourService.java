//
// $Id$

package com.threerings.msoy.world.tour.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

/**
 * Service for the "Whirled Tour".
 */
public interface TourService extends InvocationService<ClientObject>
{
    /**
     * If the caller is not yet touring, start them touring.
     * Then, proceed to the next room on the tour.
     */
    void nextRoom (boolean finishedLoadingCurrentRoom, ResultListener listener);

    /**
     * End any tour the client is involved in. There is no listener because this cannot fail.
     */
    void endTour ();
}
