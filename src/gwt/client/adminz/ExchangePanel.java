//
// $Id$

package client.adminz;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;

import com.threerings.msoy.money.data.all.ExchangeData;
import com.threerings.msoy.money.data.all.ExchangeStatusData;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.ui.MsoyUI;

import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

public class ExchangePanel extends VerticalPanel
{
    public ExchangePanel ()
    {
        HorizontalPanel hpan = new HorizontalPanel();
        hpan.add(MsoyUI.createLabel("current Rate: ", null));
        hpan.add(_rate = new Label());
        add(hpan);

        hpan = new HorizontalPanel();
        hpan.add(MsoyUI.createLabel("bar pool balance: ", null));
        hpan.add(_barPool = new Label());
        add(hpan);

        add(new RecentExchanges(new ExchangeDataDataModel()));
    }

    protected class ExchangeDataDataModel
        extends ServiceBackedDataModel<ExchangeData, ExchangeStatusData>
    {
        @Override
        protected void callFetchService (
            int start, int count, boolean needCount, AsyncCallback<ExchangeStatusData> callback)
        {
            _moneysvc.getExchangeStatus(start, count, callback);
        }

        @Override
        protected int getCount (ExchangeStatusData result)
        {
            return result.total;
        }

        @Override
        protected List<ExchangeData> getRows (ExchangeStatusData result)
        {
            return result.page;
        }

        @Override
        protected void onSuccess (
            ExchangeStatusData result, AsyncCallback<List<ExchangeData>> callback)
        {
            super.onSuccess(result, callback);

            _rate.setText(String.valueOf(result.rate));
            _barPool.setText(String.valueOf(result.barPool));
        }
    }

    protected static class RecentExchanges extends PagedTable<ExchangeData>
    {
        public RecentExchanges (ExchangeDataDataModel model)
        {
            super(20);
            setModel(model, 0);
        }

        @Override
        public List<Widget> createRow (ExchangeData data)
        {
            List<Widget> row = new ArrayList<Widget>();

            Label time = MsoyUI.createLabel(MsoyUI.formatDateTime(data.timestamp), "Time");
            time.setWordWrap(false);
            row.add(time);

            row.add(MsoyUI.createLabel(String.valueOf(data.bars), null));
            row.add(MsoyUI.createLabel(String.valueOf(data.coins), null));
            row.add(MsoyUI.createLabel(String.valueOf(data.rate), null));
            // TODO: reference tx

            return row;
        }

        @Override
        public List<Widget> createHeader ()
        {
            List<Widget> header = new ArrayList<Widget>();

            header.add(MsoyUI.createLabel("When", null));
            header.add(MsoyUI.createLabel("Bars", null));
            header.add(MsoyUI.createLabel("Coins", null));
            header.add(MsoyUI.createLabel("Rate", null));
            // TODO: reference tx

            return header;
        }

        @Override
        public String getEmptyMessage ()
        {
            return ""; // not gonna happen
        }
    }

    protected Label _rate;
    protected Label _barPool;

    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
