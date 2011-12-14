//
// $Id$

package client.adminz;

import java.util.List;

import com.google.common.collect.Lists;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedTable;
import com.threerings.gwt.ui.WidgetUtil;
import com.threerings.gwt.util.DataModel;
import com.threerings.gwt.util.DateUtil;

import com.threerings.msoy.admin.gwt.AdminService.ItemTransactionResult;
import com.threerings.msoy.admin.gwt.AdminService;
import com.threerings.msoy.admin.gwt.AdminServiceAsync;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.ItemIdent;
import com.threerings.msoy.item.gwt.ItemDetail;
import com.threerings.msoy.money.data.all.MoneyTransaction;

import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.util.InfoCallback;
import client.util.Link;

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
                List<Widget> header = Lists.newArrayList();

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
        List<Widget> row = Lists.newArrayList();

        Label time = MsoyUI.createLabel(DateUtil.formatDateTime(entry.timestamp), "Time");
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
            row.add(Link.memberView(entry.referenceMemberName));
        } else {
            row.add(MsoyUI.createLabel("", null));
        }

        return row;
    }

    protected class Model implements DataModel<MoneyTransaction>
    {
        @Override
        public void doFetchRows (
            int start, int count, final AsyncCallback<List<MoneyTransaction>> callback)
        {
            ItemIdent ident = new ItemIdent(_detail.item.getType(), _detail.item.itemId);
            _adminsvc.getItemTransactions(ident, start, count, new InfoCallback<ItemTransactionResult>() {
                public void onSuccess (ItemTransactionResult result) {
                    _result = result;
                    callback.onSuccess(result.page);
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
    }

    protected ItemDetail _detail;
    protected AdminService.ItemTransactionResult _result;

    protected static final AdminMessages _msgs = GWT.create(AdminMessages.class);
    protected static final AdminServiceAsync _adminsvc = GWT.create(AdminService.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);
}
