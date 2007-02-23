//
// $Id$

package client.swiftly;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.data.SwiftlyConfig;
import com.threerings.msoy.web.data.SwiftlyProject;

/**
 * Displays the client interface for a particular swiftly project.
 */
public class SwiftlyPanel extends VerticalPanel
{
    public SwiftlyPanel (SwiftlyConfig config, int projectId)
    {
        super();
        _projectTitle = new HorizontalPanel();
        add(_projectTitle);

        // load up the swiftly project record
        CSwiftly.swiftlysvc.loadProject(CSwiftly.creds, projectId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _project = (SwiftlyProject)result;
                _projectTitle.add(new Label(CSwiftly.msgs.swiftlyEditing()));
                _projectTitle.add(new Hyperlink(_project.projectName,
                    String.valueOf(_project.projectId)));
            }
            public void onFailure (Throwable cause) {
                CSwiftly.serverError(cause);
            }
        });

        String authtoken = (CSwiftly.creds == null) ? "" : CSwiftly.creds.token;
        setStyleName("swiftlyPanel");

        Widget display = WidgetUtil.createApplet(
            "swiftly", "/clients/swiftly-client.jar",
            "com.threerings.msoy.swiftly.client.SwiftlyApplet", "800", "600",
            new String[] {  "authtoken", authtoken,
                            "projectId", String.valueOf(projectId),
                            "server", config.server,
                            "port", String.valueOf(config.port) });
        // TODO: possible hacks to get the applet to stay the size of the browser window
        // 100% 100% should work but isn't for the createApplet width/height
        // display.setHeight("100%");
        add(display);
        // setCellHeight(display, "94%");
    }

    protected SwiftlyProject _project;
    protected HorizontalPanel _projectTitle;
}
