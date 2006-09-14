//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

import com.threerings.msoy.item.web.MediaItem;

import org.gwtwidgets.client.ui.FileUploadField;
import org.gwtwidgets.client.ui.FormPanel;

/**
 * Extends the standard {@link ItemEditor} with some magic to allow uploading
 * of the media during the item creation process.
 */
public abstract class MediaItemEditor extends ItemEditor
{
    public MediaItemEditor ()
    {
    }

    protected void createEditorInterface ()
    {
        _panel = new FormPanel(new FlowPanel());
        if (GWT.isScript()) {
            _panel.setAction("/upload");
        } else {
            _panel.setAction("http://localhost:8080/upload");
        }
        _panel.setTarget("upload");
        _panel.setMethodAsPost();
        _panel.setMultipartEncoding();

// TODO: make this work, handle errors, check the status
//        _panel.addFormHandler(new FormHandler() {
//            public void onSubmit (FormSubmitEvent event) {
//                // nada for now
//            }
//
//            public void onSubmitComplete (FormSubmitCompleteEvent event) {
//                // TODO: what is the format of the results?
//                _out.setText(event.getResults());
//            }
//        });

        _panel.addField(new FileUploadField("media"), "media");

        if (GWT.isScript()) {
            _panel.addField(new SubmitField("submit", "Upload"), "submit");
        } else {
            Button submit = new Button("Upload");
            submit.addClickListener(new ClickListener() {
                public void onClick (Widget widget) {
                    _panel.submit();
                }
            });
            _panel.add(submit);
        }

        int row = getRowCount();
        setText(row, 0, "Upload");
        setWidget(row, 1, _panel);

        String msg = "First upload the file from your computer. " +
            "Then create the item below.";
        setWidget(row+1, 0, _out = new Label(msg));
        getFlexCellFormatter().setColSpan(row+1, 0, 2);

        // reserve area for the preview
        reservePreviewSpace();

        // we have to do this wacky singleton crap because GWT and/or
        // JavaScript doesn't seem to cope with our trying to create an
        // anonymous function that calls an instance method on a JavaScript
        // object
        _singleton = this;
    }

    // @Override // from Widget
    protected void onLoad ()
    {
        super.onLoad();
        configureBridge();
    }

    /**
     * Configures this item editor with the hash value for media that it is
     * about to upload.
     */
    protected void setHash (String mediaHash, int mimeType)
    {
        if (_item != null) {
            ((MediaItem)_item).setHash(mediaHash, (byte)mimeType);
            updatePreview();
        }
        _out.setText("File uploaded.");
        updateSubmittable();
    }

    /**
     * This is called from our magical JavaScript method by JavaScript code
     * received from the server as a response to our file upload POST request.
     */
    protected static void callBridge (String mediaHash, int mimeType)
    {
        _singleton.setHash(mediaHash, mimeType);
    }

    /**
     * This wires up a sensibly named function that our POST response
     * JavaScript code can call.
     */
    protected static native void configureBridge () /*-{
        $wnd.setHash = function (hash, type) {
           @client.inventory.MediaItemEditor::callBridge(Ljava/lang/String;I)(hash, type);
        };
    }-*/; 

    /** Create a forum submit button. */
    protected class SubmitField extends Widget
    {
        public SubmitField (String name, String value) {
            setElement(DOM.createElement("input"));
            DOM.setAttribute(getElement(), "type", "submit");
            DOM.setAttribute(getElement(), "value", value);
            DOM.setAttribute(getElement(), "name", name);
        }
    }

    protected FormPanel _panel;
    protected Label _out;

    protected static MediaItemEditor _singleton;
}
