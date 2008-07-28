//
// $Id$

package client.swiftly;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.ServiceDefTarget;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.swiftly.gwt.SwiftlyConnectConfig;
import com.threerings.msoy.swiftly.gwt.SwiftlyService;
import com.threerings.msoy.swiftly.gwt.SwiftlyServiceAsync;

import client.shell.Args;
import client.shell.Page;
import client.ui.MsoyUI;
import client.util.MsoyCallback;

/**
 * Displays a page that allows a player to launch swiftly for a given project.
 */
public class SwiftlyPage extends Page
{
    /** Required to map this entry point to a page. */
    public static Creator getCreator ()
    {
        return new Creator() {
            public Page createPage () {
                return new SwiftlyPage();
            }
        };
    }

    @Override // from Page
    public void onHistoryChanged (Args args)
    {
        updateInterface(args);
    }

    @Override
    public String getPageId ()
    {
        return SWIFTLY;
    }

    @Override // from Page
    protected String getTabPageId ()
    {
        return CREATE;
    }

    @Override // from Page
    protected void initContext ()
    {
        super.initContext();

        // wire up our remote services
        CSwiftly.swiftlysvc = (SwiftlyServiceAsync)GWT.create(SwiftlyService.class);
        ((ServiceDefTarget)CSwiftly.swiftlysvc).setServiceEntryPoint("/swiftlysvc");

        // load up our translation dictionaries
        CSwiftly.msgs = (SwiftlyMessages)GWT.create(SwiftlyMessages.class);
    }

    protected void updateInterface (final Args args)
    {
        // if we have no creds, just display a message saying login
        if (CSwiftly.ident == null) {
            setContent(MsoyUI.createLabel(CSwiftly.msgs.indexLogon(), "infoLabel"));
            return;
        }

        // XXX TEMP while swiftly is broken on whirled1/2 display a message only
        if (!DeploymentConfig.devDeployment) {
            setContent(MsoyUI.createLabel("Ouch! Swiftly is experiencing growing pains as we " +
                "continue our efforts to scale Whirled. Please bear with us while we finish our " +
                "work.", "infoLabel"));
            return;
        }

        final int projectId = args.get(0, 0);
        if (projectId == 0) {
            // display the project create/list panel
            setContent(CSwiftly.msgs.projectsTitle(), new ProjectSelectionPanel());

        } else {
            // load up the information needed to launch the applet
            CSwiftly.swiftlysvc.getConnectConfig(
                CSwiftly.ident, projectId, new MsoyCallback<SwiftlyConnectConfig>() {
                public void onSuccess (SwiftlyConnectConfig config) {
                    setContent(config.project.projectName, new SwiftlyPanel(config, projectId));
                }
            });
        }
    }
}
