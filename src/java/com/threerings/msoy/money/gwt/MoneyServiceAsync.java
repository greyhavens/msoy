//
// $Id$

package com.threerings.msoy.money.gwt;

import java.util.List;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.CashOutBillingInfo;
import com.threerings.msoy.money.data.all.CashOutEntry;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.MoneyResult;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.data.all.TransactionPageResult;

/**
 * The asynchronous (client-side) version of {@link MoneyService}.
 */
public interface MoneyServiceAsync
{
    /**
     * The asynchronous version of {@link MoneyService#getTransactionHistory}.
     */
    void getTransactionHistory (
        int memberId, ReportType report, int from, int count,
        AsyncCallback<TransactionPageResult> callback);
    
    /**
     * The asynchronous version of {@link MoneyService#exchangeBlingForBars}.
     */
    void exchangeBlingForBars (
        int memberId, int blingAmount, AsyncCallback<MoneyResult> callback);

    /**
     * The asynchronous version of {@link MoneyService#requestCashOutBling}.
     */
    void requestCashOutBling (
        int memberId, int blingAmount, String password, CashOutBillingInfo info,
        AsyncCallback<BlingInfo> callback);
    
    /**
     * The asynchronous version of {@link MoneyService#getBlingCashOutRequests}.
     */
    void getBlingCashOutRequests (AsyncCallback<List<CashOutEntry>> callback);

    /**
     * The asynchronous version of {@link MoneyService#supportAdjust}.
     */
    void supportAdjust (int memberId, Currency currency, int delta, AsyncCallback<Void> callback);
    
    /**
     * The asynchronous version of {@link MoneyService#cashOutBling}.
     */
    void cashOutBling (int memberId, int blingAmount, AsyncCallback<Void> callback);
    
    /**
     * The asynchronous version of {@link MoneyService#cancelCashOut}.
     */
    void cancelCashOut (int memberId, String reason, AsyncCallback<Void> callback);
}

