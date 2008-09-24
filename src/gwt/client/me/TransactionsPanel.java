//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.money.data.all.ReportType;

import client.shell.Args;
import client.shell.Pages;
import client.ui.MsoyUI;
import client.util.Link;

public class TransactionsPanel extends VerticalPanel
{
    public TransactionsPanel (int reportIndex, final int memberId)
    {
        setStyleName("transactions");

        ReportType report = ReportType.fromIndex(reportIndex);
        final Widget icon = MsoyUI.createImage(report.icon, null);
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
        
        // The data model is used in both the balance panel and the bling panel.
        MoneyTransactionDataModel model = new MoneyTransactionDataModel(memberId, report);

        add(Link.buyBars("Buy some bars!")); // TODO: i18n
        add(new BalancePanel(model) {
            @Override protected void addCustomControls (FlexTable controls) {
                controls.setText(0, 0, _msgs.reportFilter());
                controls.getFlexCellFormatter().setStyleName(0, 0, "ReportFilter");
                controls.setWidget(0, 1, icon);
                controls.setWidget(0, 2, reportBox);
            }
        });

        if (report == ReportType.BLING) {
            add(new BlingPanel(model));
        }
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);

    protected static final String[] REPORT_NAMES = {
        _msgs.reportCoins(), _msgs.reportBars(), _msgs.reportBling(), _msgs.reportCreator()
    };
}
