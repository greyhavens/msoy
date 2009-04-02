package com.threerings.msoy.money.server;

import com.threerings.msoy.web.gwt.ServiceException;

/**
 * Indicates that the user attempted to cash out bling within less than a certain minimum amount
 * of time since their last request.
 */
public class CashedOutTooRecentlyException extends MoneyException
{
    /** Member being denied the cash out request. */
    public int memberId;

    /** Time in milliseconds the user will have to wait until their next cash out. */
    public long waitTime;

    public CashedOutTooRecentlyException (int memberId, long waitTime)
    {
        super("Member " + memberId + " requested bling cash out too soon, must wait " + waitTime +
            "milliseconds");
        this.memberId = memberId;
        this.waitTime = waitTime;
    }

    @Override
    public ServiceException toServiceException ()
    {
        return new ServiceException("e.cashed_out_too_recently");
    }
}
