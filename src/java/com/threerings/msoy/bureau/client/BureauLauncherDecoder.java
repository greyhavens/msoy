//
// $Id$

package com.threerings.msoy.bureau.client;

import com.threerings.msoy.bureau.client.BureauLauncherReceiver;
import com.threerings.presents.client.InvocationDecoder;

/**
 * Dispatches calls to a {@link BureauLauncherReceiver} instance.
 */
public class BureauLauncherDecoder extends InvocationDecoder
{
    /** The generated hash code used to identify this receiver class. */
    public static final String RECEIVER_CODE = "f8fde6d3f7a50bb8043549bf1f5b9cb4";

    /** The method id used to dispatch {@link BureauLauncherReceiver#launchThane}
     * notifications. */
    public static final int LAUNCH_THANE = 1;

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
                (String)args[0], (String)args[1], (String)args[2], ((Integer)args[3]).intValue()
            );
            return;

        default:
            super.dispatchNotification(methodId, args);
            return;
        }
    }
}
