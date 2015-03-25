//
// $Id$

package com.threerings.msoy.world.tour.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.world.tour.client.TourService;

/**
 * Provides the implementation of the {@link TourService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from TourService.java.")
public class TourMarshaller extends InvocationMarshaller<ClientObject>
    implements TourService
{
    /** The method id used to dispatch {@link #endTour} requests. */
    public static final int END_TOUR = 1;

    // from interface TourService
    public void endTour ()
    {
        sendRequest(END_TOUR, new Object[] {
        });
    }

    /** The method id used to dispatch {@link #nextRoom} requests. */
    public static final int NEXT_ROOM = 2;

    // from interface TourService
    public void nextRoom (boolean arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(NEXT_ROOM, new Object[] {
            Boolean.valueOf(arg1), listener2
        });
    }
}
