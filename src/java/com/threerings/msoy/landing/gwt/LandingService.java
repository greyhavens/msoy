//
// $Id$

package com.threerings.msoy.landing.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Provides information to our landing pages.
 */
public interface LandingService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/landingsvc";

    /**
     * Loads the data for the landing page.
     */
    public LandingData getLandingData ()
        throws ServiceException;
}
