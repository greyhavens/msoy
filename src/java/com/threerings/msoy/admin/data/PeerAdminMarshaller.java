//
// $Id$

package com.threerings.msoy.admin.data;

import com.threerings.msoy.admin.client.PeerAdminService;
import com.threerings.msoy.admin.gwt.StatsModel;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link PeerAdminService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class PeerAdminMarshaller extends InvocationMarshaller
    implements PeerAdminService
{
    /** The method id used to dispatch {@link #compileStatistics} requests. */
    public static final int COMPILE_STATISTICS = 1;

    // from interface PeerAdminService
    public void compileStatistics (Client arg1, StatsModel.Type arg2, InvocationService.ResultListener arg3)
    {
        InvocationMarshaller.ResultMarshaller listener3 = new InvocationMarshaller.ResultMarshaller();
        listener3.listener = arg3;
        sendRequest(arg1, COMPILE_STATISTICS, new Object[] {
            arg2, listener3
        });
    }
}
