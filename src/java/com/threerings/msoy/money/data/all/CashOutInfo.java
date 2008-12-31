//
// $Id$

package com.threerings.msoy.money.data.all;

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

public class CashOutInfo
    implements IsSerializable
{
    /** The amount of bling the user has requested to cash out. */
    public int blingAmount;

    /** The worth of the bling in USD cents the user has requested to cash out. */
    public int blingWorth;

    public CashOutBillingInfo billingInfo;

    public Date timeRequested;

    public Date timeCompleted;

    public boolean successful;

    public Integer actualAmountCashedOut;

    public String failureReason;

    public CashOutInfo (int blingAmount, int blingWorth, CashOutBillingInfo billingInfo,
            Date timeRequested, Date timeCompleted, boolean successful,
            Integer actualAmountCashedOut, String failureReason)
    {
        this.blingAmount = blingAmount;
        this.blingWorth = blingWorth;
        this.billingInfo = billingInfo;
        this.timeRequested = timeRequested;
        this.timeCompleted = timeCompleted;
        this.successful = successful;
        this.actualAmountCashedOut = actualAmountCashedOut;
        this.failureReason = failureReason;
    }

    /** For serialization. */
    public CashOutInfo () { }
}
