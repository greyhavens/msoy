//
// $Id$

package client.me;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.ListenerList;

import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.MoneyTransaction;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.money.data.all.TransactionPageResult;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.util.InfoCallback;

/**
 * Data model for the service backed balance sheet widget on the Transactions page.  This will load
 * all of the data necessary to view the page.  Clients of this class can pass in callbacks to
 * receive other information as it's loaded, but this is still driven by the display of the
 * balance sheet itself.
 */
public class MoneyTransactionDataModel implements DataModel<MoneyTransaction>
{
    public int memberId;
    public ReportType report;

    public MoneyTransactionDataModel (int memberId, ReportType report)
    {
        this.memberId = memberId;
        this.report = report;
    }

    /**
     * Sets the callback that should be called when the bling information is retrieved by the
     * server. If the information is immediately available, this will call the callback immediately.
     */
    public void addBlingCallback (AsyncCallback<BlingInfo> callback)
    {
        _callbackList = ListenerList.addListener(_callbackList, callback);
        if (_blingInfo != null) {
            callback.onSuccess(_blingInfo);
        }
    }

    @Override
    public void doFetchRows (
        int start, int count, final AsyncCallback<List<MoneyTransaction>> callback)
    {
        _moneysvc.getTransactionHistory(
            memberId, report, start, count, new InfoCallback<TransactionPageResult>() {
                public void onSuccess (TransactionPageResult result) {
                    callback.onSuccess(result.page);

                    _blingInfo = result.blingInfo;
                    if (_callbackList != null) {
                        _callbackList.notify(new ListenerList.Op<AsyncCallback<BlingInfo>>() {
                            public void notify (AsyncCallback<BlingInfo> listener) {
                                listener.onSuccess(_blingInfo);
                            }
                        });
                    }
                }
            });
    }

    @Override
    public int getItemCount ()
    {
        return -1;
    }

    @Override
    public void removeItem (MoneyTransaction item)
    {
    }

    protected ListenerList<AsyncCallback<BlingInfo>> _callbackList;
    protected BlingInfo _blingInfo;

    protected static final MoneyServiceAsync _moneysvc = GWT.create(MoneyService.class);
}
