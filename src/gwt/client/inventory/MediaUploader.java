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
import com.google.gwt.user.client.ui.FormHandler;
import com.google.gwt.user.client.ui.FormSubmitEvent;
import com.google.gwt.user.client.ui.FormSubmitCompleteEvent;
import com.google.gwt.user.client.ui.HasAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.ui.FileUploadField;
import org.gwtwidgets.client.ui.FormPanel;

import com.threerings.gwt.ui.SubmitField;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * Helper class, used in ItemEditor.
 */
public class MediaUploader extends FlexTable
{
    /**
     * @param id the id of the uploader to create. This value is later
     * passed to the bridge to identify the hash/mimeType returned by the
     * server.
     * @param title A title to be displayed to the user.
     * @param thumbnail if true the preview will be thumbnail sized, false it will be preview
     * sized.
     * @param updater the updater that knows how to set the media hash on
     * the item.
     */
    public MediaUploader (String id, String title, boolean thumbnail,
                          ItemEditor.MediaUpdater updater)
    {
        _thumbnail = thumbnail;
        _updater = updater;

        _panel = new FormPanel(new FlowPanel());
        if (GWT.isScript()) {
            _panel.setAction("/uploadsvc");
        } else {
            _panel.setAction("http://localhost:8080/uploadsvc");
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

        _out = new Label("Browse and upload and we'll show a preview here.");
        setWidget(row, 0, _out);
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
            Widget w = ItemContainer.createContainer(desc, _thumbnail);
            int twidth = _thumbnail ? Item.THUMBNAIL_WIDTH : Item.PREVIEW_WIDTH;
            int theight = _thumbnail ? Item.THUMBNAIL_HEIGHT : Item.PREVIEW_HEIGHT;
            switch (desc.constraint) {
            case MediaDesc.HORIZONTALLY_CONSTRAINED:
                w.setWidth(twidth + "px");
                break;
            case MediaDesc.VERTICALLY_CONSTRAINED:
                w.setHeight(theight + "px");
                break;
            }
            // update our preview
            _out.setText("Preview:");
            setWidget(_previewRow, 0, w);
        }
    }

    /**
     * Set the media as uploaded by the user.
     */
    public void setUploadedMedia (MediaDesc desc)
    {
        String result = _updater.updateMedia(desc);
        if (result == null) {
            setMedia(desc);

        } else {
            _out.setText(result);
        }
    }

    protected ItemEditor.MediaUpdater _updater;

    protected FormPanel _panel;
    protected Label _out;
    protected int _previewRow;
    protected boolean _thumbnail;
}
