//
// $Id$

package client.swiftly;

import client.shell.Application;
import client.shell.WorldClient;
import client.util.MsoyUI;

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
        String[] args = new String[] {
            "authtoken", "" + _authtoken,
            "projectId", String.valueOf(_project.projectId),
            "server", _config.server,
            "port", String.valueOf(_config.port) };
        // we have to serve swiftly-client.jar from the server to which it will connect back
        // due to security restrictions and proxy the game jar through there as well
        String swiftlyJar = "http://" + _config.server + ":" + _config.httpPort + "/clients/" +
            DeploymentConfig.version + "/swiftly-client.jar";
        _applet = WidgetUtil.createApplet(
            "swiftly", swiftlyJar, "com.threerings.msoy.swiftly.client.SwiftlyApplet",
            "100%", "100%", args);
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

    /**
     * Display a dialog for selecting a file to be uploaded into the project.
     */
    protected static void showUploadDialog (String projectId)
    {
        if (_uploadDialog == null) {
            _uploadDialog = new UploadDialog(projectId, CSwiftly.ident,
                new UploadDialog.UploadDialogListener () {
                public void dialogClosed ()
                {
                    // clear the static reference to the upload dialog
                    _uploadDialog = null;
                }
            });
        }
        _uploadDialog.show();
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display an internal error message to the user.
     */
    protected static void uploadError ()
    {
        displayError(CSwiftly.msgs.errUploadError());
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display a friendly message to the user that the upload was too large.
     */
    protected static void uploadTooLarge ()
    {
        displayError(CSwiftly.msgs.errUploadTooLarge());
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display a friendly message to the user that access was denied.
     */
    protected static void accessDenied ()
    {
        displayError(CSwiftly.msgs.errAccessDenied());
    }

    /**
     * Display an error message into the UploadDialog if open, otherwise use MsoyUI to display.
     */
    protected static void displayError (String message)
    {
        if (_uploadDialog != null) {
            _uploadDialog.setErrorMessage(message);
        } else {
            MsoyUI.error(message);
        }
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

    /** Reference to an open upload dialog window, if any is open */
    protected static UploadDialog _uploadDialog;

    protected SwiftlyProject _project;
    protected ConnectConfig _config;
    protected String _authtoken;
    protected Hyperlink _projectLink = new Hyperlink();
    protected Widget _applet;

}
