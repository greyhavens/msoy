//
// $Id$

package client.me;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;
import com.threerings.gwt.ui.SmartTable;

import com.threerings.msoy.money.data.all.MoneyTransaction;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.RowPanel;

public abstract class MoneyPanel extends PagedTable<MoneyTransaction>
{
    public MoneyPanel (MoneyTransactionDataModel model, Widget controller)
    {
        super(10, NAV_ON_TOP);

        _nav.add(MsoyUI.createLabel(_msgs.reportFilter(), "ReportFilter"));
        _nav.add(MsoyUI.createImage(model.report.icon, null), HasVerticalAlignment.ALIGN_MIDDLE);
        _nav.add(controller);

        addStyleName("Balance");
        setModel(model, 0);
    }

    @Override public SmartTable createContents (int start, int count, List<MoneyTransaction> list)
    {
        SmartTable table = super.createContents(start, count, list);
        table.setCellPadding(5);

        return table;
    }

    @Override
    public List<Widget> createRow (MoneyTransaction entry)
    {
        List<Widget> row = new ArrayList<Widget>();

        Label time = MsoyUI.createLabel(MsoyUI.formatDateTime(entry.timestamp), "Time");
        time.setWordWrap(false);
        row.add(time);

        String description = _dmsgs.xlate(MsoyUI.escapeHTML(entry.description));
        row.add(MsoyUI.createHTML(description, "Description"));

        addCustomRow(entry, row);

        return row;
    }

    @Override
    public List<Widget> createHeader ()
    {
        List<Widget> header = new ArrayList<Widget>();

        header.add(MsoyUI.createLabel(_msgs.reportColumnWhen(), null));
        header.add(MsoyUI.createLabel(_msgs.reportColumnHow(), null));

        addCustomHeader(header);

        return header;
    }

    @Override
    public String getEmptyMessage ()
    {
        return _msgs.transactionsNone();
    }

    @Override
    protected void addCustomControls (FlexTable table)
    {
        table.setWidget(0, 0, _nav = new RowPanel());
    }

    protected abstract void addCustomHeader (List<Widget> header);
    protected abstract void addCustomRow (MoneyTransaction entry, List<Widget> row);

    @Override
    protected boolean displayNavi (int items)
    {
        return true;
    }

    protected RowPanel _nav;

    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
}
