//
// $Id$

package com.threerings.msoy.world.tour.data;

import com.threerings.msoy.world.tour.client.TourService;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link TourService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class TourMarshaller extends InvocationMarshaller
    implements TourService
{
    /** The method id used to dispatch {@link #endTour} requests. */
    public static final int END_TOUR = 1;

    // from interface TourService
    public void endTour (Client arg1)
    {
        sendRequest(arg1, END_TOUR, new Object[] {});
    }

    /** The method id used to dispatch {@link #nextRoom} requests. */
    public static final int NEXT_ROOM = 2;

    // from interface TourService
    public void nextRoom (Client arg1, boolean arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, NEXT_ROOM, new Object[] {
            Boolean.valueOf(arg2), listener3
        });
    }
}
