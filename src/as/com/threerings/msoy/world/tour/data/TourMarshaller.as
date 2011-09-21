//
// $Id$

package com.threerings.msoy.world.tour.data {

import com.threerings.util.langBoolean;

import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.data.InvocationMarshaller_ResultMarshaller;

import com.threerings.msoy.world.tour.client.TourService;

/**
 * Provides the implementation of the <code>TourService</code> interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class TourMarshaller extends InvocationMarshaller
    implements TourService
{
    /** The method id used to dispatch <code>endTour</code> requests. */
    public static const END_TOUR :int = 1;

    // from interface TourService
    public function endTour () :void
    {
        sendRequest(END_TOUR, [
        ]);
    }

    /** The method id used to dispatch <code>nextRoom</code> requests. */
    public static const NEXT_ROOM :int = 2;

    // from interface TourService
    public function nextRoom (arg1 :Boolean, arg2 :InvocationService_ResultListener) :void
    {
        var listener2 :InvocationMarshaller_ResultMarshaller = new InvocationMarshaller_ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(NEXT_ROOM, [
            langBoolean.valueOf(arg1), listener2
        ]);
    }
}
}
