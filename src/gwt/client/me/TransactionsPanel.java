//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.money.data.all.ReportType;

import client.shell.Args;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.ui.StretchButton;
import client.util.Link;

public class TransactionsPanel extends VerticalPanel
{
    public TransactionsPanel (int reportIndex, final int memberId)
    {
        setStyleName("transactions");

        HorizontalPanel horiz = new HorizontalPanel();

        RoundBox tip = new RoundBox(RoundBox.MEDIUM_BLUE);
        tip.add(MsoyUI.createLabel(_msgs.moneyTip(), null));
        horiz.add(tip);

        StretchButton button = new StretchButton(StretchButton.ORANGE_THICK,
            MsoyUI.createLabel(_msgs.buyBars(), null));
        horiz.add(button);

        add(horiz);

        ReportType report = ReportType.fromIndex(reportIndex);
        // The data model is used in both the balance panel and the bling panel.
        MoneyTransactionDataModel model = new MoneyTransactionDataModel(memberId, report);

        final ListBox reportBox = new ListBox();
        for (String name : REPORT_NAMES) {
            reportBox.addItem(name);
        }
        reportBox.addChangeListener(new ChangeListener() {
            public void onChange (Widget widget) {
                Link.go(Pages.ME, Args.compose("transactions",
                        String.valueOf(reportBox.getSelectedIndex()+1),
                        String.valueOf(memberId)));
            }
        });
        reportBox.setSelectedIndex(reportIndex-1);

        add((report == ReportType.CREATOR) ?
            new IncomePanel(model, reportBox) : new BalancePanel(model, reportBox));

        // Extra bits
        if (report == ReportType.BLING) {
            add(new BlingPanel(model));
        } else if (CMe.isSupport() && report == ReportType.COINS) {
            // TODO
        }
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);

    protected static final String[] REPORT_NAMES = {
        _msgs.reportCoins(), _msgs.reportBars(), _msgs.reportBling(), _msgs.reportCreator()
    };
}
