//
// $Id$

package com.threerings.msoy.money.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.data.all.TransactionPageResult;

/**
 * Provides currency related services.
 */
public interface MoneyService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/moneysvc";

    /**
     * Fetches a player's complete income and spending history, one page at a time.
     */
    TransactionPageResult getTransactionHistory (
        int memberId, ReportType report, int from, int count)
        throws ServiceException;
    
    /**
     * Exchanges some amount of bling for bars.
     */
    BlingExchangeResult exchangeBlingForBars (int memberId, int blingAmount)
        throws ServiceException;
    
    /**
     * Request a cashout of some amount of bling.
     */
    BlingInfo requestCashOutBling (int memberId, int blingAmount)
        throws ServiceException;
}
