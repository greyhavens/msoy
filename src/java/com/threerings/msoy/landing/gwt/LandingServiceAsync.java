//
// $Id$

package com.threerings.msoy.landing.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;

/**
 * The asynchronous (client-side) version of {@link LandingService}.
 */
public interface LandingServiceAsync
{
    /**
     * The asynchronous version of {@link LandingService#getLandingData}.
     */
    void getLandingData (AsyncCallback<LandingData> callback);
}
