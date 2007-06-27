//
// $Id$

package client.swiftly;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.SwiftlyProject;

import client.shell.Application;
import client.shell.WorldClient;
import client.util.MsoyUI;

/**
 * Displays the client interface for a particular Swiftly project.
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
        CSwiftly.swiftlysvc.loadProject(CSwiftly.ident, projectId, new AsyncCallback() {
            public void onSuccess (Object result) {
                _project = (SwiftlyProject)result;
                updateProjectLink();
                loadApplet();
            }
            public void onFailure (Throwable cause) {
                MsoyUI.error(CSwiftly.serverError(cause));
            }
        });
    }

    // from ProjectEdit.ProjectEditListener
    public void projectSubmitted (SwiftlyProject project)
    {
        _project = project;
        updateProjectLink();
    }
    
    // @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        configureBridge();
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
            "swiftly", "/clients/" + DeploymentConfig.version + "/swiftly-client.jar" + 
            ",/clients/" + DeploymentConfig.version + "/swiftly-client-signed.jar",
            "com.threerings.msoy.swiftly.client.SwiftlyApplet", "100%", "100%",
            new String[] {  "authtoken", _authtoken,
                            "projectId", String.valueOf(_project.projectId),
                            "server", _config.server,
                            "port", String.valueOf(_config.port) });
        _applet.setHeight("100%");
        setWidget(1, 0, _applet);
        getFlexCellFormatter().setColSpan(1, 0, 3);
        getFlexCellFormatter().setHeight(1, 0, "100%");

        // clear out any world client because Swiftly currently kills it anyway
        WorldClient.clearClient(true);
    }

    protected void updateProjectLink ()
    {
        _projectLink.setTargetHistoryToken(
            Application.createLinkToken("swiftly", String.valueOf(_project.projectId)));
        _projectLink.setText(_project.projectName);
    }

    // display a dialog for selecting a file to be uploaded into the project
    protected static void showUploadDialog (String projectId)
    {
        new UploadDialog(projectId, CSwiftly.ident).show();
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display an internal error message to the user.
     */
    protected static void uploadError ()
    {
        MsoyUI.error(CSwiftly.msgs.errUploadError());
    }
    
    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display a friendly message to the user that the upload was too large.
     */
    protected static void uploadTooLarge ()
    {
        MsoyUI.error(CSwiftly.msgs.errUploadTooLarge());
    }
    
    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display a friendly message to the user that access was denied.
     */
    protected static void accessDenied ()
    {
        MsoyUI.error(CSwiftly.msgs.errAccessDenied());
    }
    
    /**
     * This wires up upload error functions.
     */
    protected static native void configureBridge () /*-{
        $wnd.uploadError = function () {
           @client.swiftly.SwiftlyPanel::uploadError()();
        };
        $wnd.uploadTooLarge = function () {
           @client.swiftly.SwiftlyPanel::uploadTooLarge()();
        };
        $wnd.accessDenied = function () {
           @client.swiftly.SwiftlyPanel::accessDenied()();
        };
        $wnd.showUploadDialog = function (projectId) {
           @client.swiftly.SwiftlyPanel::showUploadDialog(Ljava/lang/String;)(projectId);
        };
    }-*/;
    
    protected SwiftlyProject _project;
    protected ConnectConfig _config;
    protected String _authtoken;
    protected Hyperlink _projectLink = new Hyperlink();
    protected Widget _applet;
}
