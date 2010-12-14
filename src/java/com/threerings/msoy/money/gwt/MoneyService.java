//
// $Id$

package com.threerings.msoy.money.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;

import com.threerings.web.gwt.ServiceException;

import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.CashOutEntry;
import com.threerings.msoy.money.data.all.CharityBlingInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.data.all.TransactionPageResult;

/**
 * Provides currency related services.
 */
@RemoteServiceRelativePath(MoneyService.REL_PATH)
public interface MoneyService extends RemoteService
{
    /** The entry point for this service. */
    public static final String ENTRY_POINT = "/moneysvc";

    /** The relative path for this service. */
    public static final String REL_PATH = "../../.." + MoneyService.ENTRY_POINT;

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
     * Request a cashout of some amount of bling.  This will verify the user's password before
     * cashing out bling.
     */
    BlingInfo requestCashOutBling (
        int memberId, int blingAmount, String password, CashOutBillingInfo info)
        throws ServiceException;

    /**
     * Retrieve all members who are currently waiting for a bling cashout.
     */
    List<CashOutEntry> getBlingCashOutRequests ()
        throws ServiceException;

    /**
     * Performs the bling cashout the user requested earlier, for the specified amount of
     * centibling.
     */
    void cashOutBling (int memberId, int blingAmount)
        throws ServiceException;

    /**
     * Cancels a user's requested cash out.
     */
    void cancelCashOut (int memberId, String reason)
        throws ServiceException;

    /**
     * Used by support to deduct coins from a player as a logged transaction.
     */
    void supportAdjust (int memberId, Currency currency, int delta)
        throws ServiceException;

    /**
     * Retrieves bling info for all the charities in the system.
     */
    List<CharityBlingInfo> getCharityBlingInfo ();

    /**
     * Immediately cashes out the specified member (which must be a charity).  This will
     * automatically fill in blank billing information.
     */
    void charityCashOutBling (int memberId, int blingAmount)
        throws ServiceException;
}
