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
        setStyleName("swiftlyPanel");
        _authtoken = (CSwiftly.creds == null) ? "" : CSwiftly.creds.token;
        _config = config;

        _errorContainer = new HorizontalPanel();
        add(_errorContainer);

        // load up the swiftly project record
        CSwiftly.swiftlysvc.loadProject(CSwiftly.creds, projectId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _project = (SwiftlyProject)result;
                updateProjectLink();
                loadApplet();
            }
            public void onFailure (Throwable cause) {
                 _errorContainer.add(new Label(CSwiftly.serverError(cause)));
            }
        });

        _header = new HorizontalPanel();
        add(_header);
    }

    public void projectSubmitted (SwiftlyProject project)
    {
        _project = project;
        updateProjectLink();
        _applet.setVisible(true);
    }

    protected void loadApplet ()
    {
        // Add a header for the applet
        HorizontalPanel projectTitle = new HorizontalPanel();
        projectTitle.add(new Label(CSwiftly.msgs.swiftlyEditing()));
        projectTitle.add(_projectLink);
        _header.add(projectTitle);
        _header.add(new Button(CSwiftly.msgs.editProject(), new ClickListener() {
            public void onClick (Widget sender) {
                // TODO: unhack
                _applet.setVisible(false);
                new ProjectEdit(_project, SwiftlyPanel.this).show();
            }
        }));

        // Add the applet
        _applet = WidgetUtil.createApplet(
            "swiftly", "/clients/swiftly-client.jar",
            "com.threerings.msoy.swiftly.client.SwiftlyApplet", "100%", "600",
            new String[] {  "authtoken", _authtoken,
                            "projectId", String.valueOf(_project.projectId),
                            "server", _config.server,
                            "port", String.valueOf(_config.port) });
        // TODO: possible hacks to get the applet to stay the size of the browser window
        // 100% 100% should work but isn't for the createApplet width/height
        // setCellHeight(_applet, "94%");
        // _applet.setWidth("100%");
        add(_applet);
    }

    protected void updateProjectLink ()
    {
        _projectLink.setTargetHistoryToken(String.valueOf(_project.projectId));
        _projectLink.setText(_project.projectName);
    }

    protected SwiftlyProject _project;
    protected SwiftlyConfig _config;
    protected String _authtoken;
    protected HorizontalPanel _errorContainer;
    protected HorizontalPanel _header;
    protected Hyperlink _projectLink = new Hyperlink();
    protected Widget _applet;
}
