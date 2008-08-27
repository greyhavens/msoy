//
// $Id$

package com.threerings.msoy.money.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;

import com.threerings.msoy.web.data.ServiceException;
import com.threerings.msoy.money.data.all.MoneyHistory;

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
     List<MoneyHistory> getTransactionHistory (int memberId, int from, int count)
         throws ServiceException;
}
