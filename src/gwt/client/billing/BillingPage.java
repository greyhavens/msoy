//
// $Id$

package client.billing;

import client.shell.Page;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

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
            setContent(new SubscribePanel());
        } else {
            setContent(new SelectMethodPanel());
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.BILLING;
    }
}
