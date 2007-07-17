//
// $Id$

package client.swiftly;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Label;

import com.threerings.msoy.web.client.SwiftlyService;
import com.threerings.msoy.web.client.SwiftlyServiceAsync;

import com.threerings.msoy.web.data.ConnectConfig;

import client.shell.Page;
import client.util.MsoyUI;

/**
 * Displays a page that allows a player to launch swiftly for a given project.
 */
public class index extends Page
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

    // @Override from Page
    public void onHistoryChanged (String token)
    {
        updateInterface(token);
    }

    // @Override // from Page
    protected String getPageId ()
    {
        return "swiftly";
    }

    // @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CSwiftly.swiftlysvc = (SwiftlyServiceAsync)GWT.create(SwiftlyService.class);
        ((ServiceDefTarget)CSwiftly.swiftlysvc).setServiceEntryPoint("/swiftlysvc");

        // load up our translation dictionaries
        CSwiftly.msgs = (SwiftlyMessages)GWT.create(SwiftlyMessages.class);
    }

    protected void updateInterface (final String args)
    {
        // if we have no creds, just display a message saying login
        if (CSwiftly.ident == null) {
            setContent(MsoyUI.createLabel(CSwiftly.msgs.indexLogon(), "infoLabel"));
            return;
        } 

        setPageTitle(CSwiftly.msgs.projectsTitle());
        if (args.length() == 0) {
            // display the project create/list panel
            setContent(new ProjectSelectionPanel());

        } else {
            // load up the information needed to launch the applet
            CSwiftly.usersvc.getConnectConfig(new AsyncCallback() {
                public void onSuccess (Object result) {
                    try {
                        int projectId = Integer.parseInt(args);
                        setJavaContent(new SwiftlyPanel((ConnectConfig)result, projectId));
                        setContentStretchHeight(true);
                    } catch (NumberFormatException e) {
                        // display an error message if the supplied projectId did not parse
                        setContent(new Label(CSwiftly.msgs.invalidProjectId(args))); 
                    }
                }
                public void onFailure (Throwable cause) {
                    CSwiftly.serverError(cause);
                }
            });
        }
    }
}
