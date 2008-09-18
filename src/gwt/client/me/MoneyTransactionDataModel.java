//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.PagedResult;

import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.util.PagedServiceDataModel;
import client.util.ServiceUtil;

/**
 * Data model for service backed balance sheet widgets.
 */
public class MoneyTransactionDataModel extends PagedServiceDataModel<MoneyTransaction>
{
    public MoneyTransactionDataModel (int memberId, ReportType report)
    {
        _memberId = memberId;
        _report = report;
    }

    @Override
    protected void callFetchService (
        int start, int count, boolean needCount,
        AsyncCallback<PagedResult<MoneyTransaction>> callback)
    {
        _moneysvc.getTransactionHistory(_memberId, _report, start, count, /*needCount,*/ callback);
    }

    protected int _memberId;
    protected ReportType _report;

    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
