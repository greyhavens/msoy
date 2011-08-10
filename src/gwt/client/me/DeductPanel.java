//
// $Id$

package client.me;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;

import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.gwt.MoneyService;
import com.threerings.msoy.money.gwt.MoneyServiceAsync;

import client.shell.CShell;
import client.shell.DynamicLookup;
import client.ui.MsoyUI;

public class DeductPanel extends HorizontalPanel
{
    public DeductPanel (final int memberId, final Currency currency)
    {
        final TextBox text = new TextBox();
        add(MsoyUI.createLabel(_msgs.deductTip(_dmsgs.xlate(currency.getKey())), null));
        add(text);
        add(new Button(_msgs.deductButton(), new ClickHandler() {
            public void onClick (ClickEvent event) {
                float value = Float.parseFloat(text.getText());
                deduct(memberId, currency, (int)(currency == Currency.BLING ?
                    (value * 100.0) : // convert to centibling
                    value));
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

    protected static final MoneyServiceAsync _moneysvc = GWT.create(MoneyService.class);
}
