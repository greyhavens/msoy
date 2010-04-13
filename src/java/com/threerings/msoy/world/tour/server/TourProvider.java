//
// $Id$

package com.threerings.msoy.world.tour.server;

import javax.annotation.Generated;

import com.threerings.msoy.world.tour.client.TourService;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link TourService}.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from TourService.java.")
public interface TourProvider extends InvocationProvider
{
    /**
     * Handles a {@link TourService#endTour} request.
     */
    void endTour (ClientObject caller);

    /**
     * Handles a {@link TourService#nextRoom} request.
     */
    void nextRoom (ClientObject caller, boolean arg1, InvocationService.ResultListener arg2)
        throws InvocationException;
}
