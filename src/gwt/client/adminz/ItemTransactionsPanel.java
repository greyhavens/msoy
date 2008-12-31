//
// $Id$

package client.adminz;

import java.util.ArrayList;
import java.util.List;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.Link;
import client.util.ServiceBackedDataModel;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.PagedTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.admin.gwt.AdminService.ItemTransactionResult;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.money.data.all.MoneyTransaction;

/**
 * Panel to list the transactions involving the sale of a given item.
 */
public class ItemTransactionsPanel extends VerticalPanel
{
    /**
     * Creates a new item transaction panel.
     */
    public ItemTransactionsPanel (ItemDetail detail)
    {
        _detail = detail;
        setStyleName("itemTransactions");
        setSpacing(10);

        add(MsoyUI.createLabel(_msgs.reviewItemTransactionsHeader(detail.item.name), null));

        PagedTable<MoneyTransaction> table = new PagedTable<MoneyTransaction>(20) {
            @Override protected List<Widget> createRow (MoneyTransaction item) {
                return getRow(item);
            }

            @Override protected List<Widget> createHeader () {
                List<Widget> header = new ArrayList<Widget>();

                header.add(MsoyUI.createLabel(_msgs.reviewItemColumnWhen(), null));
                header.add(MsoyUI.createLabel(_msgs.reviewItemColumnWho(), null));
                header.add(MsoyUI.createLabel(_msgs.reviewItemColumnHow(), null));
                header.add(MsoyUI.createLabel(_msgs.reviewItemColumnIncome(), null));
                header.add(MsoyUI.createLabel(_msgs.reviewItemColumnBuyer(), null));
                return header;
            }

            @Override protected String getEmptyMessage () {
                return _msgs.reviewItemNoTransactions();
            }
        };
        table.setModel(new Model(), 0);
        table.addStyleName("Table");
        add(table);
    }

    protected List<Widget> getRow (MoneyTransaction entry)
    {
        List<Widget> row = new ArrayList<Widget>();

        Label time = MsoyUI.createLabel(MsoyUI.formatDateTime(entry.timestamp), "Time");
        time.setWordWrap(false);
        row.add(time);

        MemberName memberName = _result != null ? _result.memberNames.get(entry.memberId) : null;
        String memberNameString = "" + (memberName != null ? memberName : entry.memberId);
        row.add(Link.memberView(memberNameString, entry.memberId));
        
        String description = _dmsgs.xlate(MsoyUI.escapeHTML(entry.description));
        row.add(MsoyUI.createHTML(description, "Description"));

        // Pack the currency icon and amount into one column
        HorizontalPanel income = new HorizontalPanel();
        income.add(MsoyUI.createInlineImage(entry.currency.getSmallIcon()));
        income.add(WidgetUtil.makeShim(15, 1));
        income.add(MsoyUI.createLabel(entry.currency.format(entry.amount), "Income"));

        row.add(income);

        if (entry.referenceMemberName != null) {
            row.add(Link.memberView(entry.referenceMemberName.toString(),
                entry.referenceMemberName.getMemberId()));
        } else {
            row.add(MsoyUI.createLabel("", null));
        }

        return row;
    }

    protected class Model
        extends ServiceBackedDataModel<MoneyTransaction, AdminService.ItemTransactionResult>
    {
        @Override // from ServiceBackedDataModel
        protected void callFetchService (int start, int count, boolean needCount,
            AsyncCallback<ItemTransactionResult> callback)
        {
            _adminsvc.getItemTransactions(new ItemIdent(
                _detail.item.getType(), _detail.item.itemId), start, count, needCount, callback);
        }

        @Override // from ServiceBackedDataModel
        protected void onSuccess (
            ItemTransactionResult result, AsyncCallback<List<MoneyTransaction>> callback)
        {
            _result = result;
            super.onSuccess(result, callback);
        }

        @Override // from ServiceBackedDataModel
        protected int getCount (ItemTransactionResult result)
        {
            return result.total;
        }

        @Override // from ServiceBackedDataModel
        protected List<MoneyTransaction> getRows (ItemTransactionResult result)
        {
            return result.page;
        }
    }

    protected ItemDetail _detail;
    protected ItemTransactionResult _result;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = (AdminServiceAsync)
        ServiceUtil.bind(GWT.create(AdminService.class), AdminService.ENTRY_POINT);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
