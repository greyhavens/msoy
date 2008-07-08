//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.msoy.bureau.client.BureauLauncherDecoder;
import com.threerings.msoy.bureau.client.BureauLauncherReceiver;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.net.Transport;
import com.threerings.presents.server.InvocationSender;

/**
 * Used to issue notifications to a {@link BureauLauncherReceiver} instance on a
 * client.
 */
public class BureauLauncherSender extends InvocationSender
{
    /**
     * Issues a notification that will result in a call to {@link
     * BureauLauncherReceiver#launchThane} on a client.
     */
    public static void launchThane (
        ClientObject target, String arg1, String arg2, String arg3, int arg4)
    {
        sendNotification(
            target, BureauLauncherDecoder.RECEIVER_CODE, BureauLauncherDecoder.LAUNCH_THANE,
            new Object[] { arg1, arg2, arg3, Integer.valueOf(arg4) });
    }

}
