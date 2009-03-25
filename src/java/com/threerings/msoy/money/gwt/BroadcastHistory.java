//
// $Id$

package com.threerings.msoy.money.gwt;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class BroadcastHistory
    implements IsSerializable
{
    /** Time the message was posted. */
    public Date timeSent;

    /** Member who posted it. */
    public int memberId;

    /** Number of bars paid for it. */
    public int barsPaid;

    /** The content of the message. */
    public String message;
}
