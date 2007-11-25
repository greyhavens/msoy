//
// $Id$

package client.swiftly;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.SwiftlyConnectConfig;
import com.threerings.msoy.web.data.SwiftlyProject;

import com.threerings.gwt.ui.WidgetUtil;

import client.shell.Application;
import client.shell.Frame;
import client.util.MsoyCallback;

/**
 * Displays the client interface for a particular Swiftly project.
 */
public class SwiftlyPanel extends FlexTable
    implements ProjectEdit.ProjectEditListener
{
    public SwiftlyPanel (SwiftlyConnectConfig config, int projectId)
    {
        super();
        setStyleName("swiftlyPanel");
        _authtoken = (CSwiftly.creds == null) ? "" : CSwiftly.creds.token;
        _config = config;
        _project = config.project;
        updateProjectLink();
        loadApplet();
    }

    // from ProjectEdit.ProjectEditListener
    public void projectSubmitted (SwiftlyProject project)
    {
        _project = project;
        _editButton.setEnabled(true);
        updateProjectLink();
    }

    /**
     * Display an error message into the status label, rather than using MsoyUI which breaks on top
     * of the Java applet
     */
    public static void displayError (String error)
    {
        _status.setText(error);
    }

    // @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        configureBridge();
    }

    protected void loadApplet ()
    {
        setWidget(0, 0, _vertPanel = new VerticalPanel());
        _vertPanel.setWidth("100%");

        // Add project information to the header
        FlexTable infoPanel = new FlexTable();
        infoPanel.setText(0, 0, CSwiftly.msgs.swiftlyEditing());
        infoPanel.setWidget(0, 1, _projectLink);

        // display the edit button only if this user is the project owner
        if (CSwiftly.getMemberId() == _project.ownerId) {
            _editButton = new Button(CSwiftly.msgs.editProject(), new ClickListener() {
                public void onClick (Widget sender) {
                    _editButton.setEnabled(false);
                    _vertPanel.insert(new ProjectEdit(_project.projectId, SwiftlyPanel.this), 1);
                }
            });
            infoPanel.setWidget(0, 2, _editButton);

        // display a link to the owner if this user is not the owner
        } else {
            HorizontalPanel cell = new HorizontalPanel();
            cell.add(new Label(CSwiftly.msgs.projectOwner()));
            cell.add(_ownerLinkPanel);
            infoPanel.setWidget(0, 2, cell);
            loadOwner();
        }
        infoPanel.getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
        infoPanel.setWidth("100%");
        _vertPanel.add(infoPanel);

        // Add a status area for error messages.
        _vertPanel.add(_status);

        // Add the applet
        String[] args = new String[] {
            "authtoken", "" + _authtoken,
            "projectId", String.valueOf(_project.projectId),
            "server", _config.server,
            "port", String.valueOf(_config.port) };
        // we have to serve swiftly-client.jar from the server to which it will connect back
        // due to security restrictions
        String swiftlyJar = "http://" + _config.server + ":" + _config.httpPort + "/clients/" +
            DeploymentConfig.version + "/swiftly-client.jar";
        _applet = WidgetUtil.createApplet(
            "swiftly", swiftlyJar, "com.threerings.msoy.swiftly.client.SwiftlyApplet",
            "100%", "100%", false, args);
        _applet.setHeight("100%");
        setWidget(1, 0, _applet);
        getFlexCellFormatter().setHeight(1, 0, "100%");

        // clear out any world client because Swiftly currently kills it anyway
        Frame.closeClient(false);
    }

    protected void updateProjectLink ()
    {
        _projectLink.setTargetHistoryToken(
            Application.createLinkToken("swiftly", String.valueOf(_project.projectId)));
        _projectLink.setText(_project.projectName);
    }

    protected void loadOwner ()
    {
        CSwiftly.swiftlysvc.getProjectOwner(CSwiftly.ident, _project.projectId, new MsoyCallback() {
            public void onSuccess (Object result) {
                MemberName owner = (MemberName)result;
                _ownerLinkPanel.clear();
                _ownerLinkPanel.add(Application.memberViewLink((MemberName)result));
            }
        });
    }

    /**
     * Display a dialog for selecting a file to be uploaded into the project.
     * If a dialog is already open, do nothing.
     */
    protected static void showUploadDialog (String projectId)
    {
        if (_uploadDialog == null) {
            _uploadDialog = new UploadDialog(projectId, CSwiftly.ident, _config,
                new UploadDialog.UploadDialogListener () {
                public void dialogClosed ()
                {
                    // clear the static reference to the upload dialog
                    _uploadDialog = null;
                }
            });
            _vertPanel.insert(_uploadDialog, 1);
        }
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display an internal error message to the user.
     */
    protected static void uploadError ()
    {
        displayUploadError(CSwiftly.msgs.errUploadError());
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display a friendly message to the user that the upload was too large.
     */
    protected static void uploadTooLarge ()
    {
        displayUploadError(CSwiftly.msgs.errUploadTooLarge());
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code received from the
     * server to display a friendly message to the user that access was denied.
     */
    protected static void accessDenied ()
    {
        displayUploadError(CSwiftly.msgs.errAccessDenied());
    }

    /**
     * Display an error message into the UploadDialog if open, otherwise use the status panel
     * to display.
     */
    protected static void displayUploadError (String message)
    {
        _uploadDialog.setError();
        SwiftlyPanel.displayError(message);
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
    protected static SwiftlyConnectConfig _config;
    protected final String _authtoken;
    protected static VerticalPanel _vertPanel;
    protected final Hyperlink _projectLink = new Hyperlink();
    protected final HorizontalPanel _ownerLinkPanel = new HorizontalPanel();
    protected Button _editButton;
    protected static final Label _status = new Label();
    protected static Widget _applet;

}
