//
// $Id$

package client.util;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

/**
 * A utility method for setting up {@link RemoteService} handles.
 */
public class ServiceUtil
{
    /**
     * Binds the supplied service to the specified entry point.
     */
    public static Object bind (Object service, String entryPoint)
    {
        ((ServiceDefTarget)service).setServiceEntryPoint(entryPoint);
        return service;
    }
}
