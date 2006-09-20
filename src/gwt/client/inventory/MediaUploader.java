//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.DeferredCommand;

import com.google.gwt.user.client.rpc.AsyncCallback;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.ui.FileUploadField;
import org.gwtwidgets.client.ui.FormPanel;

import com.threerings.msoy.item.web.MediaDesc;
import com.threerings.msoy.item.web.StaticMediaDesc;

import client.util.SubmitField;

/**
 * Helper class, used in ItemEditor.
 */
public class MediaUploader extends FlexTable
{
    public MediaUploader (
        String id, String title, ItemEditor.MediaUpdater updater)
    {
        _updater = updater;

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

        _panel.addField(new FileUploadField(id), id);

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
        FlexCellFormatter cellFormatter = getFlexCellFormatter();

        setText(row, 0, title);
        setWidget(row, 1, _panel);
        row++;

        String msg = "Upload Message TODO";
        setWidget(row, 0, _out = new Label(msg));
        cellFormatter.setColSpan(row, 0, 2);
        row++;

        setText(row, 0, "Preview");
        cellFormatter.setColSpan(row, 0, 2);
        row++;

        _previewRow = row;
        prepareCell(_previewRow, 0);
        cellFormatter.setColSpan(_previewRow, 0, 2);
    }

    /**
     * Set the media to be shown in this uploader.
     */
    public void setMedia (MediaDesc desc)
    {
        if (desc != null) {
            // update our preview
            setWidget(_previewRow, 0,
                ItemContainer.createContainer(desc.getMediaPath()));
        }
    }

    /**
     * Set the media as uploaded by the user.
     */
    public void setUploadedMedia (MediaDesc desc)
    {
        setMedia(desc);

        // set it in the item, if possible
        if (_updater != null) {
            _updater.updateMedia(desc.hash, desc.mimeType);
        }
        _out.setText("File uploaded.");
    }

    protected ItemEditor.MediaUpdater _updater;

    protected FormPanel _panel;
    protected Label _out;
    protected int _previewRow;
}
