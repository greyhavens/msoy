//
// $Id$

package client.shop;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.HistoryListResult;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

/**
 * Data model for service backed balance sheet widgets.
 */
public class MoneyHistoryDataModel extends ServiceBackedDataModel<MoneyHistory, HistoryListResult>
{
    public MoneyHistoryDataModel (int memberId, Currency currency)
    {
        _memberId = memberId;
        _currency = currency;
    }

    @Override
    protected void callFetchService (int start, int count, boolean needCount,
        AsyncCallback<HistoryListResult> callback)
    {
        _moneysvc.getTransactionHistory(_memberId, _currency, start, count, callback);
    }

    @Override
    protected int getCount (HistoryListResult result)
    {
        return result.totalCount;
    }

    @Override
    protected List<MoneyHistory> getRows (HistoryListResult result)
    {
        return result.history;
    }

    protected int _memberId;
    protected Currency _currency;

    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
