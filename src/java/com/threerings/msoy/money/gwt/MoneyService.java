//
// $Id$

package com.threerings.msoy.money.gwt;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;

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
    HistoryListResult getTransactionHistory (int memberId, int from, int count)
        throws ServiceException;
}
