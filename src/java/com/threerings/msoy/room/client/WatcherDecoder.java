//
// $Id$

package com.threerings.msoy.room.client;

import com.threerings.presents.client.InvocationDecoder;

/**
 * Dispatches calls to a {@link WatcherReceiver} instance.
 */
public class WatcherDecoder extends InvocationDecoder
{
    /** The generated hash code used to identify this receiver class. */
    public static final String RECEIVER_CODE = "ce12935f1f33f05cec2d1c5d78ec71f1";

    /** The method id used to dispatch {@link WatcherReceiver#memberMoved}
     * notifications. */
    public static final int MEMBER_MOVED = 1;

    /**
     * Creates a decoder that may be registered to dispatch invocation
     * service notifications to the specified receiver.
     */
    public WatcherDecoder (WatcherReceiver receiver)
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
        case MEMBER_MOVED:
            ((WatcherReceiver)receiver).memberMoved(
                ((Integer)args[0]).intValue(), ((Integer)args[1]).intValue(), (String)args[2], ((Integer)args[3]).intValue()
            );
            return;

        default:
            super.dispatchNotification(methodId, args);
            return;
        }
    }
}
