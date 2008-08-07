//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.room.client.WatcherService;

import com.threerings.presents.client.Client;
import com.threerings.presents.data.InvocationMarshaller;

/**
 * Provides the implementation of the {@link WatcherService} interface
 * that marshalls the arguments and delivers the request to the provider
 * on the server. Also provides an implementation of the response listener
 * interfaces that marshall the response arguments and deliver them back
 * to the requesting client.
 */
public class WatcherMarshaller extends InvocationMarshaller
    implements WatcherService
{
    /** The method id used to dispatch {@link #addWatch} requests. */
    public static final int ADD_WATCH = 1;

    // from interface WatcherService
    public void addWatch (Client arg1, int arg2)
    {
        sendRequest(arg1, ADD_WATCH, new Object[] {
            Integer.valueOf(arg2)
        });
    }

    /** The method id used to dispatch {@link #clearWatch} requests. */
    public static final int CLEAR_WATCH = 2;

    // from interface WatcherService
    public void clearWatch (Client arg1, int arg2)
    {
        sendRequest(arg1, CLEAR_WATCH, new Object[] {
            Integer.valueOf(arg2)
        });
    }
}
