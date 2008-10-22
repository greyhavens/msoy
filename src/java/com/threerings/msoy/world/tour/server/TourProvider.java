//
// $Id$

package com.threerings.msoy.world.tour.server;

import com.threerings.msoy.world.tour.client.TourService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link TourService}.
 */
public interface TourProvider extends InvocationProvider
{
    /**
     * Handles a {@link TourService#endTour} request.
     */
    void endTour (ClientObject caller);

    /**
     * Handles a {@link TourService#nextRoom} request.
     */
    void nextRoom (ClientObject caller, InvocationService.ResultListener arg1)
        throws InvocationException;
}
