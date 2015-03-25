//
// $Id$

package com.threerings.msoy.admin.data;

import javax.annotation.Generated;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;

import com.threerings.msoy.admin.client.PeerAdminService;
import com.threerings.msoy.admin.gwt.StatsModel;

/**
 * Provides the implementation of the {@link PeerAdminService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
@Generated(value={"com.threerings.presents.tools.GenServiceTask"},
           comments="Derived from PeerAdminService.java.")
public class PeerAdminMarshaller extends InvocationMarshaller<ClientObject>
    implements PeerAdminService
{
    /** The method id used to dispatch {@link #compileStatistics} requests. */
    public static final int COMPILE_STATISTICS = 1;

    // from interface PeerAdminService
    public void compileStatistics (StatsModel.Type arg1, InvocationService.ResultListener arg2)
    {
        InvocationMarshaller.ResultMarshaller listener2 = new InvocationMarshaller.ResultMarshaller();
        listener2.listener = arg2;
        sendRequest(COMPILE_STATISTICS, new Object[] {
            arg1, listener2
        });
    }
}
