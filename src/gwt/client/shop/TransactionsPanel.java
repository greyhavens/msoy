//
// $Id$

package client.shop;

import java.util.List;
import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.SmartTable;
import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.item.gwt.CatalogQuery;
import com.threerings.msoy.item.gwt.CatalogService;
import com.threerings.msoy.item.gwt.CatalogServiceAsync;
import com.threerings.msoy.item.gwt.ListingCard;
import com.threerings.msoy.item.gwt.ShopData;

import com.threerings.msoy.data.all.MediaDesc;

import client.ui.HeaderBox;
import client.ui.Marquee;
import client.ui.MsoyUI;
import client.ui.PriceLabel;
import client.ui.Stars;
import client.ui.ThumbBox;

import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import client.shell.Args;
import client.shell.Pages;

import client.item.ItemMessages;

public class TransactionsPanel extends VerticalPanel
{
    public TransactionsPanel (int memberId)
    {
        setStyleName("shopPanel");
        setVerticalAlignment(HasAlignment.ALIGN_TOP);

        _moneysvc.getTransactionHistory(memberId, 0, 10,
            new MsoyCallback<List<MoneyHistory>>() {
                public void onSuccess (List<MoneyHistory> history) {
                    init(history);
                }
            });
    }

    protected void init (List<MoneyHistory> history)
    {
        for (MoneyHistory h : history) {
            add(MsoyUI.createLabel(h.getTimestamp().toString(), ""));
            add(MsoyUI.createLabel(String.valueOf(h.getAmount()), ""));
        }
    }

    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
