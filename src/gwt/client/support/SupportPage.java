//
// $Id$

package client.support;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

import com.threerings.underwire.gwt.client.AdminPanel;
import com.threerings.underwire.gwt.client.ClientMessages;
import com.threerings.underwire.gwt.client.ServerMessages;
import com.threerings.underwire.gwt.client.WebContext;
import com.threerings.underwire.web.client.UnderwireService;
import com.threerings.underwire.web.client.UnderwireServiceAsync;
import com.threerings.underwire.web.data.AccountName;

import client.shell.Page;
import client.shell.ShellMessages;
import client.ui.MsoyUI;

public class SupportPage extends Page
{
    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        // create our auth credentials if we don't already have them
        if (_webctx.ainfo == null && CSupport.creds != null) {
            UnderwireService.AuthInfo ainfo = new UnderwireService.AuthInfo();
            ainfo.authtok = CSupport.creds.token;
            ainfo.name = new AccountName();
            ainfo.name.accountName = ""+CSupport.getMemberId();
            ainfo.name.gameName = CSupport.creds.name.toString();
            ainfo.email = CSupport.creds.accountName;
            ainfo.isAdmin = CSupport.isSupport();
            if (ainfo.isAdmin) {
                ainfo.gameURL = "/#adminz-info_";
                ainfo.billingURL = "/#me-transactions_2_";
            }
            _webctx.ainfo = ainfo;
        }

        String action = args.get(0, "");
        if (action.equals("admin")) {
            if (!CSupport.isSupport()) {
                setContent(CSupport.msgs.supportTitle(),
                           MsoyUI.createLabel(CSupport.msgs.lackPrivileges(), "infoLabel"));
                return;
            }
            AdminPanel panel = new AdminPanel(_webctx);
            setContent(CSupport.msgs.supportTitle(), panel);
            panel.init();
        } else {
            ContactPanel panel = new ContactPanel(_webctx);
            setContent(CSupport.msgs.contactTitle(), panel);
        }
    }

    @Override
    public Pages getPageId ()
    {
        return Pages.SUPPORT;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // create our Underwire context
        _webctx = new WebContext();
        _webctx.undersvc = (UnderwireServiceAsync)GWT.create(UnderwireService.class);
        ((ServiceDefTarget)_webctx.undersvc).setServiceEntryPoint("/undersvc");
        _webctx.cmsgs = (ClientMessages)GWT.create(ClientMessages.class);
        _webctx.smsgs = (ServerMessages)GWT.create(ServerMessages.class);

        // initialize our MSOY context
        CSupport.msgs = (SupportMessages)GWT.create(SupportMessages.class);
    }

    protected WebContext _webctx;

    protected static final ShellMessages _cmsgs = GWT.create(ShellMessages.class);
}
