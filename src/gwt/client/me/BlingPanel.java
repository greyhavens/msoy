//
// $Id$

package client.me;

import client.shell.ShellMessages;
import client.util.MsoyCallback;
import client.util.ServiceUtil;

import com.google.gwt.core.client.GWT;
import com.google.gwt.i18n.client.NumberFormat;
import com.threerings.gwt.ui.SmartTable;
import com.threerings.msoy.money.data.all.BlingInfo;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

public class BlingPanel extends SmartTable
{
    public BlingPanel (int memberId)
    {
        setCellSpacing(10);
        setStyleName("bling");
        
        _moneysvc.getBlingInfo(memberId, new MsoyCallback<BlingInfo>() {
            public void onSuccess (BlingInfo result) {
                init(result);
            }
        });
    }
    
    protected void init (BlingInfo result)
    {
        int row = 0;
        setText(row++, 0, _msgs.blingHeader(), 4, "header");
        setText(row, 0, _msgs.blingBalance(), 1, "rightLabel");
        setText(row++, 1, Currency.BLING.format(result.bling));
        setText(row, 0, _msgs.blingWorth(), 1, "rightLabel");
        setText(row, 1, formatUSD(result.blingWorth));
    }
    
    /**
     * Converts the amount of pennies into a string to display to the user as a valid currency.
     * Note: there are some other utilities around to do this, but they're either in a different
     * project (and there's some concern about exposing them directly), or they don't properly
     * take into accound floating-point round off errors.  This may get replaced or expanded
     * later on.
     */
    protected static String formatUSD (int pennies)
    {
        int dollars = pennies / 100;
        int cents = pennies % 100;
        return "USD $" + NumberFormat.getDecimalFormat().format(dollars) + '.' +
            (cents < 10 ? '0' : "") + cents;
    }

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
