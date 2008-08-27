//
// $Id$

package client.shop;

import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.PagedGrid;

import com.threerings.msoy.money.data.all.MoneyHistory;
import com.threerings.msoy.money.data.all.MoneyType;

import client.ui.MsoyUI;

public class BalancePanel extends PagedGrid<MoneyHistory>
{
    public BalancePanel (int memberId)
    {
        super(10, 1, PagedGrid.NAV_ON_BOTTOM);

        setModel(new MoneyHistoryDataModel(memberId), 0);
    }

    public Widget createWidget (MoneyHistory item)
    {
        return MsoyUI.createLabel(item.getDescription() + ": " + item.getAmount(), "");
    }

    public String getEmptyMessage ()
    {
        return "TODO";
    }
}
