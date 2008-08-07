//
// $Id$

package com.threerings.msoy.room.server;

import com.threerings.msoy.room.client.WatcherDecoder;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationSender;

/**
 * Used to issue notifications to a {@link WatcherReceiver} instance on a
 * client.
 */
public class WatcherSender extends InvocationSender
{
    /**
     * Issues a notification that will result in a call to {@link
     * WatcherReceiver#memberMoved} on a client.
     */
    public static void memberMoved (
        ClientObject target, int arg1, int arg2, String arg3, int arg4)
    {
        sendNotification(
            target, WatcherDecoder.RECEIVER_CODE, WatcherDecoder.MEMBER_MOVED,
            new Object[] { Integer.valueOf(arg1), Integer.valueOf(arg2), arg3, Integer.valueOf(arg4) });
    }

}
