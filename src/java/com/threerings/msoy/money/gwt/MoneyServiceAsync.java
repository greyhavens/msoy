//
// $Id$

package com.threerings.msoy.money.gwt;

import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.money.data.all.BlingExchangeResult;
import com.threerings.msoy.money.data.all.BlingInfo;
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
    void exchangeBlingForBars (int memberId, int blingAmount, 
        AsyncCallback<BlingExchangeResult> callback);

    /**
     * The asynchronous version of {@link MoneyService#requestCashOutBling}.
     */
    void requestCashOutBling (int memberId, int blingAmount, AsyncCallback<BlingInfo> callback);
}

