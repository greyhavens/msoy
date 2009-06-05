//
// $Id$

package client.billing;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.Page;

/**
 * The main entry point for the billing page.
 */
public class BillingPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals("subscribe")) {
            setContent(_msgs.subscribeTitle(), new SubscribePanel());
        } else {
            setContent(_msgs.selectTitle(), new SelectMethodPanel());
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.BILLING;
    }

    protected static final BillingMessages _msgs = GWT.create(BillingMessages.class);
}
