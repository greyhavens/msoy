//
// $Id$

package com.threerings.msoy.bureau.client;

import com.threerings.presents.client.InvocationDecoder;

/**
 * Dispatches calls to a {@link BureauLauncherReceiver} instance.
 */
public class BureauLauncherDecoder extends InvocationDecoder
{
    /** The generated hash code used to identify this receiver class. */
    public static final String RECEIVER_CODE = "e594037aadf57c8011c9ecbb0c28050a";

    /** The method id used to dispatch {@link BureauLauncherReceiver#launchThane}
     * notifications. */
    public static final int LAUNCH_THANE = 1;

    /** The method id used to dispatch {@link BureauLauncherReceiver#requestInfo}
     * notifications. */
    public static final int REQUEST_INFO = 2;

    /** The method id used to dispatch {@link BureauLauncherReceiver#shutdownLauncher}
     * notifications. */
    public static final int SHUTDOWN_LAUNCHER = 3;

    /**
     * Creates a decoder that may be registered to dispatch invocation
     * service notifications to the specified receiver.
     */
    public BureauLauncherDecoder (BureauLauncherReceiver receiver)
    {
        this.receiver = receiver;
    }

    @Override // documentation inherited
    public String getReceiverCode ()
    {
        return RECEIVER_CODE;
    }

    @Override // documentation inherited
    public void dispatchNotification (int methodId, Object[] args)
    {
        switch (methodId) {
        case LAUNCH_THANE:
            ((BureauLauncherReceiver)receiver).launchThane(
                (String)args[0], (String)args[1]
            );
            return;

        case REQUEST_INFO:
            ((BureauLauncherReceiver)receiver).requestInfo(
                (String)args[0], ((Integer)args[1]).intValue()
            );
            return;

        case SHUTDOWN_LAUNCHER:
            ((BureauLauncherReceiver)receiver).shutdownLauncher(

            );
            return;

        default:
            super.dispatchNotification(methodId, args);
            return;
        }
    }
}
