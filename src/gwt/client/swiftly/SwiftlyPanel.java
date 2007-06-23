//
// $Id$

package client.swiftly;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Hyperlink;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.WidgetUtil;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.data.ConnectConfig;
import com.threerings.msoy.web.data.SwiftlyProject;
import com.threerings.msoy.web.data.WebIdent;

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
        // TODO: disable the upload buttons for further testing
        // setWidget(0, 3, new SwiftlyUploader(String.valueOf(_project.projectId), CSwiftly.ident));
        // TODO: fix the alignment here. might have to put these elements in the same cell
        getFlexCellFormatter().setHorizontalAlignment(0, 2, HasAlignment.ALIGN_RIGHT);
        // getFlexCellFormatter().setHorizontalAlignment(0, 3, HasAlignment.ALIGN_RIGHT);
        
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
        getFlexCellFormatter().setColSpan(1, 0, 4);
        getFlexCellFormatter().setHeight(1, 0, "100%");

        // clear out any world client because swiftly currently kills it anyawy
        WorldClient.clearClient(true);
    }

    protected void updateProjectLink ()
    {
        _projectLink.setTargetHistoryToken(
            Application.createLinkToken("swiftly", String.valueOf(_project.projectId)));
        _projectLink.setText(_project.projectName);
    }

    // TODO: make the browse/upload buttons look like the rest of the msoy buttons
    protected static class SwiftlyUploader extends HorizontalPanel
    {
        public SwiftlyUploader (String projectId, WebIdent ident)
        {
            final FormPanel form = new FormPanel();
            HorizontalPanel panel = new HorizontalPanel();
            form.setWidget(panel);

            if (GWT.isScript()) {
                form.setAction("/swiftlyuploadsvc");
            } else {
                form.setAction("http://localhost:8080/swiftlyuploadsvc");
            }
            form.setEncoding(FormPanel.ENCODING_MULTIPART);
            form.setMethod(FormPanel.METHOD_POST);

            final FileUpload upload = new FileUpload() {
                public void onBrowserEvent (Event event) {
                    // TODO: what is this actually doing?
                    MsoyUI.info(event.toString());
                }
            };
            // stuff the web credentials and the projectId into the field name
            upload.setName(ident.token + "::" + ident.memberId + "::" + projectId);
            panel.add(upload);

            form.addFormHandler(new FormHandler() {
                public void onSubmit (FormSubmitEvent event) {
                    // don't let them submit until they plug in a file...
                    if (upload.getFilename().length() == 0) {
                        event.setCancelled(true);
                    }
                }

                public void onSubmitComplete (FormSubmitCompleteEvent event) {
                    String result = event.getResults();
                    if (result != null && result.length() > 0) {
                        // TODO: What is this reporting? Result appears to have html tags,
                        // so it looks like we'll want a spot to report this, not MsoyUI.info
                        MsoyUI.info(result);
                    }
                }
            });
            
            Button submit = new Button(CSwiftly.msgs.upload());
            submit.addClickListener(new ClickListener() {
                public void onClick (Widget widget) {
                    form.submit();
                }
            });
            panel.add(submit);
            add(form);
        }
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
    }-*/;
    
    protected SwiftlyProject _project;
    protected ConnectConfig _config;
    protected String _authtoken;
    protected Hyperlink _projectLink = new Hyperlink();
    protected Widget _applet;
}
