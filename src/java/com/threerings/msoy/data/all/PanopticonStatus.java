//
// $Id$

package com.threerings.msoy.data.all;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains the current state of the Panopticon logger client.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
public class PanopticonStatus
    implements IsSerializable
{
    public int currentlyQueued;
    public long totalQueued;
    public long dropped;
    public long totalSent;
    public long overflowed;
    public Date lastTimeQueued;
    public Date lastTimeSent;
    public Date lastTimeDropped;
    public Date lastTimeOverflowed;
    public Date lastTimeQueueOverflowed;
    public Date timeStarted;
    public Date lastTimeEnteredRetryMode;
    public Date lastTimeRecoveredFromRetryMode;
    public Date lastTimeTempFailed;
    public String lastTempFailureInfo;
    public String lastPermFailureInfo;
    
    public boolean inRetryMode;
    public boolean disposed;
    public boolean connectedToServer;
    public boolean senderDisposed;
    public boolean persistenceManagerDisposed;
}
