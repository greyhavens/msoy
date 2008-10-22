//
// $Id$

package com.threerings.msoy.world.tour.client {

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.client.InvocationService_ResultListener;

/**
 * An ActionScript version of the Java TourService interface.
 */
public interface TourService extends InvocationService
{
    // from Java interface TourService
    function endTour (arg1 :Client) :void;

    // from Java interface TourService
    function nextRoom (arg1 :Client, arg2 :InvocationService_ResultListener) :void;
}
}
