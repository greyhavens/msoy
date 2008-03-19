//
// $Id$

package client.support;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.threerings.underwire.gwt.client.AdminPanel;
import com.threerings.underwire.gwt.client.ClientMessages;
import com.threerings.underwire.gwt.client.ServerMessages;
import com.threerings.underwire.gwt.client.UserPanel;
import com.threerings.underwire.gwt.client.WebContext;
import com.threerings.underwire.web.client.UnderwireService;
import com.threerings.underwire.web.client.UnderwireServiceAsync;

import client.msgs.MsgsEntryPoint;
import client.shell.Args;
import client.shell.Page;

public class index extends MsgsEntryPoint
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new index();
            }
        };
    }

    // @Override // from Page
    public void onHistoryChanged (Args args)
    {
        String action = args.get(0, "");

        if (action.equals("admin")) {
            AdminPanel panel = new AdminPanel(_webctx);
            setContent(panel);
            panel.init();
        } else {
            UserPanel panel = new UserPanel(_webctx);
            setContent(panel);
            panel.init();
        }
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return SUPPORT;
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // create our web context
        _webctx = new WebContext();
        _webctx.undersvc = (UnderwireServiceAsync)GWT.create(UnderwireService.class);
        ((ServiceDefTarget)_webctx.undersvc).setServiceEntryPoint("/undersvc");
        _webctx.cmsgs = (ClientMessages)GWT.create(ClientMessages.class);
        _webctx.smsgs = (ServerMessages)GWT.create(ServerMessages.class);
    }

    protected WebContext _webctx;
}
