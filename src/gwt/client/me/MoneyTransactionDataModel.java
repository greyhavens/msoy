//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.data.all.TransactionPageResult;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

/**
 * Data model for the service backed balance sheet widget on the Transactions page.  This will load
 * all of the data necessary to view the page.  Clients of this class can pass in callbacks to
 * receive other information as it's loaded, but this is still driven by the display of the
 * balance sheet itself.
 */
public class MoneyTransactionDataModel extends ServiceBackedDataModel<MoneyTransaction, TransactionPageResult>
{
    public /* final */ int memberId;
    public /* final */ ReportType report;
    
    public MoneyTransactionDataModel (int memberId, ReportType report)
    {
        this.memberId = memberId;
        this.report = report;
    }
    
    /**
     * Sets the callback that should be called when the bling information is retrieved by the server.
     * If the information is immediately available, this will call the callback immediately.
     * @param callback
     */
    public void setBlingCallback (AsyncCallback<BlingInfo> callback) 
    {
        _blingCallback = callback;
        if (_blingInfo != null) {
            callback.onSuccess(_blingInfo);
        }
    }

    @Override
    protected void callFetchService (
        int start, int count, boolean needCount,
        AsyncCallback<TransactionPageResult> callback)
    {
        _moneysvc.getTransactionHistory(memberId, report, start, count, callback);
    }

    @Override
    protected int getCount (TransactionPageResult result)
    {
        return result.total;
    }

    @Override
    protected List<MoneyTransaction> getRows (TransactionPageResult result)
    {
        return result.page;
    }
    
    @Override
    protected void onSuccess (TransactionPageResult result, AsyncCallback<List<MoneyTransaction>> callback)
    {
        super.onSuccess(result, callback);
        _blingInfo = result.blingInfo;
        if (_blingCallback != null) {
            _blingCallback.onSuccess(_blingInfo);
        }
    }
    
    protected AsyncCallback<BlingInfo> _blingCallback;
    protected BlingInfo _blingInfo;
    
    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
