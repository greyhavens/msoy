//
// $Id$

package client.shop;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.MoneyType;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

/**
 * Data model for service backed balance sheet widgets.
 */
public class MoneyHistoryDataModel extends ServiceBackedDataModel<MoneyHistory, List<MoneyHistory>>
{
    public MoneyHistoryDataModel (int memberId)
    {
        _memberId = memberId;
    }

    @Override
    protected void callFetchService (int start, int count, boolean needCount,
        AsyncCallback<List<MoneyHistory>> callback)
    {
        _moneysvc.getTransactionHistory(_memberId, start, count, callback);
    }

    @Override
    protected int getCount (List<MoneyHistory> result)
    {
        // TODO: Use a MoneyHistoryResult that contains a count of the total
        // transactions living on the server
        return 50;
    }

    @Override
    protected List<MoneyHistory> getRows (List<MoneyHistory> result)
    {
        return result;
    }

    protected int _memberId;

    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
