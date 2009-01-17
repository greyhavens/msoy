//
// $Id$

package com.threerings.msoy.server.util;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.util.PersistingUnit;

import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Adds support for ServiceException.
 */
public abstract class ServiceUnit extends PersistingUnit
{
    public ServiceUnit (String name, InvocationService.InvocationListener listener)
    {
        super(name, listener);
    }

    public ServiceUnit (String name, InvocationService.InvocationListener listener, Object... args)
    {
        super(name, listener, args);
    }

    @Override
    public void handleFailure (Exception error)
    {
        if (error instanceof ServiceException) {
            _listener.requestFailed(error.getMessage());
        } else {
            super.handleFailure(error);
        }
    }
}
