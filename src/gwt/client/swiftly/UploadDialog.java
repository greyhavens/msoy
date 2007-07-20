//
// $Id$

package client.swiftly;

import client.util.BorderedDialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FileUpload;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormPanel;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.gwt.ui.InlineLabel;

import com.threerings.msoy.web.data.WebIdent;

public class UploadDialog extends BorderedDialog
{
    /**
     * A callback interface for classes that want to know when this dialog was closed.
     */
    public static interface UploadDialogListener
    {
        public void dialogClosed ();
    }

    public UploadDialog (String projectId, WebIdent ident, UploadDialogListener listener)
    {
        // no auto hiding, have a close button, disable dragging
        super(false, false, false);
        FlexTable contents = (FlexTable)_contents;
        contents.setStyleName("swiftlyUploader");
        
        _header.add(new InlineLabel(CSwiftly.msgs.uploadTitle()));
        _status = new Label(CSwiftly.msgs.selectFile());
        _listener = listener;

        final FormPanel form = new FormPanel();

        if (GWT.isScript()) {
            form.setAction("/swiftlyuploadsvc");
        } else {
            form.setAction("http://localhost:8080/swiftlyuploadsvc");
        }
        form.setEncoding(FormPanel.ENCODING_MULTIPART);
        form.setMethod(FormPanel.METHOD_POST);

        final FileUpload upload = new FileUpload() {
            public void onBrowserEvent (Event event) {
                // something went horribly wrong, so let the user know something
                _status.setText(event.toString());
            }
        };
        // stuff the web credentials and the projectId into the field name
        upload.setName(ident.token + "::" + ident.memberId + "::" + projectId);
        form.setWidget(upload);

        form.addFormHandler(new FormHandler() {
            public void onSubmit (FormSubmitEvent event) {
                // don't let them submit until they plug in a file...
                if (upload.getFilename().length() == 0) {
                    event.setCancelled(true);
                    _status.setText(CSwiftly.msgs.mustSelectFile());
                } else {
                    _wasError = false;
                    _status.setText(CSwiftly.msgs.uploadStarted());
                }
            }

            public void onSubmitComplete (FormSubmitCompleteEvent event) {                
                String result = event.getResults();
                
                // hide the dialog after the form has been submitted only if no error was set
                if (!_wasError) {
                    closeDialog();

                // if something went horribly wrong, give the user some kind of error
                } else if (result != null && result.trim().length() > 0) {
                    _status.setText(result);
                }
            }
        });

        contents.setWidget(0, 0, _status);
        contents.setWidget(1, 0, form);

        // Upload button
        _footer.add(new Button(CSwiftly.msgs.upload(), new ClickListener() {
            public void onClick (Widget sender) {
                form.submit();
            }
        }));
        
        // Cancel button
        _footer.add(new Button(CSwiftly.msgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                closeDialog();
            }
        }));

    }

    // from BorderedDialog.  This is called in the super constructor, so no UI components that
    // depend on members that are set in this object's constructor can be used here.
    public Widget createContents ()
    {
        return new FlexTable();
    }

    /**
     * Display an error message in the UploadDialog's status panel.
     */
    public void setErrorMessage (String message)
    {
        _status.setText(message);
        updateFrame();
        _wasError = true;
    }

    /**
     * Handle closing the dialog and informing the listener.
     */
    protected void closeDialog ()
    {
        hide();
        _listener.dialogClosed();
    }

    protected Label _status;
    protected UploadDialogListener _listener;
    protected boolean _wasError;
}
