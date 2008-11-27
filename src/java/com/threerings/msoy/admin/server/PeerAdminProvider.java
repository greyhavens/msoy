//
// $Id$

package com.threerings.msoy.admin.server;

import com.threerings.msoy.admin.client.PeerAdminService;
import com.threerings.msoy.admin.gwt.StatsModel;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.server.InvocationProvider;

/**
 * Defines the server-side of the {@link PeerAdminService}.
 */
public interface PeerAdminProvider extends InvocationProvider
{
    /**
     * Handles a {@link PeerAdminService#compileStatistics} request.
     */
    void compileStatistics (ClientObject caller, StatsModel.Type arg1, InvocationService.ResultListener arg2)
        throws InvocationException;
}
