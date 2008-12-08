//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;
import client.ui.NumberTextBox;
import client.util.ServiceUtil;

public class DeductPanel extends HorizontalPanel
{
    public DeductPanel (final int memberId, final Currency currency)
    {
        final NumberTextBox text = new NumberTextBox(currency == Currency.BLING);
        add(MsoyUI.createLabel(_msgs.deductTip(_dmsgs.xlate(currency.getKey())), null));
        add(text);
        add(new Button(_msgs.deductButton(), new ClickListener() {
            public void onClick (Widget sender) {
                // We don't want to allow deducting a *negative* amount, now do we?
                if (text.getValue().floatValue() <= 0.0f) {
                    MsoyUI.error(_msgs.deductMustBePositive());
                    return;
                }
                deduct(memberId, currency, (currency == Currency.BLING ?
                    (int)(text.getValue().floatValue() * 100.0) : // convert to centibling
                    text.getValue().intValue()));
            }
        }));
    }

    protected void deduct (int memberId, final Currency currency, final int amount)
    {
        _moneysvc.supportAdjust(memberId, currency, -amount, new AsyncCallback<Void>() {
            public void onFailure (Throwable t) {
                MsoyUI.error(CShell.serverError(t));
            }
            public void onSuccess (Void v) {
                MsoyUI.info(_msgs.deductSuccess(currency.format(amount), 
                    _dmsgs.xlate(currency.getKey())));
            }
        });
    }

    protected static final MeMessages _msgs = GWT.create(MeMessages.class);
    protected static final DynamicLookup _dmsgs = GWT.create(DynamicLookup.class);

    protected static final MoneyServiceAsync _moneysvc = (MoneyServiceAsync)
        ServiceUtil.bind(GWT.create(MoneyService.class), MoneyService.ENTRY_POINT);
}
