//
// $Id$

package client.swiftly;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.SwiftlyConfig;
import com.threerings.msoy.web.data.SwiftlyProject;

import client.swiftly.ProjectEdit.ProjectEditListener;

/**
 * Displays the client interface for a particular swiftly project.
 */
public class SwiftlyPanel extends VerticalPanel
    implements ProjectEditListener
{
    public SwiftlyPanel (SwiftlyConfig config, int projectId)
    {
        super();

        HorizontalPanel projectTitle = new HorizontalPanel();
        projectTitle.add(new Label(CSwiftly.msgs.swiftlyEditing()));
        projectTitle.add(_projectLink);

        // load up the swiftly project record
        CSwiftly.swiftlysvc.loadProject(CSwiftly.creds, projectId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _project = (SwiftlyProject)result;
                updateProjectLink();
            }
            public void onFailure (Throwable cause) {
                CSwiftly.serverError(cause);
            }
        });

        HorizontalPanel header = new HorizontalPanel();
        header.add(projectTitle);
        header.add(new Button(CSwiftly.msgs.editProject(), new ClickListener() {
            public void onClick (Widget sender) {
                // TODO: unhack
                _applet.setVisible(false);
                new ProjectEdit(_project, SwiftlyPanel.this).show();
            }
        }));
        add(header);

        String authtoken = (CSwiftly.creds == null) ? "" : CSwiftly.creds.token;
        setStyleName("swiftlyPanel");

        _applet = WidgetUtil.createApplet(
            "swiftly", "/clients/swiftly-client.jar",
            "com.threerings.msoy.swiftly.client.SwiftlyApplet", "100%", "600",
            new String[] {  "authtoken", authtoken,
                            "projectId", String.valueOf(projectId),
                            "server", config.server,
                            "port", String.valueOf(config.port) });
        // TODO: possible hacks to get the applet to stay the size of the browser window
        // 100% 100% should work but isn't for the createApplet width/height
        // setCellHeight(_applet, "94%");
        // _applet.setWidth("100%");
        add(_applet);
    }

    public void projectSubmitted (SwiftlyProject project)
    {
        _project = project;
        updateProjectLink();
        _applet.setVisible(true);
    }

    protected void updateProjectLink ()
    {
        _projectLink.setTargetHistoryToken(String.valueOf(_project.projectId));
        _projectLink.setText(_project.projectName);
    }

    protected SwiftlyProject _project;
    protected Hyperlink _projectLink = new Hyperlink();
    protected Widget _applet;
}
