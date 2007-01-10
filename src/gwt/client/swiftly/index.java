//
// $Id$

package client.swiftly;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.client.SwiftlyService;
import com.threerings.msoy.web.client.SwiftlyServiceAsync;
import com.threerings.msoy.web.data.WebCreds;

import client.shell.MsoyEntryPoint;
import client.shell.ShellContext;

/**
 * Displays a page that allows a player to launch swiftly for a given project.
 */
public class index extends MsoyEntryPoint
    implements HistoryListener
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public MsoyEntryPoint createEntryPoint () {
                return new index();
            }
        };
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        updateInterface(History.getToken());
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "swiftly";
    }

    // @Override // from MsoyEntryPoint
    protected ShellContext createContext ()
    {
        return _ctx = new SwiftlyContext();
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        _ctx.swiftlysvc = (SwiftlyServiceAsync)GWT.create(SwiftlyService.class);
        ((ServiceDefTarget)_ctx.swiftlysvc).setServiceEntryPoint("/swiftlysvc");

        // load up our translation dictionaries
        _ctx.msgs = (SwiftlyMessages)GWT.create(SwiftlyMessages.class);
    }

    // @Override from MsoyEntryPoint
    protected void didLogon (WebCreds creds)
    {
        super.didLogon(creds);
        updateInterface(null);
    }

    // @Override from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        updateInterface(null);
    }

    // @Override from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
        updateInterface(History.getToken());
    }

    protected void updateInterface (String historyToken)
    {
        if (_ctx.creds == null) {
            // if we have no creds, just display a message saying login
            setContent(new Label(_ctx.msgs.indexLogon()));
        } else {
            setContent(new SwiftlyPanel(_ctx));
        }
    }

    protected SwiftlyContext _ctx;
}
