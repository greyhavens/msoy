//
// $Id$

package com.threerings.msoy.admin.client;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;

import com.threerings.msoy.admin.gwt.StatsModel;

/**
 * Peer-to-peer functionality needed by the admin services.
 */
public interface PeerAdminService extends InvocationService
{
    /**
     * Compiles statistics for the supplied report type. The result provided to the listener
     * depends on the type of statistics requested.
     */
    void compileStatistics (Client client, StatsModel.Type type, ResultListener listener);
}
