//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.HistoryListResult;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

/**
 * Data model for service backed balance sheet widgets.
 */
public class MoneyTransactionDataModel
    extends ServiceBackedDataModel<MoneyTransaction, HistoryListResult>
{
    public MoneyTransactionDataModel (int memberId, ReportType report)
    {
        _memberId = memberId;
        _report = report;
    }

    @Override
    protected void callFetchService (int start, int count, boolean needCount,
        AsyncCallback<HistoryListResult> callback)
    {
        _moneysvc.getTransactionHistory(_memberId, _report, start, count, callback);
    }

    @Override
    protected int getCount (HistoryListResult result)
    {
        return result.totalCount;
    }

    @Override
    protected List<MoneyTransaction> getRows (HistoryListResult result)
    {
        return result.transactions;
    }

    protected int _memberId;
    protected ReportType _report;

    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
