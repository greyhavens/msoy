//
// $Id$

package client.swiftly;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.SwiftlyProject;

import client.shell.Application;
import client.shell.WorldClient;
import client.util.InfoPopup;

/**
 * Displays the client interface for a particular swiftly project.
 */
public class SwiftlyPanel extends FlexTable
    implements ProjectEdit.ProjectEditListener
{
    public SwiftlyPanel (ConnectConfig config, int projectId)
    {
        super();
        setStyleName("swiftlyPanel");
        _authtoken = (CSwiftly.creds == null) ? "" : CSwiftly.creds.token;
        _config = config;

        // load up the swiftly project record
        CSwiftly.swiftlysvc.loadProject(CSwiftly.creds, projectId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _project = (SwiftlyProject)result;
                updateProjectLink();
                loadApplet();
            }
            public void onFailure (Throwable cause) {
                new InfoPopup(CSwiftly.serverError(cause)).show();
            }
        });
    }

    // from ProjectEdit.ProjectEditListener
    public void projectSubmitted (SwiftlyProject project)
    {
        _project = project;
        updateProjectLink();
    }

    protected void loadApplet ()
    {
        // Add project information to the header
        setText(0, 0, CSwiftly.msgs.swiftlyEditing());
        setWidget(0, 1, _projectLink);
        setWidget(0, 2, new Button(CSwiftly.msgs.editProject(), new ClickListener() {
            public void onClick (Widget sender) {
                new ProjectEdit(_project, SwiftlyPanel.this).show();
            }
        }));
        getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);

        // Add the applet
        _applet = WidgetUtil.createApplet(
            "swiftly", "/clients/" + DeploymentConfig.version + "/swiftly-client.jar",
            "com.threerings.msoy.swiftly.client.SwiftlyApplet", "100%", "100%",
            new String[] {  "authtoken", _authtoken,
                            "projectId", String.valueOf(_project.projectId),
                            "server", _config.server,
                            "port", String.valueOf(_config.port) });
        _applet.setHeight("100%");
        setWidget(1, 0, _applet);
        getFlexCellFormatter().setColSpan(1, 0, 3);
        getFlexCellFormatter().setHeight(1, 0, "100%");

        // clear out any world client because swiftly currently kills it anyawy
        WorldClient.clearClient();
    }

    protected void updateProjectLink ()
    {
        _projectLink.setTargetHistoryToken(
            Application.createLinkToken("swiftly", String.valueOf(_project.projectId)));
        _projectLink.setText(_project.projectName);
    }

    protected SwiftlyProject _project;
    protected ConnectConfig _config;
    protected String _authtoken;
    protected Hyperlink _projectLink = new Hyperlink();
    protected Widget _applet;
}
