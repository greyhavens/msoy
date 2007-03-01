//
// $Id$

package client.swiftly;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.client.SwiftlyService;
import com.threerings.msoy.web.client.SwiftlyServiceAsync;
import com.threerings.msoy.web.data.WebCreds;

import com.threerings.msoy.web.data.ConnectConfig;

import client.shell.MsoyEntryPoint;

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
        updateInterface(token);
    }

    // @Override // from MsoyEntryPoint
    protected String getPageId ()
    {
        return "swiftly";
    }

    // @Override // from MsoyEntryPoint
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CSwiftly.swiftlysvc = (SwiftlyServiceAsync)GWT.create(SwiftlyService.class);
        ((ServiceDefTarget)CSwiftly.swiftlysvc).setServiceEntryPoint("/swiftlysvc");

        // load up our translation dictionaries
        CSwiftly.msgs = (SwiftlyMessages)GWT.create(SwiftlyMessages.class);
    }

    // @Override from MsoyEntryPoint
    protected void onPageLoad ()
    {
        History.addHistoryListener(this);
    }

    // @Override from MsoyEntryPoint
    protected boolean didLogon (WebCreds creds)
    {
        boolean header = super.didLogon(creds);
        updateInterface(History.getToken());
        return false; // disable swiftly header client for now
    }

    // @Override from MsoyEntryPoint
    protected void didLogoff ()
    {
        super.didLogoff();
        updateInterface(History.getToken());
    }

    protected void updateInterface (final String historyToken)
    {
        // if we have no creds, just display a message saying login
        if (CSwiftly.creds == null) {
            setContent(new Label(CSwiftly.msgs.indexLogon()));
            return;
        } 

        if (historyToken.length() == 0) {
            // display the project create/list panel
            setContent(new ProjectSelectionPanel());
        } else {
            // load up the information needed to launch the applet
            CSwiftly.swiftlysvc.loadConnectConfig(CSwiftly.creds, new AsyncCallback() {
                public void onSuccess (Object result) {
                    try {
                        setContent(new SwiftlyPanel((ConnectConfig)result,
                            Integer.parseInt(historyToken)));
                    } catch (NumberFormatException e) {
                        // display an error message if the supplied projectId did not parse
                        setContent(new Label(CSwiftly.msgs.invalidProjectId(historyToken))); 
                    }
                }
                public void onFailure (Throwable cause) {
                    CSwiftly.serverError(cause);
                }
            });
        }
    }
}
