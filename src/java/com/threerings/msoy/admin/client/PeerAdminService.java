//
// $Id$

package com.threerings.msoy.admin.client;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;

import com.threerings.msoy.admin.gwt.StatsModel;

/**
 * Peer-to-peer functionality needed by the admin services.
 */
public interface PeerAdminService extends InvocationService<ClientObject>
{
    /**
     * Compiles statistics for the supplied report type. The result provided to the listener
     * depends on the type of statistics requested.
     */
    void compileStatistics (StatsModel.Type type, ResultListener listener);
}
