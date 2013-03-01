//
// $Id$

package client.me;

import java.util.Arrays;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.ReportType;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.CShell;
import client.ui.MsoyUI;
import client.ui.RoundBox;
import client.ui.TongueBox;
import client.util.Link;

public class TransactionsPanel extends FlowPanel
{
    public TransactionsPanel (int reportIndex, final int memberId)
    {
        setStyleName("transactions");

        ReportType report = ReportType.fromIndex(reportIndex);
        int comboIndex = Arrays.asList(REPORT_TYPES).indexOf(report);

        FlexTable blurb = new FlexTable();
        blurb.setStyleName("Blurb");

        RoundBox tip = new RoundBox(RoundBox.MEDIUM_BLUE);
        tip.add(MsoyUI.createHTML(comboIndex < 0 ? ":(" : REPORT_TIPS[comboIndex], null));
        blurb.setWidget(0, 0, tip);

        FlowPanel billing = MsoyUI.createFlowPanel("BillingTip");
        billing.add(MsoyUI.createHTML(_msgs.billingTip(), null));
        billing.add(MsoyUI.createButton(MsoyUI.LONG_THIN, _msgs.buyBars(),
                                        Link.createHandler(Pages.BILLING)));
        blurb.setWidget(0, 1, billing);
        blurb.getCellFormatter().setStyleName(0, 1, "Billing");

        // we use titleless tongue boxes here to make the indentation work
        add(new TongueBox(null, blurb));

        // The data model is used in both the balance panel and the bling panel.
        MoneyTransactionDataModel model = new MoneyTransactionDataModel(memberId, report);

        final ListBox reportBox = new ListBox();
        for (String name : REPORT_NAMES) {
            reportBox.addItem(name);
        }
        reportBox.addChangeHandler(new ChangeHandler() {
            public void onChange (ChangeEvent event) {
                ReportType type = REPORT_TYPES[reportBox.getSelectedIndex()];
                Link.go(Pages.ME, MePage.TRANSACTIONS, type.toIndex(), memberId);
            }
        });
        if (comboIndex >= 0) {
            reportBox.setSelectedIndex(comboIndex);
        }

        // figure out which display mode to use
        Widget panel;
        if (report == ReportType.CREATOR_BARS || report == ReportType.CREATOR_COINS) {
            panel = new IncomePanel(model, reportBox);
        } else {
            panel = new BalancePanel(model, reportBox);
        }

        // we use titleless tongue boxes here to make the indentation work
        add(new TongueBox(null, panel));

        // extra bits
        if (report == ReportType.BLING) {
            add(new BlingPanel(model)); // does its own tongue-boxing
        }
        if (CShell.isSupport() && report == ReportType.COINS) {
            add(new TongueBox("Deduct coins", new DeductPanel(memberId, Currency.COINS)));
        } else if (CShell.isAdmin() && report == ReportType.BARS) {
            add(new TongueBox("Deduct bars", new DeductPanel(memberId, Currency.BARS)));
        } else if (CShell.isAdmin() && report == ReportType.BLING) {
            add(new TongueBox("Deduct bling", new DeductPanel(memberId, Currency.BLING)));
        }
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);

    protected static final String[] REPORT_NAMES = {
        _msgs.reportCoins(), _msgs.reportCreatorCoins(),
    };
    protected static final String[] REPORT_TIPS = {
        _msgs.tipCoins(), _msgs.tipCreatorCoins(),
    };
    protected static final ReportType[] REPORT_TYPES = {
        ReportType.COINS, ReportType.CREATOR_COINS,
    };
}
