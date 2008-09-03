//
// $Id$

package client.shop;

import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.util.SimpleDateFormat;

import com.threerings.gwt.ui.PagedGrid;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.MoneyType;

import client.ui.MsoyUI;

public class BalancePanel extends PagedGrid<MoneyHistory>
{
    public BalancePanel (int memberId, MoneyType type)
    {
        super(10, 1, PagedGrid.NAV_ON_TOP);

        addStyleName("Balance");

        setModel(new MoneyHistoryDataModel(memberId, type), 0);
    }

// TODO: This should work
//    @Override
//    public boolean padToFullPage ()
//    {
//        return true;
//    }

    @Override // from PagedGrid
    protected void displayResults (int start, int count, List<MoneyHistory> list)
    {
        super.displayResults(start, count, list);

        SmartTable footer = new SmartTable("Footer", 0, 0);
        footer.setWidth("100%");
        footer.setHTML(0, 0, "&nbsp;", 1, "BottomLeft");
        footer.setHTML(0, 1, "&nbsp;");
        footer.setHTML(0, 2, "&nbsp;", 1, "BottomRight");
        add(footer);
    }

    public Widget createWidget (MoneyHistory entry)
    {
        return new TransactionWidget(entry);
    }

    public String getEmptyMessage ()
    {
        return _msgs.transactionsNone();
    }

    protected static class TransactionWidget extends HorizontalPanel
    {
        public TransactionWidget (MoneyHistory entry)
        {
            addStyleName("Transaction");

            add(MsoyUI.createLabel(_format.format(entry.getTimestamp()), "Time"));
            add(MsoyUI.createLabel(entry.getDescription(), "Description"));

            add(MsoyUI.createLabel("42", "Debit"));
            add(MsoyUI.createLabel(String.valueOf(entry.getAmount()), "Credit"));
            //setText(0, entry.isSpent() ? 3 : 2, String.valueOf(entry.getAmount()), 1, "");*/
            /*setText(0, 0, _format.format(entry.getTimestamp()), 1, "Time");
            setText(0, 1, entry.getDescription(), 1, "Description");
            setText(0, 
            setText(0, entry.isSpent() ? 3 : 2, String.valueOf(entry.getAmount()), 1, "");*/
        }
    }

    // TODO: Unify with Mail date format?
    protected static SimpleDateFormat _format = new SimpleDateFormat("MM dd YY, h:mm a");

    protected static final ShopMessages _msgs = GWT.create(ShopMessages.class);
}
