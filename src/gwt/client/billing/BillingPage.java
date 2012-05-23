//
// $Id$

package client.billing;

import com.google.gwt.core.client.GWT;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import client.shell.Page;
import client.util.BillingUtil;

/**
 * The main entry point for the billing page.
 */
public class BillingPage extends Page
{
    public static final String ABOUT_BARS = "aboutbars";
    public static final String ADMIN = "admin";
    public static final String IFRAME = "iframe";

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");
        if (action.equals(ABOUT_BARS)) {
            setContent(_msgs.aboutBarsTitle(), new AboutBarsPanel());
        } else if (action.equals(ADMIN)) {
            setContent(_msgs.billingIframeTitle(), new BillingIframePanel("admin/"));
        } else if (action.equals(IFRAME)) {
            setContent(_msgs.billingIframeTitle(), new BillingIframePanel(""));

        // default guests to ABOUT_BARS and registered billing-ready users to IFRAME
        } else {
            if (BillingUtil.isBillingReady()) {
                setContent(_msgs.billingIframeTitle(), new BillingIframePanel(""));
            } else {
                setContent(_msgs.aboutBarsTitle(), new AboutBarsPanel());
            }
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.BILLING;
    }

    protected static final BillingMessages _msgs = GWT.create(BillingMessages.class);
}
