//
// $Id$

package com.threerings.msoy.money.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;

/**
 * Provides currency related services.
 */
public interface MoneyService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/moneysvc";

    /**
     * Fetches a player's complete income and spending history.
     */
    PagedResult<MoneyTransaction> getTransactionHistory (
        int memberId, ReportType report, int from, int count)
        throws ServiceException;
    
    BlingInfo getBlingInfo (int memberId)
        throws ServiceException;
}
