//
// $Id$

package client.swiftly;

import client.shell.Page;
import client.util.MsoyUI;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.rpc.ServiceDefTarget;
import com.google.gwt.user.client.ui.Label;
import com.threerings.msoy.web.client.SwiftlyService;
import com.threerings.msoy.web.client.SwiftlyServiceAsync;
import com.threerings.msoy.web.data.ConnectConfig;

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

        // XXX TEMP while swiftly is broken on whirled1/2 display a message only
        if (true) {
            setContent(MsoyUI.createLabel("Ouch! Swiftly is experiencing growing pains as we " +
                "continue our efforts to scale Whirled. Please bear with us while we finish our " +
                "work.", "infoLabel"));
            return;
        }

        setPageTitle(CSwiftly.msgs.projectsTitle());
        if (args.length() == 0) {
            // display the project create/list panel
            setContent(new ProjectSelectionPanel());

        } else {
            final int projectId;
            try {
                projectId = Integer.parseInt(args);

            } catch (NumberFormatException e) {
                // display an error message if the supplied projectId did not parse
                setContent(new Label(CSwiftly.msgs.invalidProjectId(args)));
                return;
            }

            // load up the information needed to launch the applet
            CSwiftly.swiftlysvc.getConnectConfig(CSwiftly.ident, projectId, new AsyncCallback() {
                public void onSuccess (Object result) {
                    setJavaContent(new SwiftlyPanel((ConnectConfig)result, projectId));
                    setContentStretchHeight(true);
                }
                public void onFailure (Throwable cause) {
                    CSwiftly.serverError(cause);
                }
            });
        }
    }
}
