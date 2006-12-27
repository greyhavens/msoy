//
// $Id$

package client.inventory;

import com.google.gwt.core.client.GWT;

import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

import org.gwtwidgets.client.ui.FileUploadField;
import org.gwtwidgets.client.ui.FormPanel;

import com.threerings.gwt.ui.SubmitField;

import com.threerings.msoy.item.web.MediaDesc;

import client.util.MediaUtil;
import client.util.MsoyUI;

/**
 * Helper class, used in ItemEditor.
 */
public class MediaUploader extends VerticalPanel
{
    /**
     * @param id the id of the uploader to create. This value is later passed to the bridge to
     * identify the hash/mimeType returned by the server.
     * @param title A title to be displayed to the user.
     * @param thumbnail if true the preview will be thumbnail sized, false it will be preview
     * sized.
     * @param updater the updater that knows how to set the media hash on the item.
     */
    public MediaUploader (InventoryContext ctx, String id, String title, boolean thumbnail,
                          ItemEditor.MediaUpdater updater)
    {
        setStyleName("mediaUploader");

        _thumbnail = thumbnail;
        _updater = updater;
        _title = title;

        add(_status = MsoyUI.createLabel(_title, "Status"));

        add(_target = new HorizontalPanel());
        _target.setHorizontalAlignment(HorizontalPanel.ALIGN_CENTER);
        _target.setVerticalAlignment(HorizontalPanel.ALIGN_MIDDLE);
        _target.setStyleName(_thumbnail ? "Thumbnail" : "Preview");

        _panel = new FormPanel(new HorizontalPanel());
        _panel.setStyleName("Controls");
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
            _panel.addField(new SubmitField("submit", ctx.imsgs.upload()), "submit");
        } else {
            Button submit = new Button(ctx.imsgs.upload());
            submit.addClickListener(new ClickListener() {
                public void onClick (Widget widget) {
                    _panel.submit();
                }
            });
            _panel.add(submit);
        }
        add(_panel);
    }

    /**
     * Set the media to be shown in this uploader.
     */
    public void setMedia (MediaDesc desc)
    {
        _target.clear();
        if (desc != null) {
            int size = _thumbnail ? MediaDesc.THUMBNAIL_SIZE : MediaDesc.PREVIEW_SIZE;
            _target.add(MediaUtil.createMediaView(desc, size));
            _status.setText(_title);
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
            _status.setText(result);
        }
    }

    protected ItemEditor.MediaUpdater _updater;

    protected String _title;
    protected Label _status;
    protected HorizontalPanel _target;
    protected FormPanel _panel;

    protected boolean _thumbnail;
}
