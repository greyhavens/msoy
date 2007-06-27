/**
 * 
 */
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
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.threerings.gwt.ui.InlineLabel;
import com.threerings.msoy.web.data.WebIdent;

public class UploadDialog extends BorderedDialog
{
    public UploadDialog (String projectId, WebIdent ident)
    {
        // no autohiding, have a close button, disable dragging
        super(false, false, false);
        
        _header.add(new InlineLabel(CSwiftly.msgs.uploadTitle()));
        _status = new InlineLabel();
        
        FlexTable contents = (FlexTable)_contents;
        contents.setStyleName("swiftlyUploader");

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
                    _status.setText(CSwiftly.msgs.selectFile());
                } else {
                    _status.setText(CSwiftly.msgs.uploadStarted());
                }
            }

            public void onSubmitComplete (FormSubmitCompleteEvent event) {
                // hide the dialog only after the form has been submitted
                hide();
                // if something went horribly wrong, give the user some kind of error
                String result = event.getResults();
                if (result != null && result.length() > 0) {
                    _status.setText(result);
                }
            }
        });

        contents.setText(0, 0, CSwiftly.msgs.selectFile());
        contents.setWidget(1, 0, form);

        HorizontalPanel buttons = new HorizontalPanel();
        // Upload button
        buttons.add(new Button(CSwiftly.msgs.upload(), new ClickListener() {
            public void onClick (Widget sender) {
                form.submit();
            }
        }));
        
        // Cancel button
        buttons.add(new Button(CSwiftly.msgs.cancel(), new ClickListener() {
            public void onClick (Widget sender) {
                hide();
            }
        }));
        contents.setWidget(2, 0, buttons);
        contents.getFlexCellFormatter().setHorizontalAlignment(2, 0, HasAlignment.ALIGN_RIGHT);
        contents.setWidget(3, 0, _status);
    }
    
    // from BorderedDialog.  This is called in the super constructor, so no UI components that
    // depend on members that are set in this object's constructor can be used here.
    public Widget createContents ()
    {
        return new FlexTable();
    }
    
    protected InlineLabel _status;
}